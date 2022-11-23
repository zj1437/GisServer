package org.pygisserver.web.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.service.StyleInfoService;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.entity.RasterColorMap;
import org.pygisserver.web.entity.StyleInfo;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 
 * @author:  
 * @date: 2019-12-19
 */
@Api(value="样式操作接口",tags="样式操作接口")
@RestController
@RequestMapping("/style")
@Slf4j
public class StyleInfoController {
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private SystemLogService systemLogService;

	@Autowired
	private StyleInfoService styleInfoService;
	
	/**
	 * 获取样式集合
	 * @param type  类型
	 * @param fileUrl 文件url
	 * @return
	 */
	@ApiOperation(value = "查询样式集合",notes="按条件查询样式集合")
	@ApiImplicitParam(name="queryCondition",value="查询条件",required=false,paramType="body")
	@PostMapping("/query")
	public ReponseEntity<List<StyleInfo>> queryStyleInfoByType(@RequestBody(required=false) RequestQueryCondition queryCondition){
		int num = styleInfoService.queryStyleCount(queryCondition);
		if (num > 0) {
			queryCondition.getPageInfo().setTotalCount(num);
			List<StyleInfo> list = styleInfoService.queryStyleInfoByCondition(queryCondition);
			if (list != null && !list.isEmpty()) {
				return new ReponseEntity<List<StyleInfo>>(ReponseStatus.SUCCESS.getCode(), 
														  ReponseStatus.SUCCESS.getResult(),
														  list,
														  num); 
			}
		}
		return new ReponseEntity<List<StyleInfo>>(ReponseStatus.SUCCESS.getCode(), 
												  ReponseStatus.SUCCESS.getResult(),
												  new ArrayList<StyleInfo>()); 
	}
	
	/**
	 * 获取样式集合
	 * @param type  类型
	 * @param fileUrl 文件url
	 * @return
	 */
	@ApiOperation(value = "根据文件路径和样式类型查询样式集合",notes="根据文件路径和样式类型查询样式集合")
	@ApiImplicitParams({
		@ApiImplicitParam(name="fileUrl",value="文件路径",required=true,paramType="query"),
		@ApiImplicitParam(name="type",value="样式类型",required=true,paramType="path")
	})
	@GetMapping("/query/{type}")
	public ReponseEntity<List<StyleInfo>> queryStyleInfoList(@RequestParam(value="fileUrl") String fileUrl,
												 		 	 @PathVariable String type,
												 		 	 HttpServletRequest req){
		if (CommonUtil.isTiledServer(type)) {
			return new ReponseEntity<List<StyleInfo>>(ReponseStatus.SUCCESS.getCode(), 
			 		  ReponseStatus.SUCCESS.getResult(),
			 		  null); 
		}
		String reqIP = UserUtil.getRequestIp(req);
		List<StyleInfo> list = styleInfoService.queryStyleInfoList(type, fileUrl,reqIP);
		if (list != null) {
			return new ReponseEntity<List<StyleInfo>>(ReponseStatus.SUCCESS.getCode(), 
					 						 		  ReponseStatus.SUCCESS.getResult(),
					 						 		  list); 
		}else {
			return new ReponseEntity<List<StyleInfo>>(ReponseStatus.ERROR.getCode(), 
					 								  ReponseStatus.ERROR.getResult());
		}
	}
	
