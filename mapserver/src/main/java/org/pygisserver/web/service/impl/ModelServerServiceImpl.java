package org.pygisserver.web.service.impl;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.pygisserver.common.enumm.ServerStatusEnum;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.dao.ModelServerMapper;
import org.pygisserver.web.entity.ModelEntity;
import org.pygisserver.web.entity.ModelProduct;
import org.pygisserver.web.entity.OnlineModelEntity;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.service.ModelServerService;
import org.pygisserver.web.utils.ModelFileUtil;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.utils.ZipCompress3ds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description:  
 * @author:  
 * @date: 2018-11-03
 */
@Service
@Slf4j
public class ModelServerServiceImpl implements ModelServerService {

	@Resource
	private ModelServerMapper modelServerMapper;
	
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private SystemLogService systemLogService;
	
	@Resource
	private ModelFileUtil modelFileUtils;
	
	@Resource
	private MapStatsticService statsticService;
	
	private static final int THREAD_NUM = 10;
	
	private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_NUM);
	
	private Map<Long, SubmittedTask> currentPool = new TreeMap<Long, SubmittedTask>();
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private static List<String> serverNames = new ArrayList<String>();
	
	private List<String> layerNames = new CopyOnWriteArrayList<String>();
	
	private volatile boolean flag = true;

	private static final String qxsyPrefix = "/model/webqxsy/";

	private static Map<String,byte[]> cacheModelBuffer = new HashMap<String,byte[]>();

	private static Map<String,String> cacheFileUrl = new HashMap<String,String>();

	/**
	 * @param serverName
	 * @return
	 */
	@Override
	public boolean isPublished(String serverName) {
		if (serverNames.isEmpty()) {
			serverNames = modelServerMapper.queryModelServerName();
		}
		//查询完毕
		if (serverNames.isEmpty()) {
			return false;
		}
		return serverNames.contains(serverName);
	}
	
	/**
	 * 发布模型数据服务
	 * @param serverName  服务名称
	 * @param urls 文件urls
	 */
	@Override
	@Transactional
	public Map<String,Object> publishModel(ModelEntity entity,String reqIP) {
		Map<String,Object> results = new HashMap<String, Object>();
		int result = 0;
		boolean isDeleteRecord = false;
		List<ModelProduct> bugModels = null;
		entity.setPublishTime(new Date());
		entity.setUpdateTime(new Date());
		int serverType = entity.getServerType();
		if (serverType == 0) {
			entity.setProductNum(entity.getProducts().size());
		}
		//倾斜射影的产品名称为其选择文件夹的名称
		if (serverType == 1) {
			File file = new File(entity.getUrl());
			entity.setProductName(file.getName());
		}
 		entity.setStatus(1);
		if (serverType == 0) {
			modelServerMapper.saveModelEntity(entity);
			bugModels = modelProductDeal(entity,reqIP);
			//当发布的模型的数量与异常模型的数量相同时清除数据库中的记录
			if(bugModels.size() > 0 && bugModels.size() == entity.getProducts().size()) {
				List<Integer> ids = new ArrayList<Integer>();
				ids.add(bugModels.get(0).getModelServerId());
				modelServerMapper.deleteModelServer(ids);
				isDeleteRecord = true;
				result = 1;
			}
		} else if (serverType == 1) {
			if (!entity.getProductName().endsWith(".json")) {
				modelFileUtils.createSourceDocument(entity,false,reqIP);
			}else {
				entity.setUrl(new File(entity.getUrl()).getParent());
			}
//			//创建线程任务 
//			if (StringUtils.isNotBlank(entity.getDestPath())) {
//				layerNames.add(entity.getServerName());
//				//修改当前服务的状态为处理中
//				modelServerMapper.updateServerStatusByName(entity.getServerName(),ServerStatusEnum.PROCESSING.getCode());
////				if (!flag) {
//				executorService.submit(new UpdateServerStatus());
////				}
//				List<ZipTask> zipTasks = createZipTask(new File(entity.getUrl()));
//				lock.writeLock().lock();
//				try {
//					for (ZipTask zipTask : zipTasks) {
//						final Long taskId = currentId.incrementAndGet();
//						zipTask.setServerName(entity.getServerName());
//						Future<ZipTask> future = executorService.submit(new ZipThread(zipTask,entity.getDestPath()));
//						currentPool.put(taskId, new SubmittedTask(zipTask, future));
//					}
//				} catch (Exception e) {
//					log.error("ModelServerServiceImpl publishModel aync has error:" + e.getLocalizedMessage());
//				} finally {
//					lock.writeLock().unlock();
//				}
//				//将生成的访问文件copy到目标文件夹下
//				copyXmlToDestPath(entity.getUrl(),entity.getDestPath());
//			}
			modelServerMapper.saveModelEntity(entity);
		}
		if(!isDeleteRecord) {
			serverNames.add(entity.getServerName());
		}
		if(bugModels == null) bugModels = new ArrayList<ModelProduct>();
		results.put("result", result);
		results.put("bugModels", bugModels);
		return results;
	}

	/**
	 * @Description: 更新服务状态
	 * @author:  
	 * @date: 2019-06-06
	 */
	class UpdateServerStatus implements Runnable{

		@Override
		public void run() {
			while(flag) {
				for (Iterator<String> it = layerNames.iterator();it.hasNext();) {
					String serverName = it.next();
					if (100.0 == getCurrentDealResult(serverName)) {
						//将状态更新为运行中
						modelServerMapper.updateServerStatusByName(serverName,ServerStatusEnum.PUBLISHED.getCode());
						//将该条数据剔除
						it.remove();
					}
				}
				if (layerNames.isEmpty()) {
					flag = false;
				}
				try {
					//每隔100ms查询一次
					Thread.sleep(100);
				} catch (InterruptedException e) {
					log.error("ModelServerServiceImpl UpdateServerStatus has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.BASE_ERROR);
				}
			}
		}
	}
	
	
	/**
	 * 将原路径下的文件copy到对应的目标路径下
	 * @param resourcePath
	 * @param destPath
	 */
	private void copyXmlToDestPath(String resourcePath,String destPath) {
		File file = new File(resourcePath);
		Arrays.asList(file.listFiles()).stream().filter(f -> !f.isDirectory() && (f.getName().endsWith(".pyt") || f.getName().endsWith(".lfp") || f.getName().endsWith(".scp")))
		.forEach((t) -> {
			FileUtil.nioCopyByChannel(t.getAbsolutePath(), destPath + File.separator + t.getName());
		});
	}
	
	/**
	 * 获取某一个服务当前的压缩进展情况
	 * @param serverName
	 * @return
	 */
	@Override
	public double getCurrentDealResult(String serverName) {
		lock.readLock().lock();
		int totalTask = 0;
		int doneTask = 0;
		try {
			Iterator<SubmittedTask> it = currentPool.values().iterator();
			for(;it.hasNext();) {
				SubmittedTask task = it.next();
				ZipTask zipTask = task.zipTask;
				if (serverName != null && !serverName.equals(zipTask.getServerName())) {
					continue;
				}
				totalTask++;
				doneTask+=zipTask.state.getCode();
			}
			return CommonUtil.format2Decimal(((double)doneTask / (totalTask * ZipTask.STATE.DONE.getCode()))*100,2);
		} finally {
			lock.readLock().unlock();
		}
	}

	
	/**
	 * 
	 * @param fileUrl 文件url
	 * @return
	 */
	private List<ZipTask> createZipTask(File fileDirectory) {
		List<ZipTask> zipTasks = null;
		if (fileDirectory.exists() && fileDirectory.isDirectory()) {
			List<String> fileUrls = new ArrayList<String>();
			int count = (int) Arrays.asList(fileDirectory.listFiles()).stream().filter((file) -> file.isDirectory()).count();
			int index = 0;
			zipTasks = new ArrayList<ZipTask>();
			for (File file : fileDirectory.listFiles()) {
				if (file.isDirectory()) {
					index++;
					//集合中已经添加了20条处理路径，或者当前已经到了最后一个文件夹
					fileUrls.add(file.getAbsolutePath());
					if (fileUrls.size() == 20 || count == index) {
						ZipTask zipTask = new ZipTask(fileUrls);
						zipTasks.add(zipTask);
						fileUrls = new ArrayList<String>();
					}
				}
			}
		}
		return zipTasks;
	}
	
	/**
	 * @Description: 压缩任务类 
	 * @author: 
	 * @date: 2019-06-03
	 */
	static class ZipTask {
		private String serverName;
		
		private List<String> fileUrls;
		
		private STATE state = STATE.READY;
		
		static enum STATE {
			READY(0),RUNNING(1),COPY(2),RENAME(3),COMPRESSING(4),DONE(5);
			
			private int code;
			
			STATE(int code) {
				this.code = code;
			}

			/**
			 * @return the code
			 */
			public int getCode() {
				return code;
			}

			/**
			 * @param code the code to set
			 */
			public void setCode(int code) {
				this.code = code;
			}
			
	    };
		
		public ZipTask(List<String> fileUrls) {
			this.fileUrls = fileUrls;
		}
		
		public STATE getState() {
			return state;
		}

		/**
		 * @return the serverName
		 */
		public String getServerName() {
			return serverName;
		}

		/**
		 * @param serverName the serverName to set
		 */
		public void setServerName(String serverName) {
			this.serverName = serverName;
		}
	}
	
	/**
	 * @Description: 压缩线程 
	 * @author: 
	 * @date: 2019-06-03
	 */
	class ZipThread implements Callable<ZipTask>{

		private ZipTask zipTask;
		
		/**
		 * 目标路径
		 */
		private String destPath;
		
		public ZipThread(ZipTask zipTask,String destPath) {
			this.zipTask = zipTask;
			this.destPath = destPath;
		}
		
		@Override
		public ZipTask call() {
			zipTask.state = ZipTask.STATE.RUNNING;
			List<String> fileUrls = zipTask.fileUrls;
			List<String> destPaths = new ArrayList<String>(fileUrls.size());
			for (String fileUrl : fileUrls) {
				zipTask.state = ZipTask.STATE.COPY;
				File file = new File(fileUrl);
				String tempDestPath = destPath + File.separator + file.getName();
				destPaths.add(tempDestPath);
				FileUtil.copyFileItemsToDestDir(fileUrl,tempDestPath);
			}
			//修改对应文件的名称
			for (String path : destPaths) {
				zipTask.state = ZipTask.STATE.RENAME;
				renameFile(new File(path));
			}
			//压缩每一个文件夹
			for (String path : destPaths) {
				zipTask.state = ZipTask.STATE.COMPRESSING;
				File file = new File(path);
				String oldPathFile = file.getParent() + File.separator + file.getName().split("\\.")[0] + ".zip";
				String newPathFile = file.getParent() + File.separator + file.getName().split("\\.")[0] + ".tdb";
				FileUtil.zipCompress(new File(path));
				//删除原文件
				FileUtil.deleteFile(path);
				//压缩完后修改对应的名称
				FileUtil.renameFile(newPathFile, oldPathFile);
			}
			zipTask.state = ZipTask.STATE.DONE;
			return zipTask;
		}
	}
	
	/**
	 * 修改文件名称
	 * @param file
	 */
	private static void renameFile(File fileDirectory) {
		if (fileDirectory.exists() && fileDirectory.isDirectory()) {
			for (File file : fileDirectory.listFiles()) {
				//TODO 将osgb文件中模型的jpeg转成jpeg2000  郑州联调
				
				String fileName = file.getName();
				fileName = fileName.split("\\.")[0] + ".tob";
				FileUtil.renameFile(file.getParent() + File.separator + fileName, file.getAbsolutePath());
			}
		}
	}
	
	static class SubmittedTask {
		private ZipTask zipTask;
		private Future<ZipTask> future;
		public SubmittedTask(ZipTask zipTask,Future<ZipTask> future) {
			this.zipTask = zipTask;
			this.future = future;
		}
	}
	
	/**
	 * 倾斜射影处理
	 * @param fileUrl
	 */
	private Set<String> TiltProductDeal(ModelEntity entity) {
		String fileUrl = entity.getUrl();
		File file = new File(fileUrl);
		//遍历其下的文件
		File[] files = file.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		Set<String> urls = new HashSet<String>();
		Set<String> suffixUrls = new HashSet<String>();
		//再次遍历
		String baseUrl = entity.getRequesturl() + "model" + "/" + entity.getServerName() + "/" + file.getName();
	    for (File file2 : files) {
	    	getNodeUrl(urls, suffixUrls, baseUrl, file2);
		}
	    //生成对应的xml文件
//	    createTiltPreviewXml(suffixUrls,file);
	    return urls;
	}
	
	//开始压缩文件
	private void compressTilt(Set<String> urls) {
		for (String url : urls) {
	    	ZipCompress3ds zip3ds = new ZipCompress3ds(url);
	    	executorService.submit(zip3ds);
	    }
	}

	private void getNodeUrl(Set<String> urls, Set<String> suffixUrls, String baseUrl, File directFile) {
		if (directFile.isDirectory()) {
			for (File file : directFile.listFiles()) {
				if (!file.isDirectory() && file.getName().endsWith(".osgb")) {
					urls.add(file.getAbsolutePath());
					String fileName = file.getParentFile().getName() + "/" + file.getName().split("\\.")[0] + "-3ds.zip";
					suffixUrls.add(baseUrl + "?subName=" + fileName.replace("+", "%2B"));
				}
			}
		}
	}
	
	private void createTiltPreviewXml(Set<String> urls,File file) {
		Document document = DocumentHelper.createDocument();
		Element node = document.addElement("NodeList");
		for (String url : urls) {
			node.addElement("Node").setText(url);
		}
		XMLWriter writer;
		try {
			writer = new XMLWriter(new FileOutputStream(new File(file.getAbsolutePath() + File.separator + file.getName() + ".pre")),
					OutputFormat.createPrettyPrint());
			writer.setEscapeText(false);
			writer.write(document);
			writer.close();
		} catch (FileNotFoundException e) {
			log.error("ModelServerServiceImpl createTiltPreviewXml has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, file.getName()));
		} catch (IOException e) {
			log.error("ModelServerServiceImpl createTiltPreviewXml has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, file.getName() + "配置文件"));
		}
	}
	
	private List<ModelProduct> modelProductDeal(ModelEntity entity,String reqIP) {
		List<ModelProduct> products = new ArrayList<ModelProduct>();
		List<ModelProduct> bugModels = new ArrayList<ModelProduct>();
		List<ModelProduct> productList = null;
		String result = null;
		//保存当前模型服务,保存完毕后根据数据的url获取文件类型
		productList = entity.getProducts();
		for (ModelProduct product : productList) {
			product.setModelServerId(entity.getId());
			File file = new File(product.getFileUrl());
			//如果当前是文件夹，则获取前台传递过来的文件类型
			if (file.isDirectory()) {
				//1是文件夹
				product.setIsDirectory(1);
			} else {
				//0是文件
				product.setIsDirectory(0);
				//获取当前文件类型,获取类型时如果解压失败会返回一个带;的字符串
				result = getModelProductInfo(file,reqIP);
				product.setFileType(result);
			}
			//如果文件类型的字符串不包含;则认为zip文件解压成功，否则将其放入异常容器并返回，其他的模型按照正常逻辑处理
			if(!result.contains(";")) {
				products.add(product);
			} else {
				bugModels.add(product);
			}
		}
		//保存服务列表
		if(products.size() > 0) {
			try {
				modelServerMapper.saveModelProducts(products);
			} catch (Exception e) {
				log.error("ModelServerServiceImpl modelProductDeal has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_VALUE_TOO_LONG, "模型"));
			}
		}
		//启动线程压缩文件
//		for (ModelProduct product : products) {
//			String mdPath = product.getFileUrl();
//			String mdSuffx = mdPath.contains(".") ? mdPath.substring(mdPath.lastIndexOf(".")) : mdPath;
//			if (!product.getFileUrl().endsWith(".zip") && !(mdSuffx.equalsIgnoreCase(".glb") || mdSuffx.equalsIgnoreCase(".gltf"))) {
//				compressFileToZip(product,reqIP);
//			}
//		}
		new Thread(new ModelProductCompress(products,reqIP)).start();
		return bugModels;
	}
	
	class ModelProductRunnable implements Runnable {
		private ModelProduct product;

		private String reqIP;

		public ModelProductRunnable(ModelProduct product, String reqIP) {
			this.product = product;
			this.reqIP = reqIP;
		}

		@Override
		public void run() {
			File file = new File(product.getFileUrl());
			try {
				FileUtil.zipCompress(file);
			}catch (ResourceException e) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE,
						product.getProductName() + "模型压缩文件"), "System","文件管理", reqIP));
				throw e;
			}
		}
		
	}
	
	class ModelProductCompress implements Runnable {
		private List<ModelProduct> products;
		private String reqIP;
		
		public ModelProductCompress(List<ModelProduct> products,String reqIP) {
			this.products = products;
			this.reqIP = reqIP;
		}

		@Override
		public void run() {
			for (ModelProduct product : products) {
				String mdPath = product.getFileUrl();
				String mdSuffx = mdPath.contains(".") ? mdPath.substring(mdPath.lastIndexOf(".")) : mdPath;
				if (!product.getFileUrl().endsWith(".zip") && !(mdSuffx.equalsIgnoreCase(".glb") || mdSuffx.equalsIgnoreCase(".gltf"))) {
					executorService.submit(new ModelProductRunnable(product, reqIP));
				}
			}
		}
		
	}

	private void compressFileToZip(ModelProduct product,String reqIP) {
		File file = new File(product.getFileUrl());
		try {
			FileUtil.zipCompress(file);
		}catch (ResourceException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE,
					product.getProductName() + "模型压缩文件"), "System","文件管理", reqIP));
			throw e;
		}
		
