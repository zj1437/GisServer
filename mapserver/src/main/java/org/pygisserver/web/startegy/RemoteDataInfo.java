package org.pygisserver.web.startegy;

import java.io.IOException;
import java.net.URL;

import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.http.HTTPClient;
import org.geotools.http.SimpleHttpClient;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.CRSEnvelope;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.ows.wms.WebMapServer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 远程服务信息获取
 * @author: 
 * @date: 2020-06-05
 */
@Slf4j
public class RemoteDataInfo extends AbstractReadData {

	private WebMapServer webMapServer;
	
	public RemoteDataInfo(String remoteUrl,String fileType) {
		constructWebMapServer(remoteUrl);
	}
	
	private void constructWebMapServer(String remoteUrl) {
		HTTPClient client = new SimpleHttpClient(); 
		URL url;
		try {
			url = new URL(remoteUrl);
			webMapServer = new WebMapServer(url, client);
		} catch (ServiceException | IOException e) {
			log.error("RemoteDataInfo constructWebMapServer has error:" +  ExceptionsUtil.stackTraceToStr(e));
			throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.BASE_SETUP, "WebMapServer服务实体"));
		}
	}

	@Override
	public ReferencedEnvelope getBboxFromFile() {
		WMSCapabilities capabilities = webMapServer.getCapabilities();
		CRSEnvelope crsEnvelope = capabilities.getLayer().getLatLonBoundingBox();
		return new ReferencedEnvelope(crsEnvelope.getMinimum(0),
									  crsEnvelope.getMaximum(0),
									  crsEnvelope.getMinimum(1),
									  crsEnvelope.getMaximum(1),
									  crsEnvelope.getCoordinateReferenceSystem());
	}

	@Override
	public CoordinateReferenceSystem getResourceSrs() {
		WMSCapabilities capabilities = webMapServer.getCapabilities();
		return capabilities.getLayer().getLatLonBoundingBox().getCoordinateReferenceSystem();
	}

	@Override
	public int getMaxLevel(ReferencedEnvelope originalEnvelope) {
		return 0;
	}

}