	/**
	 * 删除单个sld文件
	 * @param ids
	 * @return
	 */
	@ApiOperation(value = "删除样式的sld文件",notes="根据样式id删除样式文件")
	@ApiImplicitParam(name="id",value="样式id",required=true,paramType="path")
	@PostMapping("/delete/{styleId}")
	public ReponseEntity<String> deleteStyleInfoById(HttpServletRequest req,@PathVariable(value="styleId") int id){
		List<Integer> list = new ArrayList<Integer>(1);
		list.add(id);
		styleInfoService.deleteStyleInfo(req, list);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "文件删除成功！");
	}
	
	
	/**
	 * 批量删除sld文件
	 * @param ids
	 * @return
	 */
	@ApiOperation(value = "批量删除样式的sld文件",notes="根据多个样式id删除样式文件")
	@ApiImplicitParam(name="ids",value="样式id",required=true,paramType="body")
	@PostMapping("/delete")
	public ReponseEntity<String> deleteStyleInfoByIds(HttpServletRequest req,@RequestBody List<Integer> ids){
		styleInfoService.deleteStyleInfo(req, ids);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "文件删除成功！");
	}
	
	/**
	 * 获取栅格色带数据
	 * @return
	 */
	@ApiOperation(value = "获取栅格色带数据",notes="获取栅格色带数据")
	@GetMapping("/color")
	public ReponseEntity<List<RasterColorMap>> queryColorMapList(){
		List<RasterColorMap> list = styleInfoService.queryColorMap();
		if (list != null) {
			return new ReponseEntity<List<RasterColorMap>>(ReponseStatus.SUCCESS.getCode(), 
					 						 		  ReponseStatus.SUCCESS.getResult(),
					 						 		  list); 
		}else {
			return new ReponseEntity<List<RasterColorMap>>(ReponseStatus.ERROR.getCode(), 
					 								  ReponseStatus.ERROR.getResult());
		}
	}
	
	/**
	 * TODO:色带的数据，直接存储到数据库
	 * @param legendName
	 * @param req
	 * @param rep
	 */
	@ApiOperation(value = "保存色带数据",notes="保存色带数据")
	@ApiImplicitParam(name="legendName",value="色带数据文件名",required=true,paramType="path")
	@GetMapping("/legend/{legendName:.+}")
	public void loadColorMap(@PathVariable(value="legendName") String legendName,
											  HttpServletRequest req,
											  HttpServletResponse rep) {
		//获取到文件路径
		String path = System.getProperty("SYSTEM_COLOR_PATH");
		OutputStream os = null;
		try {
			os = rep.getOutputStream();
			//读取文件
			byte[] buffer = FileUtil.getFileInfo(path + File.separator + legendName);
			os.write(buffer);
		} catch (IOException e) {
			log.error("StyleInfoController loadColorMap has error:" + ExceptionsUtil.stackTraceToStr(e));
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
					ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE,
					legendName + "色带文件"),UserUtil.getUserName(), "文件管理", UserUtil.getRequestIp(req)));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, legendName + "染色带数据"));
		}
	}
	
	/**
	 * 支持导入sld文件，支持批量导入
	 * @param files
	 * @param req
	 */
	@ApiOperation(value = "导入sld文件",notes="导入sld文件，既可以单个导入也可以批量导入")
	@ApiImplicitParam(name="files",value="导入的文件",required=true,paramType="query")
	@PostMapping("/import")
	public ReponseEntity<?> importCustomSld(HttpServletRequest req,@RequestParam("file")MultipartFile[] files) {

		if (files == null || files.length == 0) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "上传文件为空"); 
		}
		//导入到指定位置
		String destPath = System.getProperty("SYSTEM_STYLE_PATH");
		List<String> uploadMsg = new ArrayList<String>();
		//文件上传处理
		for (MultipartFile multipartFile : files) {
			//先校验文件是否合法
			String msg = styleInfoService.validateSldIllegel(req,multipartFile,destPath);
			if (StringUtils.isNotBlank(msg) && !"文件格式正确".equals(msg) && !"文件重名".equals(msg)) {
				uploadMsg.add(msg);
				continue;
			}
			//对于单个sld文件上传或批量上传，不再后台重命名样式名称，将该信息提示给用户，由用户去修改
			File destFile = new File(destPath,multipartFile.getOriginalFilename());
			try {
				//处理业务流程
				multipartFile.transferTo(destFile);
				String result = styleInfoService.importCustomSld(req,destFile,destPath,multipartFile.getOriginalFilename());
				//当上传单个sld文件时，如果返回信息不为空且提示已经在样式列表中就不再删除该sld文件，因为在上面的文件写入流写入文件时已经将原有的sld文件
				//覆盖，如果此时删除该文件将导致原有的样式出现问题
				//如果上传的是压缩包，则需要在业务处理完成后将该压缩包删除
				if(((result != null && !result.startsWith("样式列表中")) || destFile.getName().endsWith(".zip")) && destFile.exists()) destFile.delete();
				if(StringUtils.isNoneBlank(result)) {
					uploadMsg.add(result);
				}
			} catch (IllegalStateException | IOException e) {
				log.error("StyleInfoController importCustomSld has error:" + ExceptionsUtil.stackTraceToStr(e));
				uploadMsg.add(MessageFormat.format("样式文件{0}上传失败", multipartFile.getOriginalFilename()));
			}
		}
		if (!uploadMsg.isEmpty()) {
			return new ReponseEntity<List<String>>(ReponseStatus.ERROR.getCode(),"导入失败",uploadMsg);
		}
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),"导入成功！");
	}

	/**
	 * 根据样式查询可适用的所有服务名称
	 *
	 * @param req req
	 * @param typeId 样式名称
	 * @return ReponseEntity
	 */
	@ApiOperation(value = "保存色带数据",notes="保存色带数据")
	@ApiImplicitParam(name="typeId",value="样式类型id",required=true,paramType="path")
	@GetMapping("/query/suitservers")
	public ReponseEntity<?> queryAllServerNameByStyle(HttpServletRequest req, @RequestParam("typeId")int typeId) {
		String reqIP = UserUtil.getRequestIp(req);
		if (typeId > 5 || typeId < 1) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), ReponseStatus.ERROR.getResult(), "样式类型错误!");
		}
		List<DataServerEntity> server = styleInfoService.queryServerByStyle(typeId, reqIP);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(), server);
	}
}
