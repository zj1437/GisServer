package org.pygisserver.web.interceptor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.pygisserver.web.listener.FileUploadProgressListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 *
 * @Description: 
 * @author:  
 * @date: 2019-05-14
 */
public class CustomerMultipartResolver extends CommonsMultipartResolver {
	
	@Autowired
	private FileUploadProgressListener progressListener;
	
	@Override
	protected MultipartParsingResult parseRequest(HttpServletRequest req) throws MultipartException {
		String encoding = determineEncoding(req);
		FileUpload fileUpload = prepareFileUpload(encoding);
		List<FileItem> fileItems;
		try {
//			progressListener.setSession(req.getSession());
//			fileUpload.setProgressListener(progressListener);
			fileItems = ((ServletFileUpload)fileUpload).parseRequest(req);
			return parseFileItems(fileItems, encoding);
		} catch (FileUploadException e) {
			throw new MultipartException("Could not parse multipart servlet request",e);
		}
	}
}
