package org.pygisserver.render.tiled;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: http工具类
 * @author: 刘洋
 * @date: 2020-09-08
 */
@Slf4j
public class HttpUtils {
	public static HttpEntity httpGet(String url, HashMap<String, Object> maps) {
		HttpGet httpGet = null;
		CloseableHttpClient httpClient = null;
		try {
			// 创建HttpClient上下文
			HttpClientContext context = HttpClientContext.create();

			// 创建一个CloseableHttpClient
			httpClient = getCloseableHttpClient();
			httpGet = new HttpGet(url);
			// 设置header
			httpGet.setHeader("Connection","keep-alive");
			httpGet.setHeader("Accept-Language","zh-CN,zh;q=0.8");
			httpGet.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			httpGet.setHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.153 Safari/537.36");
			CloseableHttpResponse response = null;
			response = httpClient.execute(httpGet, context);
			HttpEntity entity = response.getEntity();
			return entity;
		} catch (Exception e) {
			log.error("HttpUtils httpGet has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.REMOTE_HTTP_FAIL);
		}
	}

	/**
	 * 创建CloseableHttpClient
	 * 
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	private static CloseableHttpClient getCloseableHttpClient() {
		// 全局请求设置
		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD)
				.setConnectionRequestTimeout(5000).setConnectTimeout(5000).setSocketTimeout(5000).build();
		PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
		manager.setMaxTotal(500);
		// http 请求默认设置
		HttpClientBuilder custom = HttpClients.custom();
		custom.setDefaultRequestConfig(globalConfig);
		custom.setConnectionManager(manager);
		custom.setConnectionManagerShared(true);
		return custom.build();
	}
}
