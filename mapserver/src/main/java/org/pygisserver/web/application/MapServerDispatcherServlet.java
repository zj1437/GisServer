package org.pygisserver.web.application;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.web.service.ModelServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class MapServerDispatcherServlet extends AbstractController {
	
	private static final String qxsyPrefix = "/model/webqxsy/";
	
	@Autowired
	private ModelServerService modelServerService;

	private static Map<String,byte[]> cacheModelBuffer = new HashMap<String,byte[]>();
	
	private static Map<String,String> cacheFileUrl = new HashMap<String,String>();
	
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String path = request.getServletPath();
		//将请求path中的/model/前缀去掉
		String path1 = path.replaceFirst(qxsyPrefix, "");
		String[] params = path1.split("/");
		if (path.startsWith(qxsyPrefix)) {
			String serverName = params[0];
			if (StringUtils.isNotBlank(serverName)) {
				//首先查看当前文件服务是否已经发布
				int status = modelServerService.queryCurrentServerStatus(serverName);
				//0表示暂停
				OutputStream os = null;
				if (status == 0) {
					response.setCharacterEncoding("UTF-8");
					response.setStatus(ReponseStatus.ERROR.getCode());
					os = response.getOutputStream();
					os.write("当前服务被暂停，请联系管理员或服务发布人".getBytes());
				}
				String fileUrl;
				if (cacheFileUrl.containsKey(serverName)) {
					fileUrl = cacheFileUrl.get(serverName);
				}else {
					fileUrl = modelServerService.queryFileUrl(serverName, null);
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
					buffer = modelServerService.getModelFileStream(request,fileUrl,null,false);
//					cacheModelBuffer.put(fileUrl, buffer);
//				}
				reponseFileBuffer(response, lastModified, buffer);
			}
		}
		return null;
	}

	private void reponseFileBuffer(HttpServletResponse response, long lastModifyTime, byte[] buffer) {
		response.setHeader("Cache-Control", "max-age=31536000,public");
		response.setCharacterEncoding("UTF-8");
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
			Iterator<Entry<String,String>> it = cacheFileUrl.entrySet().iterator();
			while(it.hasNext()) {
				Entry<String,String> entry = it.next();
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
		Iterator<Entry<String,byte[]>> it = cacheModelBuffer.entrySet().iterator();
		while(it.hasNext()) {
			String url = it.next().getKey();
			if (url.startsWith(fileUrl)) {
				it.remove();
			}
		}
	}
}
