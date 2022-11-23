package org.pygisserver.web.controller;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.web.entity.ModelEntity;
import org.pygisserver.web.service.ModelServerService;
import org.pygisserver.web.service.UserService;
import org.pygisserver.web.entity.ModelProduct;
import org.pygisserver.web.entity.OnlineModelEntity;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;


/**
 * @Description: 模型服务管理 
 * @author: 
 * @date: 2018-11-02
 */
@Api(value="模型服务",tags="模型服务")
@RestController
@RequestMapping(value="/model")
@Slf4j
public class ModelServerController {
	@Autowired
	private ModelServerService modelServerService;
	
	@Autowired
	private UserService userService;

	/**
	 * 获取模型服务列表
	 * TODO 考虑后期是否将分页和检索置于后台
	 * @return 返回服务列表
	 */
	@ApiOperation(value="模型服务列表",notes="获取当前模型列表服务")
	@ApiImplicitParam(name="queryCondition",value="查询条件",required=false,paramType="body")
	@RequestMapping(value="/queryModelServer",method=RequestMethod.POST)
	public ReponseEntity<List<ModelEntity>> queryModelServer(@RequestBody(required=false) RequestQueryCondition queryCondition) {
		int num = modelServerService.queryModelServerCount(queryCondition);
		if (num > 0) {
			List<ModelEntity> list = modelServerService.queryModelServerList(queryCondition);
			return new ReponseEntity<List<ModelEntity>>(ReponseStatus.SUCCESS.getCode(), 
														ReponseStatus.SUCCESS.getResult(), 
														list,
														num);
		} else {
			return new ReponseEntity<List<ModelEntity>>(ReponseStatus.SUCCESS.getCode(), 
														ReponseStatus.SUCCESS.getResult(), 
														new ArrayList<ModelEntity>(),
														num);
		}
	}
	
	/**
	 * 获取倾斜射影服务列表
	 * TODO 考虑后期是否将分页和检索置于后台
	 * @return 返回服务列表
	 */
	@ApiOperation(value="倾斜射影列表",notes="获取当前倾斜射影数据列表")
	@ApiImplicitParam(name="queryCondition",value="查询条件",required=false,paramType="body")
	@RequestMapping(value="/queryTiltServer",method=RequestMethod.POST)
	public ReponseEntity<List<ModelEntity>> queryTiltServer(@RequestBody(required=false) RequestQueryCondition queryCondition) {
		int num = modelServerService.queryTiltServerCount(queryCondition);
		if (num > 0) {
			List<ModelEntity> list = modelServerService.queryTiltServerList(queryCondition);
			return new ReponseEntity<List<ModelEntity>>(ReponseStatus.SUCCESS.getCode(), 
														ReponseStatus.SUCCESS.getResult(), 
														list,
														num);
		} else {
			return new ReponseEntity<List<ModelEntity>>(ReponseStatus.SUCCESS.getCode(), 
														ReponseStatus.SUCCESS.getResult(), 
														new ArrayList<ModelEntity>(),
														num);
		}
	}
	
