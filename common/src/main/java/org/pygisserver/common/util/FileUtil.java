package org.pygisserver.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.charset.StandardCharsets;


/**
 * @Description: 文件操作工具类 
 * @author:
 * @date: 2019-12-18
 */
@Slf4j
public class FileUtil {

	public static Map<String,FileInputStream> streamCache = new ConcurrentHashMap<String,FileInputStream>();



	/**
	 * 创建文件
	 * @param filePath
	 * @param fileContent
	 * @throws IOException
	 */
	public static void createFile(String filePath,String fileContent) throws IOException{
		File checkFile = new File(filePath);
		FileWriter writer = null;
		try {
			if(!checkFile.exists()){
				checkFile.createNewFile();
			}
			writer = new FileWriter(checkFile,true);
			writer.append(fileContent);
			writer.flush();

		}catch (IOException e){
			e.printStackTrace();
		}finally {
			if(null!=writer){
				writer.close();
			}
		}

	}

	/**
	 * 修改文件内容
	 * @param filePath
	 * @param fileContent
	 * @throws IOException
	 */
	public static void updateFile(String filePath,String fileContent) throws IOException{
		try {
			OutputStream outFile = new FileOutputStream(filePath);
			outFile.write(fileContent.getBytes());
			outFile.flush();
			outFile.close();
		}catch (IOException e){

		}
	}

	public static String readTxt(String filePath) {
		StringBuilder lineTxt = new StringBuilder();
		try {
			File file = new File(filePath);
			if(file.isFile()&&file.exists()){
				InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
				BufferedReader br = new BufferedReader(isr);
				String lineTxt1;
				while ((lineTxt1=br.readLine())!=null){
					lineTxt.append(lineTxt1).append("\n");
				}
				br.close();
			}else {
				log.error("文件不存在");
			}
		}catch (Exception e){
			log.error("文件读取错误");
		}
		return  lineTxt.toString();

	}

