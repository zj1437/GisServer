package org.pygisserver.web.listener;

import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.ProgressListener;
import org.pygisserver.web.entity.Progress;
import org.springframework.stereotype.Component;

@Component
public class FileUploadProgressListener implements ProgressListener {

	
	private HttpSession session;
	
	public void setSession(HttpSession session) {
		this.session = session;
		Progress progress = new Progress();
		session.setAttribute("progress", progress);
	}
	
	@Override
	public void update(long pBytesRead, long pContentLength, int pItems) {
//		Progress progress = new Progress(pBytesRead, pContentLength, pItems);
		Progress progress = (Progress) session.getAttribute("progress");
		progress.setByteRead(pBytesRead);
		progress.setContentLength(pContentLength);
		progress.setItem(pItems);
	}
}