	/**
	 * 预览当前模型服务
	 * @param id 产品id
	 * @return
	 */
	@ApiOperation(value="预览模型",notes="预览当前的模型列表")
	@ApiImplicitParams({
		@ApiImplicitParam(name="serverType",value="服务类型",required=true,dataType="Integer",paramType="path"),
		@ApiImplicitParam(name="id",value="模型id",required=true,dataType="Integer",paramType="path")
	})
	@RequestMapping(value="/modelpreview/{serverType}/{id}",method=RequestMethod.GET)
	public ReponseEntity<List<String>> modelPreview(@PathVariable int serverType,
					                  @PathVariable int id,
									  HttpServletRequest request,
									  HttpServletResponse reponse){
		try {
			String previewUrl = "";
			String reqIP = UserUtil.getRequestIp(request);
			if (serverType == 0) {
				ModelProduct product = modelServerService.getProductInfo(id);
				File file = new File(product.getFileUrl());
				//如果是3ds,则直接获取其压缩文件
				if ("3ds".equalsIgnoreCase(product.getFileType())) {
					if (file.getName().endsWith(".zip")) {
						previewUrl = product.getFileUrl();
					}else {
						previewUrl = file.getAbsolutePath() + ".zip";
					}
				}else {
					//如果zip结尾，则获取查看其是否为
					if (file.getName().endsWith(".zip")) {
						String fileName = file.getName().split("\\.")[0];
						if (fileName.endsWith("-3ds")) {
							previewUrl = product.getFileUrl();
						}else {
							File tempFile = new File(String.format(file.getParent() + File.separator + "%s-3ds.zip", file.getName().split("\\.")[0]));
							previewUrl = tempFile.getAbsolutePath();
						}
					}else {
						File tempFile = new File(String.format(file.getParent() + File.separator + "%s-3ds.zip", file.getName().split("\\.")[0]));
						previewUrl = tempFile.getAbsolutePath();
					}
				}
			}else {
				//解析xml，获取其中的Node值，并传递给后台
				return new ReponseEntity<List<String>>(ReponseStatus.SUCCESS.getCode(), 
													   ReponseStatus.SUCCESS.getResult(), 
													   modelServerService.getTiltServerUrl(id,reqIP));
			}
			reponse.setCharacterEncoding("UTF-8");
			OutputStream os = reponse.getOutputStream();
			byte[] buffer = modelServerService.getModelFileStream(request,previewUrl,null,true);
			os.write(buffer);
			return null;
		} catch (IOException e) {
			log.error("ModelServerController modelPreview has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "模型数据信息"));
		}
	}
	
	/**
	 * 
	 * @param serverName 服务名称
	 * @return
	 */
	@ApiOperation(value="模型服务唯一校验",notes="判断当前模型是否已经发布过")
	@ApiImplicitParam(name="serverName",value="服务类型",required=true,paramType="path")
	@RequestMapping(value="/ispublised/{serverName}",method=RequestMethod.GET)
	public ReponseEntity<Boolean> isPublishedModel(@PathVariable String serverName){
		boolean flag = modelServerService.isPublished(serverName);
		return new ReponseEntity<Boolean>(ReponseStatus.SUCCESS.getCode(), 
				  ReponseStatus.SUCCESS.getResult(), 
				  flag);
	}
	
	/**
	 * 模型数据发布
	 * 
	 * @param entity 模型数据
	 * @return
	 */
	@ApiOperation(value="模型服务发布",notes="发布当前的模型数据服务")
	@RequestMapping(value="/publish",method=RequestMethod.POST)
	public ReponseEntity<String> publishModel(HttpServletRequest request,@RequestBody @ApiParam("模型数据") ModelEntity entity) {
		String baseUrl = queryServerUrl(request);
		entity.setRequesturl(baseUrl);
		String reqIP = UserUtil.getRequestIp(request);
		Map<String,Object> result = modelServerService.publishModel(entity,reqIP);
		
		int code = (Integer)result.get("result");
		List<ModelProduct> bugModels = (List<ModelProduct>)result.get("bugModels");
		String returnInfo = null;
		int bModels = bugModels.size();
		if(bModels > 0) {
			StringBuffer sb = new StringBuffer();
			String tipInfo = bugModels.get(0).getFileType().split(";")[1];
			for (ModelProduct modelProduct : bugModels) {
				String zipName = modelProduct.getFileType().split(";")[0];
				sb.append(zipName).append(";");
			}
			returnInfo = sb.toString().substring(0, sb.toString().length() -1) + tipInfo;
		}
		if (code == 0 && bModels > 0) {
			return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), "发布部分成功," + returnInfo);
		} else if(code == 0) {
			return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), "发布成功");
		} else if (code == 1 && bModels > 0) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), returnInfo);
		} else {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "发布失败，请检查后再试！");
		}
	}
	
	private String queryServerUrl(HttpServletRequest req) {
		String host = req.getServerName();
		if (host.equals("localhost")) {
			host = CommonUtil.getLocalHost();
		}
		StringBuffer sb = new StringBuffer(req.getScheme());
        sb.append("://").append(host).append(":").append(req.getServerPort())
        .append(req.getContextPath()).append("/");
        return sb.toString();
	}
	
	/**
	 * 模型服务编辑
	 * 
	 * @param entity 模型数据
	 * @return
	 */
	@ApiOperation(value="模型服务编辑",notes="编辑当前的模型数据服务")
	@RequestMapping(value="/edit",method=RequestMethod.POST)
	public ReponseEntity<String> editModel(HttpServletRequest request, @RequestBody @ApiParam("模型数据") ModelEntity entity) {
		String reqIP = UserUtil.getRequestIp(request);
		String baseUrl = queryServerUrl(request);
		entity.setRequesturl(baseUrl);
		Map<String,String> results = modelServerService.editModelServer(entity,reqIP);
		int totalModels = Integer.valueOf(results.get("totalModels"));
		String result = results.get("result");
		int bugModelNum = 0;
		if(result != null) {
			bugModelNum = result.split(";").length;
			result = result + "解压失败，请检查数据是否正常";
		}
		if(result != null && bugModelNum > 0) {
			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(), "编辑失败," + result);
		} else {
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "编辑成功");
		}
		