	/**
	 * 关闭数据流
	 * @param io 数据流
	 * @throws IOException 
	 */
	public static void closeStream(Closeable... io) {
		try {
			for (Closeable closeable : io) {
				if (closeable != null) {
					closeable.close();
				}
			}
		} catch (IOException e) {
			log.error("FileUtil closeStream has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CLOSE, "数据流"));
		}
		
	}
	
	/**
	 * 压缩文件
	 * 
	 * @param reourceFile  原文件路径
	 * @throws Exception
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void zipCompress(File reourceFile) {
		ZipOutputStream out = null;
		try {
			// 根据原文件构建压缩后的文件
			String fileName = reourceFile.getName();
			String zipName = null;
			if (reourceFile.isDirectory()) {
				zipName = fileName + ".zip";
			} else {
				zipName = fileName.split("\\.")[0] + ".zip";
			}

			// 构建目标文件
			String destPath = reourceFile.getParent() + File.separator + zipName;
			File destFile = new File(destPath);
			// 不存在创建
			if (!destFile.exists()) {
				destFile.createNewFile();
			}
			out = new ZipOutputStream(new FileOutputStream(destFile));
			createZipFile(reourceFile, fileName, out);
		} catch (IOException e) {
			log.error("FileUtil zipCompress has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "Zip压缩文件"));
		} finally {
			closeStream(out);
		}
		
	}

	private static void createZipFile(File reourceFile, String entryBase, ZipOutputStream out) {
		BufferedInputStream bis = null;
		ZipEntry ze = null;
		try {
			if (reourceFile.isDirectory()) {
				// 如果文件夹有文件就遍历
				File[] fileList = reourceFile.listFiles();
				if (fileList.length == 0) {
					ze = new ZipArchiveEntry(reourceFile.getName() + File.separator);
					out.putNextEntry(ze);
				}
				for (File f : fileList) {
					createZipFile(f, entryBase + File.separator + f.getName(), out);
				}
			} else {
				// 如果是文件，直接往里面写
				ze = new ZipEntry(entryBase);
				out.putNextEntry(ze);
				bis = new BufferedInputStream(new FileInputStream(reourceFile));
				// 读写文件
				byte[] buffer = new byte[1024];
				int len = 0;
				while ((len = bis.read(buffer)) != -1) {
					out.write(buffer,0,len);
				}
				out.flush();
			}
		} catch (IOException e) {
			log.error("FileUtil createZipFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "Zip压缩文件"));
		} finally {
			closeStream(bis);
		}
	}
	
	
	/**
	 * 内存映射文件copy
	 * @param resPath
	 * @param destPath
	 * @throws IOException
	 */
	public static void nioCopyByMapped(String resPath, String destPath) {
		FileChannel inChannel = null;
		FileChannel outChannel = null;
		try {
			//判断目标文件夹是否已经存在
			File file = new File(destPath);
			if (!file.getParentFile().exists())
			{
				file.getParentFile().mkdirs();
			}
			inChannel = FileChannel.open(Paths.get(resPath), StandardOpenOption.READ);
			outChannel = FileChannel.open(Paths.get(destPath), StandardOpenOption.WRITE,
													  StandardOpenOption.READ,
													  StandardOpenOption.CREATE_NEW);
			MappedByteBuffer inMappedBuf = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
			MappedByteBuffer outMappedBuf = outChannel.map(FileChannel.MapMode.READ_WRITE, 0, inChannel.size());
			//直接从缓存冲读写数据
			byte[] dst = new byte[inMappedBuf.limit()];
			inMappedBuf.get(dst);
			outMappedBuf.put(dst);
		} catch (IOException e) {
			log.error("FileUtil nioCopyByMapped has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_COPY, "瓦片数据文件"));
		} finally {
			closeStream(inChannel,outChannel);
		}
	}
	
	/**
	 * 文件拷贝(效率高)
	 * @param resPath 原文件路径
	 * @param destPath 目标文件路径
	 * @throws IOException 
	 */
	public static void transferResToDest(String resPath, String destPath) {
		File file = new File(destPath);
		if (!file.getParentFile().exists())
		{
			file.getParentFile().mkdirs();
		}
		FileOutputStream fos = null;
		FileInputStream fis = null;
		try {
			fos = new FileOutputStream(file);
			fis = new FileInputStream(resPath);
			// 创建输出流通道
			FileChannel target = fos.getChannel();
			FileChannel in = fis.getChannel();
			long size = in.size();
			in.transferTo(0, size, target);
		} catch (FileNotFoundException e) {
			log.error("FileUtil transferResToDest has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, "文件"));
		} catch (IOException e) {
			log.error("FileUtil transferResToDest has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_UPLOAD, ""));
		} finally {
			closeStream(fos, fis);
		}
	}
	
	/**
	 * 文件夹拷贝
	 * @param resourcePath
	 * @param destPath
	 * @throws IOException 
	 */
	public static void copyFileItemsToDestDir(String resourcePath,String destPath) {
		if (StringUtils.isBlank(resourcePath) || StringUtils.isBlank(destPath)) {
			throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_NULL, "resourcePath或destPath"));
		}
		File resourceFile = new File(resourcePath);
		if (!resourceFile.exists()) {
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, resourcePath));
		}
		File destFile = new File(destPath);
		if (!destFile.exists()) {
			destFile.mkdirs();
		}
		for (File file : resourceFile.listFiles()) {
			if (!file.isDirectory()) {
				transferResToDest(resourcePath + File.separator + file.getName(),destPath + File.separator + file.getName());
			}else {
				//如果是文件夹，继续执行
				copyFileItemsToDestDir(resourcePath + File.separator + file.getName(),destPath + File.separator + file.getName());
			}
		}
	}
	
