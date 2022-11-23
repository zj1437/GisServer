package org.pygisserver.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.entity.RoadPlanEntity;
import org.pygisserver.common.entity.RoadPlanParameter;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.web.service.NetworkAnalysisService;
import org.pygisserver.netanalysis.service.NetworkMapLayerAnalysis;
import org.pygisserver.web.factory.PySpiServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @Description: 网络分析 控制类 
 * @author:  
 * @date: 2021-07-06
 */
@Api(tags = "路径规划接口")
@RestController
@RequestMapping("/netanalysis")
public class NetWorkAnalysisController {

	@Resource
	private PySpiServiceFactory spserviceFactory;

	@Autowired(required = false)
	private NetworkMapLayerAnalysis networkMapLayerAnalysis;

	@Autowired
	private NetworkAnalysisService netWorkAnalysisService;

	/**
	 * 根据起点和终点计算最短路径
	 * 
	 * @param startPoint
	 *            起点坐标
	 * @param endPoint
	 *            终点坐标
	 * @param planType
	 *            规划方式（0 最短 1 最优）
	 * @param resultType
	 *            结果类型（是否将返回结果合并，默认合并）
	 * @param rationValue
	 *            道路限宽(-1表示不限制路宽)
	 * @param limitValue
	 *            道路限高(-1表示不限制高度)
	 * @return
	 */
	@ApiOperation(value = "路径规划", notes = "获取poi的信息点")
	@ApiImplicitParams({ @ApiImplicitParam(name = "startPoint", value = "起点坐标", required = true, paramType = "query"),
			@ApiImplicitParam(name = "endPoint", value = "终点坐标", required = true, paramType = "query"),
			@ApiImplicitParam(name = "planType", value = "规划方式（0 最短  1 最优）", required = true, paramType = "query"),
			@ApiImplicitParam(name = "resultType", value = "结果类型（是否将返回结果合并，默认合并）", required = false, paramType = "query"),
			@ApiImplicitParam(name = "rationValue", value = "道路限宽(-1表示不限制路宽)", required = false, paramType = "query"),
			@ApiImplicitParam(name = "limitValue", value = "道路限高(-1表示不限制高度)", required = false, paramType = "query"),
			@ApiImplicitParam(name = "avoidpoints", value = "规避点(点与点之间以逗号隔开)", required = false, paramType = "query"), })
	@GetMapping("/routing/plan")
	public ReponseEntity<?> getShortestRouting(@RequestParam("startPoint") String startPoint,
			@RequestParam("endPoint") String endPoint, @RequestParam("planType") int planType,
			@RequestParam(value = "resultType", required = false, defaultValue = "1") int resultType,
			@RequestParam(value = "ration", required = false, defaultValue = "-1") double rationValue,
			@RequestParam(value = "limit", required = false, defaultValue = "-1") double limitValue,
			@RequestParam(value = "avoidpoints", required = false) String points, HttpServletRequest req) {
		if (networkMapLayerAnalysis == null) {
			throw new BaseException(ExceptionsUtil.ERROR_500,
					ExceptionsUtil.formatInfo(ExceptionsUtil.TERRAIN_ANALYSIS_PLUG, "网络分析"));
		}

		/**
		 * 如果规避点不为空，则线处理规避点，如果规避点由于客户没有标记在某条线上 则按照该点距离5KM范围内最近的线进行处理，如果找不到则剔除该点
		 */
		List<String> pointQuery = null;
		if (StringUtils.isNotBlank(points)) {
			pointQuery = createListPoints(points);
			netWorkAnalysisService.updateRoadCost(pointQuery);
		}

		String[] startPointArray = startPoint.split(",");
		String[] endPointArray = endPoint.split(",");
		RoadPlanParameter roadPlanParameter = new RoadPlanParameter();
		roadPlanParameter.setStartX(Double.parseDouble(startPointArray[0]));
		roadPlanParameter.setStartY(Double.parseDouble(startPointArray[1]));
		roadPlanParameter.setEndX(Double.parseDouble(endPointArray[0]));
		roadPlanParameter.setEndY(Double.parseDouble(endPointArray[1]));
		roadPlanParameter.setPlanType(planType);
		// 参照标准参数
		if (rationValue <= 3.0) {
			rationValue = 15;
		} else if (rationValue > 3.0 && rationValue <= 5.5) {
			rationValue = 30;
		} else if (rationValue > 5.5 && rationValue <= 13) {
			rationValue = 55;
		} else if (rationValue > 13) {
			rationValue = 130;
		}
		roadPlanParameter.setRationValue(rationValue);
		roadPlanParameter.setLimitValue(limitValue);
		// 0 表示不将所有结果数据集合并 1 是将对应的数据合并
		List<RoadPlanEntity> roadGeometryJson = null;
		String roadGeometryStr = null;
		if (resultType == 0) { // 0 是不合并
			roadGeometryJson = networkMapLayerAnalysis.roadsPlan(roadPlanParameter);
		} else {
			roadGeometryStr = networkMapLayerAnalysis.roadPlan(roadPlanParameter);
		}
		// 规划完，就还原数据
		if (StringUtils.isNotBlank(points)) {
			netWorkAnalysisService.restoreRoadCost(pointQuery);
		}

		if (StringUtils.isBlank(roadGeometryStr) && roadGeometryJson == null) {
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),
					"路径规划失败");
		}
		if (resultType == 0) {
			return new ReponseEntity<List<RoadPlanEntity>>(ReponseStatus.SUCCESS.getCode(),
					ReponseStatus.SUCCESS.getResult(), roadGeometryJson);
		} else {
			return new ReponseEntity<Object>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),
					JSONObject.parse(roadGeometryStr));
		}

	}

	private List<String> createListPoints(String points) {
		List<String> pointQuery;
		pointQuery = new ArrayList<String>();
		String[] pointArray = points.split(";");
		StringBuilder sb = new StringBuilder();
		for (String point : pointArray) {
			String[] lonLat = point.split(",");
			sb.append("POINT").append("(").append(lonLat[0]).append(" ").append(lonLat[1]).append(")");
			pointQuery.add(sb.toString());
			sb.setLength(0);
		}
		return pointQuery;
	}

	@ApiOperation(value = "正向地理编码", notes = "正向地理编码")
	@ApiImplicitParam(name = "address", value = "地址", required = true, paramType = "query")
	@GetMapping(value = "/geocoding")
	public ReponseEntity<?> geocoding(@RequestParam(value = "address", required = true) String address) {
		if (StringUtils.isBlank(address)) {
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),
					"地理编码参数错误");
		} else {
			String location = networkMapLayerAnalysis.geocoding(address);
			return new ReponseEntity<Object>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),
					location);
		}
	}

	@ApiOperation(value = "逆向地理编码", notes = "逆向地理编码")
	@ApiImplicitParam(name = "location", value = "坐标", required = true, paramType = "query")
	@GetMapping(value = "/reverseGeocoding")
	public ReponseEntity<?> reverseGeocoding(@RequestParam(value = "location", required = true) String location) {
		String[] split = location.split(",");
		if (StringUtils.isBlank(split[0]) || StringUtils.isBlank(split[1])) {
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),
					"逆向地理编码坐标参数错误");
		} else {
			String address = networkMapLayerAnalysis.reverseGeocoding(split[0], split[1]);
			return new ReponseEntity<Object>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),
					address);
		}
	}

}