//		if(result != null && bugModelNum == entity.getProducts().size()) {
//			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(), "编辑失败," + result);
//		} else if(result != null && bugModelNum != entity.getProducts().size()) {
//			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "编辑部分成功," + result);
//		} else {
//			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "编辑成功");
//		}
	}
	
	/**
	 * 模型服务删除
	 * @param id 删除id
	 * @return
	 */
	@ApiOperation(value="模型服务删除",notes="删除单个模型服务")
	@ApiImplicitParam(name="id",value="模型服务id",required=true,paramType="path")
	@RequestMapping(value="/delete/{id}",method=RequestMethod.GET)
	public ReponseEntity<String> deleteModelServer(HttpServletRequest request,@PathVariable int id) {
		List<Integer> list = new ArrayList<Integer>();
		list.add(id);
		modelServerService.deleteModelServer(list);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "删除成功");
	}
	
	
	/**
	 * 批量模型服务删除
	 * @param ids 模型服务id
	 * @return
	 */
	@ApiOperation(value="模型服务删除",notes="批量删除多个模型服务")
	@ApiImplicitParam(name="ids",value="模型服务id集合",required=true,paramType="body")
	@RequestMapping(value="/delete",method=RequestMethod.POST)
	public ReponseEntity<String> deleteModelServers(HttpServletRequest request,@RequestBody List<Integer> ids) {
		modelServerService.deleteModelServer(ids);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "删除成功");
	}
	
	/**
	 * 模型服务的启停
	 * @param id 模型服务id
	 * @return
	 */
	@ApiOperation(value="模型服务启停",notes="启动和暂停某个模型服务")
	@ApiImplicitParam(name="id",value="模型服务id",required=true,dataType="Integer",paramType="path")
	@RequestMapping(value="/update/{id}",method=RequestMethod.GET)
	public ReponseEntity<String> updateServerStatus(HttpServletRequest request,@PathVariable int id) {
		modelServerService.updateModelServerStatus(id);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "更新成功");
	}
	
	/**
	 * 模型服务rest接口
	 * @param serverName 服务名称
	 * @param productName 产品名称
	 * @param modelFileName 产品名称
	 * @param subName subName
	 * @param response 响应实体
	 * @throws IOException 
	 */
	@ApiOperation(value="服务访问",notes="获取当前模型数据")
	@ApiImplicitParams({
		@ApiImplicitParam(name="server",value="服务名称",required=true,paramType="path"),
		@ApiImplicitParam(name="productName",value="产品名称",required=true,paramType="path"),
		@ApiImplicitParam(name="modelFileName",value="产品名称",required=true,paramType="path"),
		@ApiImplicitParam(name="subName",value="子名称",required=false,paramType="query")
	})
	@RequestMapping(value="/{server}/{productName}/{modelFileName}/**",method=RequestMethod.GET)
