package org.pygisserver.web.wmtsprotocol.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.web.entity.ServerRequestInfo;
import org.pygisserver.web.entity.GuassCoordinates;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.utils.UserUtil;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.List;

/**
 * @Description: wmts的拦截请求，主要处理wmts如果行列号不正确，超过最大请求，服务暂停等判断
 * @author: 刘洋
 * @date: 2020-12-02
 */
@Slf4j
@Component
public class WmtsServiceInterceptor implements HandlerInterceptor {

	@Autowired
	private MapServerService mapServerService;

	@Autowired
	private RedisUtils redisUtils;

	@Autowired
	private GuassCoordinates guassCoordinates;

	public static final ThreadLocal<TileRequestInfo> TILEREQUEST = new ThreadLocal<>();

	public static final ThreadLocal<LayerBaseInfo> LAYERBASE = new ThreadLocal<>();

	public static final ThreadLocal<ServerRequestInfo> SERVERREQUESTINFO = new ThreadLocal<>();

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		TileRequestInfo requestInfo = getRequestParam(request);	
		if ("getcapabilities".equalsIgnoreCase(requestInfo.getRequest())) {
			TILEREQUEST.set(requestInfo);
			return true;
		}
		final LayerBaseInfo baseInfo = mapServerService.getLayerBaseInfo(requestInfo.getLayerName(),
				UserUtil.getRequestIp(request));
        return checkUnValidReq(baseInfo, requestInfo);
	}

	public boolean checkUnValidReq(LayerBaseInfo baseInfo, TileRequestInfo requestInfo) {
		boolean flag = beyondMaxRowOrCol(requestInfo, baseInfo);
		if (!flag) {
			String msg = "行列号请求不正确，请检查";
			log.error(msg);
			throw new BaseException(ErrorCode.NOT_FOUND.getErrorCode(), msg);
		}
		String format = requestInfo.getMineType();
		int maxZoomLevel = baseInfo.getSupportMaxLevel();
		//如果是terrain 由于瓦片大小是65*65
		if (format.endsWith("terrain")) {
			maxZoomLevel += 2;
		}
		//如果是墨卡托，则最大级别增加1
		if (OgcEpsg.getOgcEpsgCodeType(requestInfo.getGridSet()) == OgcEpsg.MERCATOR_CODE) maxZoomLevel += 1;
		if (maxZoomLevel < requestInfo.getZoomLevel()) {
			String msg = String.format("TileServiceController getWmtsTileInfo has error:当前请求的级别%S大于最大级别%s",
					requestInfo.getZoomLevel(), baseInfo.getSupportMaxLevel());
			log.warn(msg);
			throw new BaseException(ErrorCode.NOT_FOUND.getErrorCode(), msg);
		}
		TILEREQUEST.set(requestInfo);
		LAYERBASE.set(baseInfo);    
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		TILEREQUEST.remove();
	}

	private boolean beyondMaxRowOrCol(TileRequestInfo requestInfo, LayerBaseInfo layerBaseInfo) {
		int row = requestInfo.getTileRow();
		int col = requestInfo.getTileCol();
		String srs = requestInfo.getGridSet();
		int level = requestInfo.getZoomLevel();
		int[] tileRange = null;
		int guassMaxCol = -1;
		if (guassCoordinates.getGuassCoordinates().contains(srs)){
			List<String> tilecount = guassCoordinates.getTileParam().get(srs.replace(":", "")).get("tilecount");
			List<String> guassCoordinateBbox = guassCoordinates.getTileParam().get(srs.replace(":", "")).get("bbox");
			BoundingBox resourceBoundingBox = convertBoundingBox(layerBaseInfo.getResourceBbox(),srs);
			String[] split = tilecount.get(level).split("\\*");
			guassMaxCol = Integer.parseInt(split[0]);
			tileRange = GeometryUtil.convertGuassToTiledRange(tilecount.get(level), guassCoordinateBbox, resourceBoundingBox, level);
			if (row < 0 || row > Integer.parseInt(split[1]) - 1) {
				return false;
			}
		}else if (row < 0 || row > Math.pow(2, level) - 1) {
			return false;
		}
		// 判断当前的瓦片在该级别下的边界行列号，如果超过，直接返回
		String nativeBbox = layerBaseInfo.getNativaBbox();
		String[] nativeBboxArray = nativeBbox.split(";");
		double minX = Double.parseDouble(nativeBboxArray[0]);
		double maxX = Double.parseDouble(nativeBboxArray[2]);
		double minY = Double.parseDouble(nativeBboxArray[1]);
		double maxY = Double.parseDouble(nativeBboxArray[3]);
		ReferencedEnvelope envelope = null;
		try {
			envelope = new ReferencedEnvelope(minX, maxX, minY, maxY, CRS.decode("EPSG:4326", true));
		} catch (MismatchedDimensionException | FactoryException e) {
			e.printStackTrace();
		}
		if (OgcEpsg.getOgcEpsgCodeType(srs) == OgcEpsg.MERCATOR_CODE) {
			if (col < 0) {
				if (Math.abs(col) % Math.pow(2, level) == 0) {
					col = 0;
				} else {
					col = (int) (Math.pow(2, level) - Math.abs(col) % Math.pow(2, level));
				}
			}
			if (col > Math.pow(2, level) - 1) {
				col = (int) (col % (Math.pow(2, level)));
			}
			envelope = GeometryUtil.transformCoordinateSystem(envelope, "EPSG:3857");
			tileRange = GeometryUtil.convert3857ToTiledRange(level, envelope);
		} else if (guassCoordinates.getGuassCoordinates().contains(srs)){
			if (col < 0) {
				if (Math.abs(col) % guassMaxCol == 0) {
					col = 0;
				} else {
					col = (int) (guassMaxCol - Math.abs(col) % guassMaxCol);
				}
			}
			if (col > guassMaxCol - 1) {
				col = (int) (col % (guassMaxCol));
			}
			
		}else {
			if (col < 0) {
				if (Math.abs(col) % Math.pow(2, level + 1) == 0) {
					col = 0;
				} else {
					col = (int) (Math.pow(2, level + 1) - Math.abs(col) % Math.pow(2, level + 1));
				}
			}
			if (col > Math.pow(2, level + 1) - 1) {
				col = (int) (col % (Math.pow(2, level + 1)));
			}
			tileRange = GeometryUtil.convert4326ToTiledRange(level, envelope);
		}
		if (col > tileRange[2] || col < tileRange[0] || row > tileRange[3] || row < tileRange[1]) {
			return false;
		}
		requestInfo.setTileCol(col);
		return true;
	}
	
	private BoundingBox convertBoundingBox(String resourcebbox,String srs) {
		try {
			String[] nativeBboxArray = resourcebbox.split(";");
			return new ReferencedEnvelope(Double.parseDouble(nativeBboxArray[0]), Double.parseDouble(nativeBboxArray[2]), Double.parseDouble(nativeBboxArray[1]), Double.parseDouble(nativeBboxArray[3]), CRS.decode(srs,true));
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("MapServerServiceImpl convertBoundingBox has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "坐标系Bbox信息"));
		}
	}

	/**
	 * 处理wmts的请求参数
	 * 
	 * @param req
	 * @return
	 */
	private TileRequestInfo getRequestParam(HttpServletRequest req) {
		TileRequestInfo tileInfo = new TileRequestInfo();
		String tileMatrix = "";
		for (Enumeration<String> paraNames = req.getParameterNames(); paraNames.hasMoreElements();) {
			String paramName = paraNames.nextElement();
			if ("request".equalsIgnoreCase(paramName)) {
				tileInfo.setRequest(req.getParameter(paramName));
			}
			if ("layer".equalsIgnoreCase(paramName)) {
				tileInfo.setLayerName(req.getParameter(paramName));
			}
			if ("format".equalsIgnoreCase(paramName)) {
				tileInfo.setMineType(req.getParameter(paramName));
			}
			if ("TileCol".equalsIgnoreCase(paramName)) {
				tileInfo.setTileCol(Integer.parseInt(req.getParameter(paramName)));
			}
			if ("TileRow".equalsIgnoreCase(paramName)) {
				tileInfo.setTileRow(Integer.parseInt(req.getParameter(paramName)));
			}
			if ("tilematrixset".equalsIgnoreCase(paramName)) {
				tileInfo.setGridSet(req.getParameter(paramName));
			}
			if ("TileMatrix".equalsIgnoreCase(paramName)) {
				tileMatrix = req.getParameter(paramName);
			}
			if ("style".equalsIgnoreCase(paramName)) {
				String styleName = StringUtils.isBlank(req.getParameter(paramName)) ? "" : req.getParameter(paramName);
				tileInfo.setStyle("default".equalsIgnoreCase(styleName) ? "" : styleName);
			}
		}
		int level = -1;
		if (StringUtils.isNotBlank(tileMatrix)) {
			if (tileMatrix.contains(tileInfo.getGridSet())) {
				level = Integer.parseInt(tileMatrix.split(tileInfo.getGridSet() + ":")[1]);
			} else {
				level = Integer.parseInt(tileMatrix);
			}
		}
		tileInfo.setZoomLevel(level);
		return tileInfo;
	}
}
