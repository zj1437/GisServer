package org.pygisserver.common.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NativeLoader {

	public static List<File> errorArray = new ArrayList<File>();
	
//	public synchronized static void loader(String dirPath) {
//		Enumeration<URL> dir = Thread.currentThread().getContextClassLoader().getResources(dirPath);
//		String systemType = System.getProperty("os.name");
//		String next = (systemType.toLowerCase().indexOf("windows") != -1) ? ".dll" : ".so";
//		while(dir.nextElement()) {
//			URL url = dir.nextElement();
//			String protocol = url.getProtocol();
//			if ("jar".equals(protocol)) {
//				
//			}
//			
//		}
//	}
	

}