//	@RequestMapping(value="/{server}/{productName}",method=RequestMethod.GET)
	public void getModelDataReponse(HttpServletRequest request,
									 @PathVariable(value="server") String serverName,
									 @PathVariable(value="productName") String productName,
									 @PathVariable(value="modelFileName") String modelFileName,
									 @RequestParam(value="subName",required=false) String subName,
									 HttpServletResponse response) {
		//根据服务名称和产品名称获取模型所在位置
		boolean isModel = true;

		// online倾斜摄影
		if ("webqxsy".equals(serverName) && StringUtils.isNoneBlank(productName, modelFileName)) {
			try {
				//首先查看当前文件服务是否已经发布
				int status = modelServerService.queryCurrentServerStatus(productName);
				//0表示暂停
				if (status == 0) {
					throw new BaseException(500, "当前服务被暂停，请联系管理员或服务发布人");
				}
				modelServerService.onlineQxsyData(request, response, productName, modelFileName);
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (StringUtils.isNotBlank(serverName) && StringUtils.isNotBlank(productName)) {
			//首先查看当前文件服务是否已经发布
			int status = modelServerService.queryCurrentServerStatus(serverName);
			//0表示暂停
			if (status == 0) {
				throw new BaseException(500, "当前服务被暂停，请联系管理员或服务发布人");
			}
			//获取文件所在位置
			StringBuilder sb = new StringBuilder(modelServerService.queryFileUrl(serverName, productName));
			if (StringUtils.isNotBlank(subName)) {
				isModel = false;
				//倾斜射影需要访问不同用户的文件，如果是游客，则访问admin的配置文件
				if (subName.endsWith(".fgt") 
						|| subName.endsWith(".lfp") 
						|| subName.endsWith(".scp")) {
					Subject subject = SecurityUtils.getSubject();
					String userName = null;
					if (subject != null && subject.getPrincipal() != null) {
						userName = subject.getPrincipal().toString();
					}
					if (StringUtils.isBlank(userName)) {
						userName = userService.querySystemUser();
					}
					sb.append(File.separator).append(userName).append("_").append(serverName).append(".").append(subName.split("\\.")[1]);
				} else {
					sb.append(File.separator).append(subName);
				}
			} else {
				//模型数据 web端数据需要考虑gltf有内部文件的特例
				final String commonURI = "/pygisserver/model/" + serverName + "/" + productName + "/";
				String reqUri = null;
				try {
					reqUri = URLDecoder.decode(request.getRequestURI(), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.error("");
				}
				String subURI = reqUri.replace(commonURI, "");
				File file = new File(sb.toString());
				if (!subURI.equals(productName)) {
					if (subURI.endsWith(".zip")) {
						subURI = sb.substring(sb.lastIndexOf("/") + 1, sb.lastIndexOf(".")) + ".zip";
					} else {
						subURI = sb.substring(sb.lastIndexOf("/") + 1);
					}
					file = new File(file.getParentFile(), subURI);
					sb.setLength(0);
					sb.append(file.getAbsolutePath());
				}
				if (file.exists() && file.isDirectory()) {
					sb.append(File.separator).append(productName);
				}
				
				
			}
			OutputStream os = null;
			try {
				response.setCharacterEncoding("UTF-8");
				os = response.getOutputStream();
				byte[] buffer = modelServerService.getModelFileStream(request,sb.toString(),serverName,isModel);
				os.write(buffer);
			} catch (IOException e) {
				log.error("ModelServerController getModelDataReponse has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "模型数据信息"));
			}
		}
	}
	
	/**
	 * 服务提供给online查询online可用的模型接口
	 * @param req
	 * @return
	 */
	@ApiOperation(value="查询online可用的模型接口",notes="查询online可用的模型接口")
	@GetMapping(value="/online/models")
	public ReponseEntity<List<OnlineModelEntity>> getOnlineModels(HttpServletRequest req) {
		StringBuilder sb = new StringBuilder(req.getScheme());
        sb.append("://").append(req.getServerName()).append(":").append(req.getServerPort()).append(req.getContextPath()).append("/");
		RequestQueryCondition queryCondition = new RequestQueryCondition();
		return new ReponseEntity<List<OnlineModelEntity>>(ReponseStatus.SUCCESS.getCode(), 
				ReponseStatus.SUCCESS.getResult(),modelServerService.queryOnlineModels(queryCondition,sb.toString()));
	}
	
	/**
	 * 服务提供给online查询online可用的倾斜摄影
	 * @param req
	 * @return
	 */
	@ApiOperation(value="查询online可用的倾斜摄影",notes="查询online可用的倾斜摄影")
	@GetMapping(value="/online/onlinePhtographys")
	public ReponseEntity<List<OnlineModelEntity>> getOnlineQXSY(HttpServletRequest req) {
		StringBuilder sb = new StringBuilder(req.getScheme());
        sb.append("://").append(req.getServerName()).append(":").append(req.getServerPort()).append(req.getContextPath()).append("/");
		RequestQueryCondition queryCondition = new RequestQueryCondition();
		queryCondition.setServerStatus(1);
		return new ReponseEntity<List<OnlineModelEntity>>(ReponseStatus.SUCCESS.getCode(), 
				ReponseStatus.SUCCESS.getResult(), modelServerService.getOnlineQXSY(queryCondition,sb.toString()));
	}
	
	@ApiOperation(value="服务访问",notes="获取当前模型数据")
	@ApiImplicitParams({
		@ApiImplicitParam(name="server",value="服务名称",required=true,paramType="path"),
		@ApiImplicitParam(name="productName",value="产品名称",required=true,paramType="path")
	})
	@GetMapping(value = "/online/thumbal/{server}/{productName:.+}")
	public ReponseEntity<String> getOnlineThumbal(HttpServletRequest request,
			 @PathVariable(value="server") String serverName,
			 @PathVariable(value="productName") String productName,
			 HttpServletResponse response) {
		if (StringUtils.isBlank(serverName) || StringUtils.isBlank(productName) || (!productName.endsWith(".png")
				&& !productName.endsWith(".jpeg") && !productName.endsWith(".bmp"))) {
			throw new ArgumentException(500, "请求缩略图参数不合法，请确保参数无误后再试！");
		}
		//0表示暂停
		if (modelServerService.queryCurrentServerStatus(serverName) == 0) {
			throw new BaseException(500, "当前服务被暂停，请联系管理员或服务发布人");
		}
		//获取文件所在位置
		String format = null;
		String thmbalPath = null;
		File serverUrl = new File(modelServerService.queryFileUrl(serverName, productName));
		String pPath = serverUrl.isDirectory() ? serverUrl.getAbsolutePath() : serverUrl.getParent();
		File thmbalFile = new File(pPath, productName);
		if (thmbalFile.exists()) {
			format = productName.substring(productName.lastIndexOf("."));
			thmbalPath = thmbalFile.getAbsolutePath();
		}

		if (StringUtils.isAllEmpty(format, thmbalPath)) {
			return null;
		}

		byte[] buff = new byte[1024];
		try (FileInputStream fileInputStream = new FileInputStream(new File(thmbalPath))){
			BufferedInputStream bis = new BufferedInputStream(fileInputStream);
			response.setCharacterEncoding("UTF-8");
			response.setHeader("content-type", "image/" + format);
			OutputStream os = response.getOutputStream();
			while ((bis.read(buff)) != -1) {
				os.write(buff, 0, buff.length);
				os.flush();
			}
		} catch (IOException e) {
			log.error("ModelServerController getModelDataReponse has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "模型数据信息"));
		}
		return null;
	}
	
	
	/**
	 * 获取当前模型服务的处理进度
	 * @param serverName
	 * @return
	 */
	@ApiOperation(value="获取当前模型服务的处理进度",notes="获取当前模型服务的处理进度")
	@ApiImplicitParam(name="serverName",value="服务名称",required=true,paramType="path")
	@GetMapping("/process/{serverName}")
	public ReponseEntity<Double> getCurrentDealProcess(@PathVariable("serverName") String serverName){
		return new ReponseEntity<Double>(ReponseStatus.SUCCESS.getCode(),
										 ReponseStatus.SUCCESS.getResult(),
										 modelServerService.getCurrentDealResult(serverName));
	}
	
	
	/**
	 * 根据文件类型获取对应的位置信息
	 * @param fileUrl
	 */
	@ApiOperation(value="根据文件类型获取对应的位置信息",notes="根据文件类型获取对应的位置信息")
	@ApiImplicitParam(name="fileUrl",value="文件url",required=true,paramType="query")
	@RequestMapping(value="/position",method=RequestMethod.GET)
	public ReponseEntity<String> getTiltPosition(@RequestParam("fileUrl") String fileUrl, HttpServletRequest req) {
		String currUser = UserUtil.getUserName();
		String position = modelServerService.queryFilePosition(fileUrl,currUser);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
										 ReponseStatus.SUCCESS.getResult(),
										 position);
	}

	/**
	 * online倾斜摄影偏移高度
	 *
	 * @param entity 模型数据
	 * @return String
	 */
	@ApiOperation(value="Online模型服务编辑",notes="编辑当前的模型数据服务")
	@RequestMapping(value="/editOnline",method=RequestMethod.POST)
	public ReponseEntity<String> editModelOnline(@RequestBody @ApiParam("模型数据") ModelEntity entity) {
		int result = modelServerService.updateOffsetHeight(entity.getId(), entity.getOffsetHeight());
		if (result > 0) {
			return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), "编辑成功");
		} else {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "编辑失败");
		}
	}
	@RequestMapping(value="/queryGltfFile/{serverName}/**",method=RequestMethod.GET)
	public ReponseEntity<String> queryGltfFile(HttpServletRequest request, HttpServletResponse response,
											   @PathVariable String serverName) {
		try {
			response.setCharacterEncoding("UTF-8");
			OutputStream os = response.getOutputStream();
			byte[] buffer = modelServerService.getGltfFile(request,serverName);
			os.write(buffer);
		} catch (Exception e){
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(),"获取gltf文件失败");
		}
		return null;
	}
}
