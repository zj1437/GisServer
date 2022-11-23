package org.pygisserver.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.util.ExcelUtil;
import org.pygisserver.common.util.ExportUtils;
import org.pygisserver.netanalysis.service.NetworkMapLayerAnalysis;
import org.pygisserver.web.entity.PoiQueryInfo;
import org.pygisserver.web.entity.PoiQueryResult;
import org.pygisserver.web.service.BaseCalculateService;
import org.pygisserver.web.service.PoiQueryService;
import org.pygisserver.web.utils.MapServerUtils;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @Description: 基础量算类
 * @author:  
 * @date: 2022-11-17
 */
@Api(tags="基础量算查询接口")
@RestController
@RequestMapping("/basecalculate/")
@Slf4j
public class BaseCalculateController {


	@Autowired(required = false)
	private NetworkMapLayerAnalysis networkMapLayerAnalysis;

	@Autowired
	private BaseCalculateService baseCalculateService;

	@ApiOperation(value="测地距离",notes="测地距离")
	@ApiImplicitParams({
			@ApiImplicitParam(name="startPoint",value="起点经纬度",required=true,paramType="query"),
			@ApiImplicitParam(name="endPoint",value="终点经纬度",required=true,paramType="query")
	})
	@RequestMapping(value="/sphere",method=RequestMethod.GET)
	public ReponseEntity<Double> projection(@RequestParam(value ="startPoint", required=true) String startPoint, @RequestParam(value ="endPoint", required=true) String endPoint) {

		String [] startPoint2=startPoint.split(",");

		String [] endPoint2=endPoint.split(",");

		double startLon = Double.parseDouble(startPoint2[0]);
		double startLat = Double.parseDouble(startPoint2[1]);

		double endLon = Double.parseDouble(endPoint2[0]);
		double endLat = Double.parseDouble(endPoint2[1]);

		if(startLon < -180 || startLon > 180 || startLat < -90 || startLat > 90 || endLon < -180 || endLon > 180 || endLat < -90 || endLat > 90) {
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, "数据"));
		}

		// 84坐标系构造GeodeticCalculator
		GeodeticCalculator geodeticCalculator = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
		// 起点经纬度
		geodeticCalculator.setStartingGeographicPoint(startLon,startLat);
		// 末点经纬度
		geodeticCalculator.setDestinationGeographicPoint(endLon,endLat);
		// 计算距离，单位：米
		double orthodromicDistance = geodeticCalculator.getOrthodromicDistance();
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),
				orthodromicDistance/1000);
	}


	@ApiOperation(value="平面距离",notes="平面距离")
	@ApiImplicitParams({
			@ApiImplicitParam(name="startPoint",value="起点经纬度",required=true,paramType="query"),
			@ApiImplicitParam(name="endPoint",value="终点经纬度",required=true,paramType="query"),
			@ApiImplicitParam(name="srid",value="空间标识符-参考投影",required=true,paramType="query")

	})
	@RequestMapping(value="/projection",method=RequestMethod.GET)
	public ReponseEntity<String> projection(@RequestParam(value ="startPoint",required=true) String startPoint,
											@RequestParam(value ="endPoint",required=true) String endPoint,
											@RequestParam(value ="srid",required=true) String srid
	) {

		srid=srid.split(":")[1];

		String [] startPoint2=startPoint.split(",");

		String [] endPoint2=endPoint.split(",");

		double startLon = Double.parseDouble(startPoint2[0]);
		double startLat = Double.parseDouble(startPoint2[1]);

		double endLon = Double.parseDouble(endPoint2[0]);
		double endLat = Double.parseDouble(endPoint2[1]);

		if(startLon < -180 || startLon > 180 || startLat < -90 || startLat > 90 || endLon < -180 || endLon > 180 || endLat < -90 || endLat > 90 || StringUtils.isBlank(srid)) {
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, "数据"));
		}

		String resource = "" ;

		StringBuilder sBuilder = new StringBuilder("POINT(");
		sBuilder.append(startLon).append(" ").append(startLat);
		sBuilder.append(")");

		StringBuilder eBuilder = new StringBuilder("POINT(");
		eBuilder.append(endLon).append(" ").append(endLat);
		eBuilder.append(")");

		String resour=baseCalculateService.projection(sBuilder.toString(),eBuilder.toString(), "4326",srid);
		DecimalFormat df=null;
		if ("4326".equals(srid)){
			df=new DecimalFormat("#.0000");
			df.format(new BigDecimal(resour));
			resource= resour+" deg";
		}else{
			df=new DecimalFormat("#.000");
			resource=df.format(new BigDecimal(resour));
		}

		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),
				resource);

	}
}
