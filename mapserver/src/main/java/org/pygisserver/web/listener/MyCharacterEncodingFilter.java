package org.pygisserver.web.listener;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.CharacterEncodingFilter;

@WebFilter(urlPatterns={"/*"})
public class MyCharacterEncodingFilter extends CharacterEncodingFilter {

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getServletPath();
		String encoding = getEncoding();
		if (encoding != null) {
			if (isForceRequestEncoding() || request.getCharacterEncoding() == null) {
				request.setCharacterEncoding(encoding);
			}
			if ((path.indexOf("/pytc/service") < 0 && path.indexOf("/wms") < 0 
					&& path.indexOf("/wcs") < 0
					) && isForceResponseEncoding()) {
				response.setCharacterEncoding(encoding);
			}
		}
		filterChain.doFilter(request, response);
		
	}
}
