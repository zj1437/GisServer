package org.pygisserver.web.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.util.ExcelUtil;
import org.pygisserver.common.util.ExportUtils;
import org.pygisserver.web.service.PoiQueryService;
import org.pygisserver.web.entity.PoiQueryInfo;
import org.pygisserver.web.entity.PoiQueryResult;
import org.pygisserver.web.utils.MapServerUtils;
import org.pygisserver.web.utils.UserUtil;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;

/**
 * @Description: POI查询控制类
 * @author:  
 * @date: 2019-01-26
 */
@Api(tags="poi查询接口")
@RestController
@RequestMapping("/poi/query")
@Slf4j
public class PoiQueryController {

	@Autowired
	private PoiQueryService poiService;
	
	

	private List<PoiQueryResult> poiQueryResult;

	/**
	 * 地点检索服务
	 * 匹配用户输入内容，提供输入提示功能。常与地点检索服务搭配使用,
	 * 根据距离的远近返回最匹配的10条
	 *
	 * @param keyWord 查询地名  如果为空  则查询指定范围内的所有poi数据（目前查询医院和学校）
	 * @param location 坐标点位置
	 * @param radius 对应圆形区域的半径
	 * @param lineString 对应线缓冲区    //格式  经纬组之间逗号隔开  点之间分号隔开
	 * @param bounds 对应矩形范围     //格式  矩形的左上角和右下角（108.34,34.52;110,34,30.50）
	 * @param polygon 对应多边形范围     //格式各个多边形的顶点信息，顺时针传递（322,276;405,300;442,264;399,249;344,244）
	 * @param hasBuffer 是否指定带缓冲区 ，主要针对的是多边形，因为多边形可以查缓冲区，也可以不带缓冲区，默认带缓冲区
	 * @param region 对应的查询区域  比如西安市  /雁塔区 ==
	 * @param scope 分页显示还是全部显示   1 分页显示   2 返回全部   默认分页
	 * @param tag 分类信息
	 * @param page_num 查询第几页数据
	 * @param page_size 每页查询数据量
	 * @param city_limit 城市限制 如果为1，则只匹配区域内的对应信息，2,全国范围内查询 此参数必须在有region参数的条件下使用
	 * @return json字符串
	 */
	@ApiOperation(value="poi查询",notes="获取poi的信息点")
	@ApiImplicitParams({
			@ApiImplicitParam(name="keyWord",value="关键词",required=true,paramType="query"),
			@ApiImplicitParam(name="region",value="区域",required=false,paramType="query"),
			@ApiImplicitParam(name="location",value="位置点",required=false,paramType="query"),
			@ApiImplicitParam(name="radius",value="检索半径",required=false,paramType="query"),
			@ApiImplicitParam(name="lineString",value="检索线段",required=false,paramType="query"),
			@ApiImplicitParam(name="bounds",value="矩形范围",required=false,paramType="query"),
			@ApiImplicitParam(name="bounds",value="矩形范围",required=false,paramType="query"),
			@ApiImplicitParam(name="scope",value="信息详细程度",required=false,paramType="query"),
			@ApiImplicitParam(name="tag",value="分类",required=false,paramType="query"),
			@ApiImplicitParam(name="page_num",value="查询第几页",required=false,paramType="query"),
			@ApiImplicitParam(name="page_size",value="每页显示条数",required=false,paramType="query"),
			@ApiImplicitParam(name="city_limit",value="城市限制",required=false,paramType="query")
	})
	@RequestMapping(value="/place",method=RequestMethod.GET)
	public ReponseEntity<List<PoiQueryResult>> associationPoi(@RequestParam(value = "query",required=false) String keyWord,
															  @RequestParam(value ="region",required=false,defaultValue="全国") String region,
															  @RequestParam(value ="location",required=false) String location,
															  @RequestParam(value="radius",required=false,defaultValue="1000") double distance,
															  @RequestParam(value="lineString",required=false) String lineString,
															  @RequestParam(value="bounds",required=false) String bounds,
															  @RequestParam(value="polygon",required=false) String polygon,
															  @RequestParam(value="hasBuffer",required=false,defaultValue="1") int hasBuffer,
															  @RequestParam(value="scope",required=false,defaultValue="1") int scope,
															  @RequestParam(value="tag",required=false) String tag,
															  @RequestParam(value="page_num",required=false,defaultValue="1") int pageNum,
															  @RequestParam(value="page_size",required=false,defaultValue="10") int pageSize,
															  @RequestParam(value="city_limit",required=false,defaultValue="1") int city_limit) {
		if(StringUtils.isAllEmpty(location, keyWord, lineString, bounds)) {
			poiQueryResult = new ArrayList<>();
			return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(), poiQueryResult);
		}
		Map poiInfo = getPoiInfo(distance, region, keyWord, tag, city_limit, pageSize, pageNum, scope, location, lineString, bounds, polygon);
		poiQueryResult = (List)poiInfo.get("poiData");
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),
				poiQueryResult, Integer.parseInt(poiInfo.get("count").toString()), pageNum, pageSize);
	}



	/**
	 * 构造圆形点的wkt参数（1,2）
	 * "POINT(108.30 34.25)"
	 * @param location 点位置坐标
	 */
	private String createPointWKT(String location) {
		double lon = Double.parseDouble(location.split(",")[0]);
		double lat = Double.parseDouble(location.split(",")[1]);
		return new StringBuilder().append("POINT").append("(").append(lon)
				.append(" ").append(lat).append(")").toString();
	}

	/**
	 * 构造线段wkt参数（1,2;3,4）
	 * "LINESTRING (300 270, 400 320, 460 270)" 
	 * @param lineString 查询线段参数
	 */
	private String createLineStringtWKT(String lineString) {
		String[] lineStrArray = lineString.split(";");
		StringBuilder sb = new StringBuilder("LINESTRING");
		sb.append("(");
		int index = 0;
		for (String point : lineStrArray) {
			String[] xyz = point.split(",");
			if (xyz.length == 2) {  //二维平面坐标
				sb.append(xyz[0]).append(" ").append(xyz[1]);
			} else if (xyz.length == 3) {  //带三维坐标
				sb.append(xyz[0]).append(" ").append(xyz[1]).append(" ").append(xyz[2]);
			}
			if (index < (lineStrArray.length - 1)) {
				sb.append(",");
			}
			index++;
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * 多边形wkt参数（1,2;3,4）
	 * POLYGON ((322 276, 405 300, 442 264, 399 249, 344 244, 324 251, 322 276))
	 * @param polygon 查询线段参数
	 * @param isRectangle 是否为矩形
	 */
	private String createPolygonWKT(String polygon,boolean isRectangle) {
		final String[] pointArray;
		StringBuilder sb = new StringBuilder("POLYGON");
		String[] oldArray = polygon.split(";");
		sb.append("((");
		if (isRectangle) {
			pointArray = new String[5];
			//构造对应的数据多边形点数据
			String oldArray0 = oldArray[0];
			String oldArray1 = oldArray[1];
			String[] leftUpperPoint = oldArray0.split(",");
			String[] rightLowerPoint = oldArray1.split(",");
			pointArray[0] = oldArray0;
			pointArray[1] = rightLowerPoint[0] + "," + leftUpperPoint[1];
			pointArray[2] = oldArray1;
			pointArray[3] = leftUpperPoint[0] + "," + rightLowerPoint[1];
			pointArray[4] =  oldArray0;
		}else {
			pointArray = Arrays.copyOf(oldArray, oldArray.length+1);
			//将起点增加到最后一个点位置
			pointArray[oldArray.length] = oldArray[0];
		}
		int index = 0;
		for (String point : pointArray) {
			String[] xyz = point.split(",");
			if (xyz.length == 2) {  //二维平面坐标
				sb.append(xyz[0]).append(" ").append(xyz[1]);
			} else if (xyz.length == 3) {  //带三维坐标
				sb.append(xyz[0]).append(" ").append(xyz[1]).append(" ").append(xyz[2]);
			}
			if (index < pointArray.length - 1) {
				sb.append(",");
			}
			index++;
		}
		sb.append("))");
		return sb.toString();
	}


	private List<String> analysisKeywords(String text) {
		List<String> list = new ArrayList<String>();
		StringReader re = new StringReader(text);
		IKSegmenter ik = new IKSegmenter(re, true);
		Lexeme lex = null;
		try {
			while((lex=ik.next()) != null) {
				//如果分词中出现了地名则剔除掉
				if (lex.getLexemeText().length() >= 2) {
					list.add(lex.getLexemeText());
				}
			}
		} catch (IOException e) {
			log.error("PoiQueryController analysisKeywords has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "POI查询关键字"));
		}

		//按照字符length的先后顺序进行排序
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o2.length() - o1.length();
			}
		});
		return list;
	}

	/**
	 * POI查询数据导出csv
	 *
	 * @param tag 分类信息
	 * @param location 坐标点位置
	 * @param region 对应的查询区域
	 * @param keyWord 查询地名
	 * @param city_limit 城市限制 如果为1，则只匹配区域内的对应信息，2,全国范围内查询 此参数必须在有region参数的条件下使用
	 * @return json字符串
	 */
	@ApiOperation(value="poi查询导出",notes="获取poi的信息点")
	@RequestMapping(value="/export",method=RequestMethod.GET)
	public void associationPoiExport(HttpServletResponse response) {
		String fileName = "POI查询" + System.currentTimeMillis() + ".geojson";
		List<Map<String, Object>> poiResultMaps = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(poiQueryResult)) {
			poiQueryResult.forEach(doc ->{
				Map<String, Object> poiResultMap = new HashMap<>();
				Coordinate coordinate = new Coordinate(doc.getLongitude(), doc.getLatitude());
				Object getgeometryObject = MapServerUtils.getgeometryObject(0, coordinate);
				poiResultMap.put("geometry", getgeometryObject);
				poiResultMap.put("id", doc.getId());
				poiResultMap.put("type", "Feature");
				Map<String, String> propertiesMap = new HashMap<>(4);
				propertiesMap.put("名称", doc.getName());
				propertiesMap.put("地址", doc.getAddress());
				propertiesMap.put("电话", doc.getPhone());
				poiResultMap.put("properties", propertiesMap);
				poiResultMaps.add(poiResultMap);
			});
		}
		JSONArray jsonArray = JSONArray.fromObject(poiResultMaps);
		String exportInfo = jsonArray.toString();
		ExportUtils.doExportGeoJson(response, fileName, exportInfo);
	}
	
	//下载poi模板
	@ApiOperation(value="poi模板下载",notes="poi模板下载")
	@RequestMapping(value = "/download",method=RequestMethod.GET)
	public void  loadTemplate(HttpServletResponse response) {
		
		File file = null;
		try {
			file = ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX + "template/POI数据模板.xlsx");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String url = file.getPath();
		ExcelUtil.exportExcel(response, "PoiTemplate", file);
	}
	//poi导入
	@ApiOperation(value="poi导入",notes="poi导入")
	@RequestMapping(value = "/import", method = RequestMethod.POST)
    public ReponseEntity<String> PoiExportDB(HttpServletRequest req,@RequestParam("file")MultipartFile[] files){
		
		String reqIP = UserUtil.getRequestIp(req);
	
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),poiService.ExportTableToPostGis(reqIP,files));
	}
	
	
	private Map getPoiInfo(double distance, String region, String keyWord, String tag, int city_limit, int pageSize,
						   int pageNum, int scope, String location, String lineString, String bounds, String polygon) {
		//构造查询参数
		PoiQueryInfo queryParam = new PoiQueryInfo(region);
		if (StringUtils.isNotBlank(keyWord)) {
			queryParam.setKeyWords(keyWord);
			queryParam.setKeyWordsLength(keyWord.length());
		}
		if (StringUtils.isNotBlank(tag)) {
			queryParam.setType(tag);
		}
		queryParam.setDistance(distance);
		queryParam.setCityLimit(city_limit);
		queryParam.setPageSize(pageSize);
		queryParam.setPageNum(pageNum);
		queryParam.setScope(scope);
		//如果location不为空，则将其转为查询的点信息
		if (StringUtils.isNotBlank(location)) {
			String pointWkt = createPointWKT(location);
			queryParam.setPoint(pointWkt);
		} else if (StringUtils.isNotBlank(lineString)) {
			//构造线段
			String lineStringWkt = createLineStringtWKT(lineString);
			queryParam.setLineString(lineStringWkt);
		} else if (StringUtils.isNotBlank(bounds)) {
			//构造矩形
			String boundsWkt = createPolygonWKT(bounds,true);
			queryParam.setBounds(boundsWkt);
		} else if (StringUtils.isNotBlank(polygon)) {
			//构造多边形
			String polygonWkt = createPolygonWKT(bounds,false);
			queryParam.setBounds(polygonWkt);
		}
		int count = poiService.queryBestPoiCount(queryParam);
		List<PoiQueryResult> poiList;
		if (count > 0) {
			queryParam.setCount(count);
			poiList = poiService.queryBestPoiInfo(queryParam);
		} else {	//如果count为0，再分词查看当前是否可以继续
			if (StringUtils.isNotBlank(keyWord)) {
				List<String> analyseWord = analysisKeywords(keyWord);
				queryParam.setKeyWordList(analyseWord);
				count = poiService.queryBestPoiCount(queryParam);
				if (count > 0) {
					poiList = poiService.queryBestPoiInfo(queryParam);
				} else {
					poiList = new ArrayList<>();
				}
			} else {
				poiList = new ArrayList<>();
			}

		}
		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("count", count);
		resultMap.put("poiData", poiList);
		return resultMap;
	}
	
}