	/**
	 * nio 通过内存通道复制(这种方式copy效率差)
	 * @param resPath
	 * @param destPath
	 * @throws IOException 
	 */
	public static void nioCopyByChannel(String resPath, String destPath) {
		FileChannel inChannel = null;
		FileChannel outChannel = null;	
		try {
			//判断目标文件夹是否已经存在
			File file = new File(destPath);
			if (!file.getParentFile().exists())
			{
				file.getParentFile().mkdirs();
			}
			inChannel = FileChannel.open(Paths.get(resPath), StandardOpenOption.READ);
			outChannel = FileChannel.open(Paths.get(destPath), StandardOpenOption.WRITE,
													  StandardOpenOption.READ,
													  StandardOpenOption.CREATE_NEW);
			MappedByteBuffer readBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
			outChannel.write(readBuffer);
		} catch (IOException e) {
			log.error("FileUtil nioCopyByChannel has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_COPY, "数据流"));
		} finally {
			try {
				inChannel.close();
			} catch (IOException e) {
				log.error("FileUtil nioCopyByChannel has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			try {
				outChannel.close();
			} catch (IOException e) {
				log.error("FileUtil nioCopyByChannel has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
		}
		
	}

	/**
	 * 将文件读取成字节数组
	 * @param filePath
	 * @return 
	 * @throws IOException 
	 */
	public static byte[] getFileInfo(String filePath) {
		File file = new File(filePath);
		byte[] fileData = null;
		InputStream in = null;
		if (file.exists()) {
			try {
				in = new FileInputStream(file);
				fileData = new byte[(int) file.length()];
				in.read(fileData);
			} catch (IOException e) {
				log.error("FileUtil getFileInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "文件"));
			} finally {
				closeStream(in);
			}
		}
		return fileData;
	}
	
	
	/**
	 * 将文件读取成字节数组
	 * @param filePath
	 * @return 
	 * @throws IOException 
	 */
	public static byte[] getFileResource(File file)  {
		
		FileInputStream fis = null;
		byte[] buffer = new byte[(int) file.length()];
		try {
			fis = new FileInputStream(file);
			MappedByteBuffer byteBuffer = fis.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
			byteBuffer.get(buffer);
		} catch (FileNotFoundException e) {
			log.error("FileUtil getFileResource has error:" + e.getMessage(), e.getCause());
			return null;
		} catch (IOException e) {
			log.error("FileUtil getFileResource has error:" + e.getMessage(), e.getCause());
			return null;
		}finally {
			closeStream(fis);
		}
		
		return buffer;
		
		
//		return getFileInfo(file.getAbsolutePath());
		
		/*FileInputStream in = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
		String filePath = file.getAbsolutePath();
		try {
			if (streamCache.containsKey(filePath)) {
				in = streamCache.get(filePath);
				invokeInputStream(in,filePath);
			}else {
					in = new FileInputStream(file);
					streamCache.put(filePath, in);
			}
			byte[] buf = new byte[4096];
			int len = -1;
			while((len = in.read(buf)) != -1) {
				bos.write(buf,0,len);
			}
		} catch (IOException e) {
			log.error("FileUtil getFileInfo has error:" + e.getMessage(), e.getCause());
			return null;
		}
		finally {
			closeStream(bos,in);
		}
		return bos.toByteArray();*/
	}
	
	public static void invokeInputStream(InputStream is,String filePath) {
		Class in = is.getClass();
		Method open0;
		try {
			open0 = in.getDeclaredMethod("open0", String.class);
			open0.setAccessible(true);
			open0.invoke(is, filePath);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	

	public static synchronized Map<String,FileInputStream> getCloseStream(){
		return streamCache;
	}
	
	public static synchronized void closeStream(Map<String,FileInputStream> closeables) throws IOException {
		closeStream(closeables.values().toArray(new Closeable[] {}));
	}
	
	public static synchronized void clearStream(Map<String,FileInputStream> closeables) {
		for (Iterator iterator = streamCache.keySet().iterator(); iterator.hasNext();) {
			String filePath = (String) iterator.next();
			if (closeables.containsKey(filePath)) {
				iterator.remove();
			}
		}
	}


	
	
	/**
	 * 递归获取文件的大小
	 * @param filePath 文件路径
	 * @return 返回文件或文件夹的大小
	 */
	public static long getFileLength(String filePath) {
		File file = new File(filePath);
		long size = 0l;
		if (file.exists()) {
			for (File file1 : file.listFiles()) {
				if (!file1.isDirectory()) {
					size += file1.length();
				}else {
					size += getFileLength(file1.getAbsolutePath());
				}
			}
		}
		return size;
	}
	
	/**
	 * 将文件形式同统一格式化为File.separator
	 * @param fileUrl
	 * @return
	 */
	public static String formatFileUrl(String fileUrl) {
		if (fileUrl.indexOf("/") > -1) {
			return fileUrl.replaceAll("/", Matcher.quoteReplacement(File.separator));
		}else if (fileUrl.indexOf("//") > -1){
			return fileUrl.replaceAll("//", Matcher.quoteReplacement(File.separator));
		}
		return fileUrl;
	}
	
	
	/**
	 * @param path 格式化文件url
	 * @return
	 */
	public static String formalFilePath(String filePath) {
		//如果是以ip开头，且开头没有"//",
		if (filePath.matches("^\\d{3}.+$")) {
			filePath = "//" + filePath;
		}
		return filePath;
	}
	
	/**
	 * @param path 格式化文件url
	 * @return
	 */
	public static String decodeFilePath(String filePath) {
		String path = null;
		URLDecoder decoder = new URLDecoder();
		try {
			path = decoder.decode(filePath, "utf-8");
		} catch (UnsupportedEncodingException e) {
			log.error("FileUtil decodeFilePath has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DECODE, filePath));
		}
		return path;
	}
	
	/**
	 * 文件重命名
	 * @param newName
	 * @param oldName
	 */
	public static void renameFile(String newPathFile,String oldPathFile) {
		File oldFile = new File(oldPathFile);
		File newFile = new File(newPathFile);
		oldFile.renameTo(newFile);
	}
	
	/**
	 * 文件移动
	 * @param newName
	 * @param oldName
	 */
	public static void moveFile(String newPathFile,String oldPathFile) {
		try {
			FileUtils.moveDirectoryToDirectory(new File(oldPathFile), new File(newPathFile),true);
		} catch (IOException e) {
			log.error("FileUtil moveFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_MOVE, oldPathFile));
		}
	}
	
	/**
	 * 删除对应文件夹下的所有文件
	 * @param filePath
	 */
	public static void deleteFile(String filePath) {
		if (StringUtils.isBlank(filePath)) {
			log.warn("FileUtil deleteFile has error,参数 {} 为空", filePath);
		}
		
		File file = new File(filePath);
		if (!file.exists()) {
			log.warn("FileUtil deleteFile has error,参数{}对应的资源文件不存在", filePath);
		}
		if (file.isDirectory() && file.list().length > 0) {
			for (File tempFile : file.listFiles()) {
				if (tempFile.isDirectory()) {
					deleteFile(tempFile.getAbsolutePath());
				}else {
					tempFile.delete();
				}
			}
			file.delete();
		}else {
			file.delete();
		}
	}
	
	/**
	 * 统计某个目录下某种类型文件的个数
	 * @param path
	 * @param fileType
	 * @return
	 */
	public static int getFileNumber(String path,String fileType) {
		File file = new File(path);
		int fileSize = 0;
		if(file.isFile()) {
			file = file.getParentFile();
		}
		fileSize = file.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					if(name.endsWith(fileType)) {
						return true;
					} else {
						return false;
					}
				}
		}).length;
		
		return fileSize;
	}

	public static void writeResponse(HttpServletResponse rep, HttpServletRequest req, byte[] resourceInfo, String lastModify,
							   String mineType, int httpCode) {
		if (StringUtils.isNotBlank(lastModify)) {
			final String ifModSinceHeader = req.getHeader("If-Modified-Since");
			// 设置请求头
			rep.setHeader("Last-Modified", lastModify);
			if (StringUtils.isNotBlank(ifModSinceHeader)) {
				// 判断时间是否相等
				long ifModSinceSeconds = DateUtil.convertTimestampToDate(ifModSinceHeader).getTime();
				long lastModifySeconds = DateUtil.convertTimestampToDate(lastModify).getTime();
				if (ifModSinceSeconds >= lastModifySeconds) {
					httpCode = HttpServletResponse.SC_NOT_MODIFIED;
					resourceInfo = null;
				}
			}
		}
		// 处理web端访问缓存问题
		if (resourceInfo == null && httpCode == HttpServletResponse.SC_OK) {
			throw new BaseException(ErrorCode.NOT_FOUND.getErrorCode(), "请求的资源不存在");
		}
//		rep.setCharacterEncoding("UTF-8");
		rep.setContentType(mineType);
		rep.setStatus(httpCode);
		if (resourceInfo != null) {
			OutputStream outPutStream = null;
			try {
				outPutStream = rep.getOutputStream();
				outPutStream.write(resourceInfo);
				outPutStream.flush();
			} catch (IOException e) {
				log.error("Respense Write dealReponseResult has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
		}

	}
}