//		ZipCompressThread thread = new ZipCompressThread(file);
//		executorService.submit(thread);
//		//目前的已知需要转换的有ive,osgb
//		if ("ive".equalsIgnoreCase(product.getFileType()) || 
//			"osgb".equalsIgnoreCase(product.getFileType())) {
//			ZipCompress3ds zip3ds = new ZipCompress3ds(product.getFileUrl());
//			executorService.submit(zip3ds);
//		}
	}
	
	/**
	 * 获取模型数据文件类型
	 * 
	 * @param file 文件实体
	 */
	private String getModelProductInfo(File file,String reqIP) {
		//首先查看数据类型
		String fileName = file.getName();
		int lastIndex = fileName.lastIndexOf(".");
		if (lastIndex > 0) {
			String prefix = fileName.substring(lastIndex + 1);
			if ("osg".equalsIgnoreCase(prefix)) {
				return "osg";
			}
			if ("osgb".equalsIgnoreCase(prefix)) {
				return "osgb";
			}
			if ("ive".equalsIgnoreCase(prefix)) {
				return "ive";
			}
			if ("3ds".equalsIgnoreCase(prefix)) {
				return "3ds";
			}
			if ("obj".equalsIgnoreCase(prefix)) {
				return "obj";
			}
			if ("flt".equalsIgnoreCase(prefix)) {
				return "flt";
			}
			if ("fbx".equalsIgnoreCase(prefix)) {
				return "fbx";
			}
			if ("dae".equalsIgnoreCase(prefix)) {
				return "dae";
			}
			if ("glb".equalsIgnoreCase(prefix)) {
				return "glb";
			}
			if ("gltf".equalsIgnoreCase(prefix)) {
				return "gltf";
			}
			if ("zip".equalsIgnoreCase(prefix)) {
				//解压缩查看当前压缩中的文件类型
				return getFileType(file,reqIP);
			}
		}
		return "";
	}

	private String getFileType(File file,String reqIP) {
		String zipFileName = file.getName();
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file,Charset.forName("gbk"));
			String name = zipFile.stream().filter(entry -> !entry.isDirectory()).findFirst().get().getName();
			return name.split("\\.")[1].toLowerCase();
		} catch (IOException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, "模型发布失败:" + ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DECOMPRESS, zipFileName + "模型压缩文件"),
					"System","模型发布", reqIP));
			log.error("ModelServerServiceImpl getFileType has error:" + ExceptionsUtil.stackTraceToStr(e));
			return zipFileName + ";解压失败，请检查数据是否正常";
		}finally {
			try {
				if(zipFile != null) zipFile.close();
			} catch (IOException e) {
				log.error("ModelServerServiceImpl getFileType has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
		}
	}
	
	
	/**
	 * 编辑当前服务列表
	 * @param entity 模型服务 
	 */
	@Override
	@Transactional
	public Map<String,String> editModelServer(ModelEntity entity,String reqIP) {
		Map<String,String> results = new HashMap<String, String>();
		int totalModels = 0;
		String result = null;
		ModelEntity oldModelEntity = modelServerMapper.queryModelEntityById(entity.getId());
		if (!oldModelEntity.equals(entity)) {
			//处理模型数据
			if (entity.getServerType() == 0) {
				List<ModelProduct> newProducts = entity.getProducts();
				//缓存新的list
				List<ModelProduct> tempProducts = new ArrayList<ModelProduct>();
				tempProducts.addAll(newProducts);
				List<ModelProduct> oldProducts = modelServerMapper.queryProducts(entity.getId());
				entity.setProductNum(newProducts.size());
				//获取两个对象的交集，然后分别去获取两个list的并集部分
				tempProducts.retainAll(oldProducts);
				//获取新增的文件
				newProducts.removeAll(tempProducts);
				//获取删除的文件
				oldProducts.removeAll(tempProducts);
				//更新当前服务信息
				entity.setUpdateTime(new Date());
				//删除模型产品
				if (!oldProducts.isEmpty()) {
					modelServerMapper.deleteModelProduct(oldProducts);
				}
				//保存更新文件
				if (!newProducts.isEmpty()) {
					totalModels = newProducts.size();
					String fileType = null;
					tempProducts.clear();
					for (ModelProduct modelProduct : newProducts) {
						fileType = getModelProductInfo(new File(modelProduct.getFileUrl()),reqIP);
						if(!fileType.contains(";")) {
							modelProduct.setModelServerId(entity.getId());
							modelProduct.setFileType(fileType);
							compressFileToZip(modelProduct,reqIP);
							tempProducts.add(modelProduct);
						} else {
							result = result + fileType.split(";")[0] + ";";
						}
					}
					if(result != null) {
						result = result.substring(0, result.length() - 1).substring(4);
					}
					if(tempProducts.size() > 0) {
						try {
							modelServerMapper.saveModelProducts(tempProducts);
						} catch (Exception e) {
							log.error("ModelServerServiceImpl editModelServer has error:" + ExceptionsUtil.stackTraceToStr(e));
							throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_VALUE_TOO_LONG, "模型"));
						}
					} else {
						entity.setProductNum(0);
					}
				}
			} else {
				//重新生成原有的服务文件
				if (!entity.getProductName().endsWith(".json")) {
					modelFileUtils.createSourceDocument(entity,true,reqIP);
				}
//			TiltProductDeal(entity);
			}
			//设置当前服务下的产品数量
			modelServerMapper.editModelServer(entity);
			serverNames.clear();
		}
		results.put("result", result);
		results.put("totalModels", totalModels + "");
		return results;
	}

	/**
	 * 模型服务的删除
	 */
	@Override
	@Transactional
	public void deleteModelServer(List<Integer> ids) {
		//删除统计信息
		statsticService.deleteServerReqInfoById(ids);
		//如果是倾斜射影数据，需要删除其生成的xml文件
		List<ModelEntity> entitys = modelServerMapper.queryModelEntityByIds(ids);
		if (entitys.isEmpty()) {
			return;
		}
		for (ModelEntity modelEntity : entitys) {
			//1 是倾斜射影
			if (modelEntity.getServerType() == 1) {
				deleteLayerCache(modelEntity.getServerName());
			}
			if (modelEntity.getServerType() == 1 && !modelEntity.getUrl().endsWith(".json")) {
				modelFileUtils.deleteFileByPublishor(modelEntity);
			}
		}
		//删除模型数据
		modelServerMapper.deleteModelServer(ids);
		//清空
		serverNames.clear();
	}

	@Override
	public void updateModelServerStatus(int id) {
		try {
			modelServerMapper.updateServerStatus(id);
		} catch (Exception e) {
			log.error("ModelServerServiceImpl updateModelServerStatus has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "地图服务是否可访问"));
		}
	}

	@Override
	public List<ModelEntity> queryModelServerList(RequestQueryCondition queryCondition) {
		try {
			return modelServerMapper.queryModellist(queryCondition);
		} catch (Exception e) {
			log.error("ModelServerServiceImpl queryModelServerList has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "模型服务"));
		}
	}

	@Override
	public int queryModelServerCount(RequestQueryCondition condition) {
		try {
			return modelServerMapper.queryModelCount(condition);
		} catch (Exception e) {
			log.error("ModelServerServiceImpl queryModelServerCount has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "模型服务数量"));
		}
	}
	
	/**
	 * 获取倾斜射影数据量
	 */
	@Override
	public int queryTiltServerCount(RequestQueryCondition condition) {
		try {
			return modelServerMapper.queryTiltCount(condition);
		} catch (Exception e) {
			log.error("ModelServerServiceImpl queryModelServerCount has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "倾斜摄影服务数量"));
		}
	}
	

	@Override
	public String queryFileUrl(String serverName, String productName) {
		try {
			Map<String, String> paramMap = new HashMap<String,String>();
			paramMap.put("serverName", serverName);
			if (StringUtils.isNotBlank(productName)) {
//				if(productName.contains(".zip") || productName.endsWith(".glb") || productName.endsWith(".gltf")
//						|| productName.endsWith(".png") || productName.endsWith(".jpeg") || productName.endsWith(".bmp")) {
				if (productName.indexOf(".") > -1) 
				productName = productName.substring(0,productName.indexOf("."));
//				}
				paramMap.put("productName", productName);
			}
			return modelServerMapper.queryServerUrl(paramMap);
		} catch (Exception e) {
			log.error("ModelServerServiceImpl queryFileUrl has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "文件路径"));
		}
	}

	@Override
	public String queryFilePosition(String fileurl,String user) {
		Map<String,Double> mapParam = modelFileUtils.analysisXmlFile(fileurl,user);
		//将获取的参数构造成以经纬高为一体的字符串
		StringBuilder sb = new StringBuilder();
		sb.append(mapParam.get("lon")).append(":").append(mapParam.get("lat")).append(":").append(mapParam.get("hei"));
		return sb.toString();
	}

	/**
	 * 获取模型文件流
	 * @param url 文件url
	 * @throws FileNotFoundException 
	 */
	@Override
	public byte[] getModelFileStream(HttpServletRequest request,final String url,String modelName,boolean isModel) {
		File[] fileArray = {new File(url)};
		if (!fileArray[0].exists()) {
			//不存在，则随机找一个通后缀名的文件
			RequestQueryCondition condition = new RequestQueryCondition();
			condition.setServerName(modelName);
			List<ModelEntity> entities = modelServerMapper.queryTiltlist(condition);
			final String user = entities.get(0).getPublishor();
			File parentFile = fileArray[0].getParentFile();
			parentFile.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if (name.contains(".pyt") || name.contains(".lfp") || name.contains(".scp")) {
						String reqType = url.substring(url.lastIndexOf("."));
						String configFile = user + "_" + modelName + reqType;
						if (configFile.equals(name)) {
							File file = new File(dir,name);
							fileArray[0] = file;
						}
					}
					return false;
				}
			});
		}
		String modelUrl = fileArray[0].getAbsolutePath();
		if(isModel) {
			String fileName = (new File(modelUrl)).getName();
			if (fileName.endsWith(".gltf")){
				//对gltf文件进行相应更改
				return getGltfFile(request,modelName);
			} else if (fileName.endsWith(".glb")) {
				return modelFileUtils.getFileInfo(getZipUrl(modelUrl));
			}
		}
		return modelFileUtils.getFileInfo(modelUrl);
	}
	
	
	
	private static String getZipUrl(String url) {
		File file = new File(url);
		//如果本身已经是zip
		if (file.getName().endsWith(".zip")) {
			return url;
		}
		//如果发布的本身就是zip，那直接读取
		File directFile = file.getParentFile();
		File[] zipFiles = directFile.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.split("\\.")[0].equals(file.getName().split("\\.")[0]) && name.endsWith(".zip");
			}
		});
		if (zipFiles != null && zipFiles.length > 0) {
			return zipFiles[0].getAbsolutePath();
		}
		return url;
	}

	/**
	 * 获取倾斜射影服务列表
	 */
	@Override
	public List<ModelEntity> queryTiltServerList(RequestQueryCondition condition) {
		try {
			List<ModelEntity> modelEntities = modelServerMapper.queryTiltlist(condition);
			modelEntities.parallelStream().forEach(modelEntity -> {
				File file = new File(modelEntity.getUrl());
				if (!file.exists()){
					modelEntity.setStatus(2);
				}
			});
			return modelEntities;
		} catch (Exception e) {
			log.error("ModelServerServiceImpl queryTiltServerList has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "倾斜摄影服务"));
		}
	}

	@Override
	public int queryCurrentServerStatus(String serverName) {
		try {
			return modelServerMapper.queryServerStatus(serverName);
		} catch (Exception e) {
			log.error("ModelServerServiceImpl queryCurrentServerStatus has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, serverName + "模型服务状态"));
		}
	}

	@Override
	public ModelProduct getProductInfo(int id) {
		try {
			return modelServerMapper.queryProductInfo(id);
		} catch (Exception e) {
			log.error("ModelServerServiceImpl getProductInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "模型服务"));
		}
	}

	
	
	@Override
	public List<String> getTiltServerUrl(int serverId,String reqIP) {
		String fileurl = modelServerMapper.queryServerFileUrl(serverId);
		//找到对应文件下的.pre文件，并且读取
		File file = new File(fileurl);
		File[] files = file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				File tempFile = new File(dir, name);
				return (!tempFile.isDirectory() && name.endsWith(".pre"));
			}
		});
		return readXmlFile(files[0],reqIP);
	}
	
	
	public List<String> readXmlFile(File file, String reqIP) {
		SAXReader read = new SAXReader();
		Document document;
		try {
			document = read.read(file);
			Element rootDom = document.getRootElement();
			Element nodeList = rootDom.element("NodeList");
			//获取所有node节点
			List<Element> nodes = rootDom.elements("Node");
			List<String> urls = new ArrayList<String>();
			for (Element element : nodes) {
				String url = element.getText();
				urls.add(url);
			}
			return urls;
		} catch (DocumentException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE,
					file.getName() + "配置文件"), "System","文件管理", reqIP));
			log.error("ModelServerServiceImpl readXmlFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, file.getName() + "配置文件"));
		}
	}

	/**
	 * 获取online支持的模型数据
	 */
	@Override
	public List<OnlineModelEntity> queryOnlineModels(RequestQueryCondition condition,String url) {
		List<OnlineModelEntity> onlineModels = new ArrayList<OnlineModelEntity>();
		String thumbal ="model/online/thumbal/{0}/{1}";
		String model ="model/{0}/{1}/{2}";
		String modelPath = null;
		List<ModelEntity> list = queryModelServerList(condition);
		for (ModelEntity modelEntity : list) {
			List<ModelProduct> products = modelEntity.getProducts();
			for (ModelProduct product: products) {
				String modelType = product.getFileType();
				if (modelType.equalsIgnoreCase("glb") || modelType.equalsIgnoreCase("gltf")) {
					File file = new File(product.getFileUrl());
					String modelName = file.getName().substring(0, file.getName().lastIndexOf("."));
					String pPath = file.getParentFile().getAbsolutePath();
					File pngFile = new File(pPath,modelName + ".png");
					File jpegFile = new File(pPath,modelName + ".jpeg");
					File bmpFile = new File(pPath,modelName + ".bmp");
					String thumbalFileName = pngFile.exists()
							? pngFile.getName() : jpegFile.exists()
							? jpegFile.getName() : bmpFile.exists()
							? bmpFile.getName() : null;
					String thumbalPath = StringUtils.isBlank(thumbalFileName)
							? null : url + MessageFormat.format(thumbal, modelEntity.getServerName(), thumbalFileName);
					modelPath = url + MessageFormat.format(model, modelEntity.getServerName(), file.getName().substring(0, file.getName().lastIndexOf(".")), file.getName());
					OnlineModelEntity entity = new OnlineModelEntity(product.getProductName(), modelPath, thumbalPath);
					onlineModels.add(entity);
				}
			}
		}
		return onlineModels;
	}
	
	/**
	 * 获取online支持的倾斜摄影数据
	 */
	@Override
	public List<OnlineModelEntity> getOnlineQXSY(RequestQueryCondition condition,String url) {
		List<OnlineModelEntity> onlineModels = new ArrayList<OnlineModelEntity>();
		String thumbal ="model/online/thumbal/{0}/{1}";
		String fgt ="model/webqxsy/{0}/{1}";
		String thumbalPath = null;
		List<ModelEntity> entities = queryTiltServerList(condition);
		for (ModelEntity modelEntity : entities) {
			String productName = modelEntity.getProductName();
			if (productName.endsWith(".json")) {
				String modelName = productName.substring(0, productName.lastIndexOf("."));
				String pPath = modelEntity.getUrl();
				File file = new File(pPath);
				if (file.exists()){
					File pngFile = new File(pPath,modelName + ".png");
					File jpegFile = new File(pPath,modelName + ".jpeg");
					File bmpFile = new File(pPath,modelName + ".bmp");
					String thumbalFileName = pngFile.exists()
							? pngFile.getName() : jpegFile.exists()
							? jpegFile.getName() : bmpFile.exists()
							? bmpFile.getName() : null;
					thumbalPath = StringUtils.isBlank(thumbalFileName)
							? null : url + MessageFormat.format(thumbal, modelEntity.getServerName(), thumbalFileName);
					String fgtPath = url + MessageFormat.format(fgt, modelEntity.getServerName(), modelEntity.getProductName());
					OnlineModelEntity entity = new OnlineModelEntity(modelEntity.getServerName(), fgtPath, thumbalPath);
					entity.setId(modelEntity.getId());
					entity.setOffsetHeight(modelEntity.getOffsetHeight());
					onlineModels.add(entity);
				}
			}
		}
		return onlineModels;
	}

	@Override
	public int updateOffsetHeight(int id, double offsetHeight) {
		return modelServerMapper.updateOffsetHeight(id, offsetHeight);
	}

	@Override
	public void onlineQxsyData (HttpServletRequest request, HttpServletResponse response, String serverName, String productName) throws IOException {
		String path = request.getServletPath();
		//将请求path中的/model/前缀去掉
		String path1 = path.replaceFirst(qxsyPrefix, "");
		if (path.startsWith(qxsyPrefix)) {
			if (StringUtils.isNotBlank(serverName)) {
				String fileUrl;
				if (cacheFileUrl.containsKey(serverName)) {
					fileUrl = cacheFileUrl.get(serverName);
				}else {
					fileUrl = queryFileUrl(serverName, null);
					cacheFileUrl.put(serverName, fileUrl);
				}
				fileUrl = fileUrl + path1.replace(serverName, "");
				File file = new File(fileUrl);
				long lastModified = file.lastModified();
				final byte[] buffer;
				/**
				 * 暂时先不缓存，主要是内存消耗过大
				 */
//				if (cacheModelBuffer.containsKey(fileUrl)) {
//					buffer = cacheModelBuffer.get(fileUrl);
//				}else {
				buffer = getModelFileStream(request,fileUrl,null,false);
//					cacheModelBuffer.put(fileUrl, buffer);
//				}
				reponseFileBuffer(response, buffer);
			}
		}
	}

	private void reponseFileBuffer(HttpServletResponse response, byte[] buffer) {
		response.setHeader("Cache-Control", "max-age=31536000,public");
//		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json;chartser=UTF-8");

		OutputStream os = null;
		try {
			os = response.getOutputStream();
			os.write(buffer);
			os.flush();
		} catch (IOException e) {
			try {
				os.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void deleteLayerCache(String...serverNames) {
		for (String serverName : serverNames) {
			Iterator<Map.Entry<String,String>> it = cacheFileUrl.entrySet().iterator();
			while(it.hasNext()) {
				Map.Entry<String,String> entry = it.next();
				String name = entry.getKey();
				if (serverName.equals(name)) {
					String fileUrl = entry.getValue();
					deleteBufferCache(fileUrl);
					it.remove();
				}
			}
		}
	}

	private void deleteBufferCache(String fileUrl) {
		Iterator<Map.Entry<String,byte[]>> it = cacheModelBuffer.entrySet().iterator();
		while(it.hasNext()) {
			String url = it.next().getKey();
			if (url.startsWith(fileUrl)) {
				it.remove();
			}
		}
	}

	@Override
	public byte[] getGltfFile(HttpServletRequest request, String serverName){
		try {
			String fileName = null;
			File file = null;
			String requestURI = request.getRequestURI().substring(1);
			String[] params = requestURI.split("/");
			fileName = params[params.length - 1];
			RequestQueryCondition condition = new RequestQueryCondition();
			condition.setServerName(serverName);
			List<ModelEntity> modelEntities = modelServerMapper.queryModellist(condition);
			ModelEntity modelEntity = modelEntities != null ? modelEntities.get(0) : null;
			if (modelEntity == null || !new File(modelEntity.getUrl()).exists()) return new byte[1];
			if ("model".equalsIgnoreCase(params[1]) && "queryGltfFile".equalsIgnoreCase(params[2])) {
				//请求除gltf外的其余资源
				File resource = new File(modelEntity.getUrl()).getParentFile();
				if ("0".equals(params[4])){
					resource = resource.getParentFile();
				}
				file = new File(resource.getAbsolutePath() + File.separator + requestURI.substring(requestURI.indexOf(params[4]) + 2));
			} else {
				for (ModelProduct pro : modelEntity.getProducts()) {
					if (pro.getProductName().equals(params[3])) file = new File(pro.getFileUrl());
				}
			}
			//如果是图像、bin则直接返回文件内容
			if (fileName.endsWith("jpg") || fileName.endsWith("jpeg") || fileName.endsWith("png") || fileName.endsWith(".bin")) return FileUtil.getFileResource(file);
			File temp = new File(System.getProperty("java.io.tmpdir") + File.separator + "model");
			if (!temp.exists()) temp.mkdir();
			File result = new File(temp.getAbsoluteFile() + File.separator + serverName + "-" + file.getName());
			if (result.exists()) return FileUtil.getFileResource(result);
			byte[] fileResource = FileUtil.getFileResource(file);
			String jsonString = new String(fileResource);
			JSONObject jsonObject = JSONObject.parseObject(jsonString);
			File parentFile = file.getParentFile();

			// .bin处理
			Object buffers = jsonObject.get("buffers");
			if (buffers instanceof JSONObject){
				JSONObject tempObject = (JSONObject) buffers;
				if (tempObject.containsKey("byteLength") && tempObject.containsKey("name")) return modelFileUtils.getFileInfo(file.getAbsolutePath());
				String uri = tempObject.getString("uri");
				uri = new File(FileUtil.decodeFilePath(uri)).getPath();
				File realFile = CommonUtil.getRealFile(uri, parentFile);
				if (realFile != null){
					(tempObject).replace("uri",dealTempUrl(uri,serverName,request));
				}
			} else if (buffers instanceof JSONArray){
				JSONArray array = (JSONArray) buffers;
				for (int i = 0; i < array.size() ; i++) {
					JSONObject tempObject = array.getJSONObject(i);
					if (tempObject.containsKey("byteLength") && tempObject.containsKey("name")) return modelFileUtils.getFileInfo(file.getAbsolutePath());
					String uri = tempObject.getString("uri");
					uri = new File(FileUtil.decodeFilePath(uri)).getPath();
					File realFile = CommonUtil.getRealFile(uri, parentFile);
					if (realFile != null){
						(tempObject).replace("uri",dealTempUrl(uri,serverName,request));
					}
				}
			}

			// .jpg处理
			Object images = jsonObject.get("images");
			if (images instanceof JSONObject){
				JSONObject tempObject = (JSONObject) images;
				String uri = tempObject.getString("uri");
				uri = new File(FileUtil.decodeFilePath(uri)).getPath();
				File realFile = CommonUtil.getRealFile(uri, parentFile);
				if (realFile != null){
					(tempObject).replace("uri",dealTempUrl(uri,serverName,request));
				}
			} else if (images instanceof JSONArray){
				JSONArray array = (JSONArray) images;
				for (int i = 0; i < array.size() ; i++) {
					JSONObject tempObject = array.getJSONObject(i);
					String uri = tempObject.getString("uri");
					uri = new File(FileUtil.decodeFilePath(uri)).getPath();
					File realFile = CommonUtil.getRealFile(uri, parentFile);
					if (realFile != null){
						(tempObject).replace("uri",dealTempUrl(uri,serverName,request));
					}
				}
			}

			//写出文件
			FileOutputStream outputStream = new FileOutputStream(result.getAbsoluteFile(),true);
			FileChannel channel = outputStream.getChannel();
			byte[] bytes = jsonObject.toJSONString().getBytes();
			ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
			byteBuffer.put(bytes);
			byteBuffer.flip();
			channel.write(byteBuffer);
			channel.close();
			if (outputStream != null){
				outputStream.close();
			}
			return bytes;
		} catch (Exception e) {
			log.error("ModelServerService getGltfFile has error : " + e.getLocalizedMessage());
		}
		return null;
	}

	private String dealTempUrl(String uri,String serverName,HttpServletRequest request){
		int flag = 1;
		if (uri.contains(".." + File.separator)){
			flag = 0;
			uri = uri.replace(".." + File.separator,"");
		}
		StringBuffer sb = new StringBuffer(request.getScheme());
		StringBuffer baseUrl = sb.append("://").append(UserUtil.getLocalIpArress()).append(":").append(request.getServerPort())
				.append(request.getContextPath()).append("/model/queryGltfFile/").append(serverName).append("/").append(flag).append("/").append(uri);
		return baseUrl.toString();
	}
}
