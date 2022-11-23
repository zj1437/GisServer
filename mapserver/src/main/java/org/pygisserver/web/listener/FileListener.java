package org.pygisserver.web.listener;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.enumm.FileType;
import org.pygisserver.common.enumm.ServerStatusEnum;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.entity.PublishedDataFormat;
import org.pygisserver.web.service.impl.MapServerServiceImpl;
import org.pygisserver.web.utils.MapServerUtils;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.websocket.WebSocketManager;
import org.springframework.context.ApplicationContext;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 文件夹变化监听 
 * @author:  
 * @date: 2020-01-03
 */
@Slf4j
public class FileListener extends FileAlterationListenerAdaptor {
	
	private Map<String,String> protocalMap = new HashMap<String,String>();
	
	private Map<String,Integer> openTiledMap = new HashMap<String,Integer>();

    private MapServerServiceImpl mapServerService /*= SpringUtil.getBean("mapServerServiceImpl")*/;
	
	private WebSocketManager socketManager /*= SpringUtil.getBean("webSocketManager")*/;
	
	private static Map<String, DataServerEntity> failMapData = new HashMap<String,DataServerEntity>();
	
	private static final String DEFAULT_SERVER_NAME = "未命名";

	private static Map<String,File> directoryMap = new HashMap<>();

	public FileListener(List<PublishedDataFormat> dataFormat, ApplicationContext applicationContext) {
		mapServerService = (MapServerServiceImpl) applicationContext.getBean("mapServerServiceImpl");
		socketManager = (WebSocketManager) applicationContext.getBean("webSocketManager");
		if(protocalMap.size() > 0) protocalMap.clear();
		if(openTiledMap.size() > 0) openTiledMap.clear();
		for (PublishedDataFormat publishedDataFormat : dataFormat) {
		    protocalMap.put(publishedDataFormat.getDataFormat(), publishedDataFormat.getServerFormat());
		    openTiledMap.put(publishedDataFormat.getDataFormat(), publishedDataFormat.getOpenTiled());
		}
	}
	
	 /**
     * 文件创建执行
     */
	 @Override
    public void onFileCreate(File file) {
		String name = file.getName();
		String suffix = name.contains(".") ? name.substring(name.lastIndexOf(".")) : name;
		//截取文件后缀后如果仍然包含"."则将其过滤掉
		String newName = name.replace(suffix, "");
		if (!newName.contains(".")) {//&& !file.getName().split("\\.")[1].equals("itdb")
			autoPublishServer(file);
		}
    }

	public void autoPublishServer(File file) {
		String[] fileName = file.getName().split("\\.");
    	String prefix = fileName[0].replaceAll(Constant.SPECIAL_CHARACTER, "");
    	//单独处理.xml文件，设置为.tms，目的是从类型枚举中获取文件类型
        if (fileName[1].equals("xml")){
            fileName[1]="tms";
        }
    	if(StringUtils.isBlank(prefix)) {
    		prefix = DEFAULT_SERVER_NAME;
    	}
		//服务名称长度不能超过20，过长需要截取
    	if (prefix.length() > 20) {
    		prefix = prefix.substring(0,20);
		}
    	if (mapServerService.isPublished(prefix)) {
    		//服务重命名
    		prefix = mapServerService.getNewServerName(prefix);
    	}
    	DataServerEntity serverEntity = new DataServerEntity(prefix, prefix, file.getAbsolutePath());
    	serverEntity.setEncoding("GBK");
    	serverEntity.setFileType(FileType.getFileType("." + fileName[1]));
    	serverEntity.setOpenTiled(openTiledMap.get(fileName[1]));
    	serverEntity.setPublisedProctol(getServerPublisedProctol(fileName[1]));
    	serverEntity.setPublishTime(new Date());
    	serverEntity.setPublishor(UserUtil.getUserName());
    	serverEntity.setIsPublic(1);
        try {
            mapServerService.queryBboxFromFile(serverEntity.getDataUrl(), MapServerUtils.getServerFileType(serverEntity.getDataUrl()),serverEntity,ExceptionsUtil.getServerIp(null));
            mapServerService.singlePublishServerData(serverEntity,ExceptionsUtil.getServerIp(null));
        } catch (Exception e) {
            serverEntity.setStatus(ServerStatusEnum.ABNORMAL.getCode());
        }
        if (serverEntity.getStatus() == ServerStatusEnum.ABNORMAL.getCode()) {
            failMapData.put(prefix, serverEntity);
        }else {
            try {
                //推送给前端，刷新页面
                Map<String,Object> msgMap = new HashMap<String,Object>();
                msgMap.put("msg", MessageFormat.format("刚刚后台自动发布了服务名为{0}的服务", prefix));
                msgMap.put("msgKey", "mapServer");
                JSONObject jsonObject = new JSONObject(msgMap);
                socketManager.sendMessage(jsonObject.toJSONString());
            }
            catch (Exception e) {
                log.error("自动发布服务{}失败,失败原因{}",prefix,e.getMessage());
            }
        }

    }

    /**
     * 创建版本信息
     * @param 
     * @return
     */
    private String getServerPublisedProctol(String fileType) {
    	//获取其对应的协议，然后再处理
    	String serverProtocol = protocalMap.get(fileType);
    	if(StringUtils.isNoneBlank(serverProtocol) && serverProtocol.endsWith(";")) {
    		serverProtocol = serverProtocol.substring(0, serverProtocol.length() -1);
    	}
    	return serverProtocol;
    }
    
    /**
     * 文件创建修改
     */
	@Override
    public void onFileChange(File file) {
    	String[] fileName = file.getName().split("\\.");
    	String prefix = fileName[0];
    	if (mapServerService.isPublished(fileName[0])) {
    		//服务重命名
    		prefix = mapServerService.getNewServerName(fileName[0]);
    	}
    	//	发布失败的集合数据实体重新发布
		if (failMapData.containsKey(prefix)) {
			DataServerEntity mapData = failMapData.get(prefix);
			try {
                mapData.setStatus(ServerStatusEnum.PUBLISHED.getCode());
                mapServerService.queryBboxFromFile(mapData.getDataUrl(), MapServerUtils.getServerFileType(mapData.getDataUrl()), mapData, ExceptionsUtil.getServerIp(null));
                mapServerService.singlePublishServerData(mapData, ExceptionsUtil.getServerIp(null));
            }catch (Exception e){
			    mapData.setStatus(ServerStatusEnum.ABNORMAL.getCode());
            }
			if (mapData.getStatus() == ServerStatusEnum.ABNORMAL.getCode()){
			    failMapData.put(prefix,mapData);
            }else {
			    try {
                    //推送给前端，刷新页面
                    Map<String, Object> msgMap = new HashMap<String, Object>();
                    msgMap.put("msg", MessageFormat.format("刚刚后台自动发布了服务名为{0}的服务", prefix));
                    msgMap.put("msgKey", "mapServer");
                    JSONObject jsonObject = new JSONObject(msgMap);
                    socketManager.sendMessage(jsonObject.toJSONString());
                    failMapData.remove(prefix);
                }catch (Exception e) {
                    log.error("自动发布服务{}失败,失败原因{}",prefix,e.getMessage());
                }
            }
		}
    }

    /**
     * 文件删除，对应的服务应该删除
     * 
     */
	@Override
    public void onFileDelete(File file) {
    }

    /**
     * 目录创建,遍历其中的文件，进行发布
     */
	@Override
	public void onDirectoryCreate(File directory) {
        String directoryName = directory.getName();
        try {
            Arrays.asList(directory.listFiles()).forEach((file) -> {
                //单独处理tms数据的xml文件创建遍历
                if (!file.isDirectory() && file.getName().equals("tms.xml")) {
                    onFileCreate(file);
                }else {
                    directoryMap.put(directoryName,directory);
                }
                if (file.isDirectory()){
                    onDirectoryCreate(file);
                }
            });
        }catch (Exception e){
            log.error("onDirectoryCreate has an error !");
        }
	}

    /**
     * 目录修改
     */
	@Override
	public void onDirectoryChange(File directory) {
	    String directoryName = directory.getName();
        if (directoryMap.containsKey(directoryName)) {
            try {
                Arrays.asList(directory.listFiles()).forEach((file) -> {
                    //满足数据类型的文件遍历出来，或者tms数据遍历
                    if (!file.isDirectory() && protocalMap.containsKey(file.getName().split("\\.")[1]) || file.getName().equals("tms.xml")) {
                        onFileChange(file);
                    }
                });
            } catch (Exception e) {
                log.error("onDirectoryChange has an error !");
            }
        }
	}

    /**
     * 目录删除，目录下对应的服务需要删除
     */
	@Override
    public void onDirectoryDelete(File directory) {
    }

	@Override
    public void onStart(FileAlterationObserver observer) {
        super.onStart(observer);
    }

	@Override
    public void onStop(FileAlterationObserver observer) {
        super.onStop(observer);
    }
}
