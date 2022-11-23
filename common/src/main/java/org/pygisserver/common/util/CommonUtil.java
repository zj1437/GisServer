package org.pygisserver.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.enumm.DataTypeEnum;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * @Description: 公共工具类 
 * @author: 
 * @date: 2019-11-28
 */
@Slf4j
public class CommonUtil {

	private static final String[] WINDOWS_COMMOND = {"ipconfig","/all"};
	
	private static final String[] LINUX_COMMOND = {"/sbin/ifconfig","-a"};
	
	private static final Pattern matchPattern = Pattern.compile(".*((:?[0-9a-f]{2}[-:]){5}[0-9a-f]{2}).*", Pattern.CASE_INSENSITIVE);

	private static final String LINUX_DATA_FILE = "/bin/pygisserver";

	private static final String WINDOWS_INSTALL_PATH = "/WENA/install";
	public static byte[] int2byte(int value) {
		byte[] byteArray = new byte[4];
		byteArray[0] = (byte) (value & 0xFF);
		byteArray[1] = (byte) (value >> 8  & 0xFF);
		byteArray[2] = (byte) (value >> 16 & 0xFF);
		byteArray[3] = (byte) (value >> 24 & 0xFF);
		return byteArray;
	}
	
	
	/**
     * 字节数组转成int
     * 
     * @param bytes 字节数组
     * @return
     */
    public static int bytes2int(byte[] bytes)
    {
        
        int a = (bytes[3] & 0xff) << 24;
        int b = (bytes[2] & 0xff) << 16;
        int c = (bytes[1] & 0xff) << 8;
        int d = (bytes[0] & 0xff);
        return a | b | c | d;
    }
    
    /**
     * 字节数据转为float
     * @param bytes
     * @return
     */
    public static float bytes2float(byte[] bytes) {
    	return Float.intBitsToFloat(bytes2int(bytes));
    }
	
	
	/**
	 * 将doule保留为有效数字
	 * @param number
	 * @param digital 保留位数
	 * @return
	 */
	public static double format2Decimal(double number, int digital) {
		BigDecimal decimal = new BigDecimal(number);
		return decimal.setScale(digital, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	/**
	 * 将doule保留为有效数字
	 * @param number1
	 * @param digital 保留位数
	 * @return
	 */
	public static double format2Decimal(double number1, double number2, int digital) {
		BigDecimal decimal1 = new BigDecimal(number1);
		BigDecimal decimal2 = new BigDecimal(number2);
		return format2Decimal(decimal1.divide(decimal2,8,BigDecimal.ROUND_HALF_UP).doubleValue(),7);
	}
	
	 /**
     * 加载properties文件
     * @param filePath 文件路径
     * @return properties 返回properties对象
	 * @throws IOException 
     */
    public static Properties loadPropertyFile(String filePath) {
    	File file = new File(filePath);
    	try {
            Properties properties = null;
            if (file.exists())
            {
                InputStream is = new FileInputStream(file);
                properties = new Properties();
                properties.load(is);
            }
            return properties;
		} catch (Exception e) {
			log.error("CommonUtil loadPropertyFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, file.getName()));
		}
        
    }
    
	/**
	 * 根据文件类型判断服务是否为瓦片的或者在线服务 
	 * 增加海图数据
	 * @param type
	 * @return
	 */
	public static boolean isTiledServer(String type) {
		if (StringUtils.isBlank(type)) {
			return false;
		}
		if ("Tdb".equalsIgnoreCase(type) ||"FlatGis".equalsIgnoreCase(type) 
				|| "TMS".equalsIgnoreCase(type) || "google_raster".equalsIgnoreCase(type)
				|| "google_combine".equalsIgnoreCase(type) || "google_map".equalsIgnoreCase(type)
				|| "gaode_raster".equalsIgnoreCase(type) || "gaode_map".equalsIgnoreCase(type)
				|| "mapbox".equalsIgnoreCase(type) || "osm".equalsIgnoreCase(type) 
				|| "tianditu".equalsIgnoreCase(type) || "MBTiles".equalsIgnoreCase(type)
				) {
			return true;
		}
		return false;
	}
	
	
	
	/**
	 * 获取本机的ip
	 * @return
	 * @throws UnknownHostException 
	 */
	public static String getLocalHost() {
		try {
			InetAddress address = null;
			address = InetAddress.getLocalHost();
			return address.getHostAddress();
		} catch (Exception e) {
			log.error("CommonUtil getLocalHost has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "本地IP地址"));
		}
	}
	
	/**
	 * 从注册机查询当前系统的注册时间
	 * @return
	 * @throws ParseException 
	 */
	public static Date getSystemExpireTime() {
		String date = "";
		if (isWindows()){
			Preferences userData = Preferences.systemRoot().node(WINDOWS_INSTALL_PATH);
			date = userData.get("date", "OK");
			if ("OK".equalsIgnoreCase(date)) {
				return null;
			}
		} else {
			File file = new File(LINUX_DATA_FILE);
			if (file.exists()) {
				String str = new String();
				try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(LINUX_DATA_FILE))){
					byte[] bytes = new byte[2048];
					int n = -1;
					while ((n = inputStream.read(bytes, 0, bytes.length)) != -1) {
						str = new String(bytes, 0, n, "UTF-8");
					}
				} catch (IOException e) {
					log.error("CommonUtil read linux data file has error:" + ExceptionsUtil.stackTraceToStr(e));
				}
				Base64.Decoder decoder = Base64.getDecoder();
				byte[] decode = decoder.decode(str);
				try {
					String linuxFile = new String(decode, "UTF-8");
					date = linuxFile.substring(linuxFile.indexOf(":"));
				} catch (UnsupportedEncodingException e) {
					log.error("CommonUtil read linux data file has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DECODE, LINUX_DATA_FILE));
				}
			} else {
				return null;
			}
		}
		return DateUtil.covertStringToDate(CrypotUtil.decrypt(date));

	}

	/**
	 * 从注册机查询当前系统使用得最后时间
	 * @return
	 * @throws ParseException
	 */
	public static Date getSystemLastTime() {
		Preferences userData = Preferences.userRoot().node("/PYGISServer/install");
		String date = userData.get("dateLast", "OK");
		if ("OK".equalsIgnoreCase(date)) {
			return null;
		}
		return DateUtil.covertStringToDateHour(CrypotUtil.decrypt(date));
	}
	
	/**
	 * 激活更新注册表中的到期时间
	 * @return
	 * @throws ParseException 
	 */
	public static void updateSystemExpireTime(String date) {
		if (isWindows()) {
			Preferences userData = Preferences.systemRoot().node(WINDOWS_INSTALL_PATH);
			userData.put("date", CrypotUtil.encrypt(date));
		} else {
			String readFileToLinux = readFileToLinux();
			String macInfo = readFileToLinux.substring(0, readFileToLinux.indexOf(":") + 1) + CrypotUtil.encrypt(date);
			try(BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(LINUX_DATA_FILE))) {
				byte[] bytes = macInfo.getBytes("UTF-8");
				Base64.Encoder encoder = Base64.getEncoder();
				String str = encoder.encodeToString(bytes);
				byte[] bytes1 = str.getBytes();
				outputStream.write(bytes1);
			} catch (IOException e) {
				e.getMessage();
			}
		}
	}

	/**
	 * 激活更新注册表中的到期时间
	 * @return
	 * @throws ParseException
	 */
	public static void updateSystemLastTime(String date) {
		Preferences userData = Preferences.userRoot().node("/PYGISServer/install");
		userData.put("dateLast", CrypotUtil.encrypt(date));

	}
	private static String readFileToLinux() {
		File file = new File(LINUX_DATA_FILE);
		if (file.exists()) {
			String str = new String();
			try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(LINUX_DATA_FILE))){
				byte[] bytes = new byte[2048];
				int n = -1;
				while ((n = inputStream.read(bytes, 0, bytes.length)) != -1) {
					str = new String(bytes, 0, n, "UTF-8");
				}
			} catch (IOException e) {
				e.getMessage();
			}
			Base64.Decoder decoder = Base64.getDecoder();
			byte[] decode = decoder.decode(str);
			try {
				return new String(decode, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				log.error("CommonUtil read Linux data has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
		}
		return null;
	}
	/**
	 * 获取机器的网卡信息
	 * @return
	 * @throws IOException 
	 */
	public static String getMacAddress() {
		StringBuilder stringBuilder = new StringBuilder();
		try {
			String os = System.getProperty("os.name");
			// Windows获取Mac地址
			if (os.startsWith("Windows")) {
				InetAddress localHost = InetAddress.getLocalHost();
				byte[] hardwareAddress = NetworkInterface.getByInetAddress(localHost).getHardwareAddress();
				for (int count = 0; count < hardwareAddress.length; count++) {
					if (count != 0) {
						stringBuilder.append("-");
					}
					int tem = hardwareAddress[count]&0xff;
					String string = Integer.toHexString(tem);
					if (string.length() == 1) {
						stringBuilder.append("0").append(string);
					} else {
						stringBuilder.append(string);
					}
				}
			} else if (os.startsWith("LINUX") || os.startsWith("Linux") ) {	//Linux获取hostName
				String linuxFile = readFileToLinux();
				stringBuilder.append(linuxFile.substring(0, linuxFile.indexOf(":")));
			}
		} catch (UnknownHostException | SocketException e) {
			log.error("CommonUtil getSystemExpireTime has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "本机MAC码"));
		}
		return stringBuilder.toString().toUpperCase(Locale.US);
	}
	
	public static boolean installExpireTime(Date expireTime ,boolean isFirst) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 30);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		String installTime = year + "-" + month + "-" + day;
		// expireTime = DateUtil.covertStringToDate(installTime);
		if (isWindows()) {
			Preferences userData = Preferences.systemRoot().node(WINDOWS_INSTALL_PATH);
			userData.put("date", CrypotUtil.encrypt(installTime));
		} else {
			File file = new File(LINUX_DATA_FILE);
			if (!file.exists() && isFirst) {
				StringBuilder sb = new StringBuilder();
				sb.append(getStringRandom(2))
						.append("-")
						.append(getStringRandom(2))
						.append("-")
						.append(getStringRandom(2))
						.append("-")
						.append(getStringRandom(2))
						.append(":")
						.append(CrypotUtil.encrypt(installTime));
				byte[] bytes = new byte[0];
				try {
					bytes = sb.toString().getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.error("CommonUtil write Linux data has error:" + ExceptionsUtil.stackTraceToStr(e));
				}
				Base64.Encoder encoder = Base64.getEncoder();
				String str = encoder.encodeToString(bytes);
				try(BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(LINUX_DATA_FILE))) {
					byte[] bytes1 = str.getBytes();
					outputStream.write(bytes1);
				} catch (IOException e) {
					log.error("CommonUtil write linux data file has error:" + ExceptionsUtil.stackTraceToStr(e));
				}
			} else {
				return false;
			}
		}
		return true;
	}

	//生成随机数字和字母,
	private static String getStringRandom(int length) {

		String val = "";
		Random random = new Random();

		//参数length，表示生成几位随机数
		for(int i = 0; i < length; i++) {

			String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";
			//输出字母还是数字
			if( "char".equalsIgnoreCase(charOrNum) ) {
				//输出是大写字母还是小写字母
				int temp = random.nextInt(2) % 2 == 0 ? 65 : 97;
				val += (char)(random.nextInt(26) + temp);
			} else if( "num".equalsIgnoreCase(charOrNum) ) {
				val += String.valueOf(random.nextInt(10));
			}
		}
		return val;
	}
	
	/**
	 * 序列化当前对象
	 * @param object
	 * @return
	 */
	public static byte[] serialize(Object object) {
        ObjectOutputStream oos = null;
        ByteArrayOutputStream baso = null;
        try {
            baso = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baso);
            oos.writeObject(object);
            oos.flush();
            return baso.toByteArray();
        } catch (IOException e) {
        	log.error("CommonUtil serialize has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SERIALZABLE, "Java对象"));
        }
    }

    /**
     * 反序列化操作
     * @param buf 序列化数组
     * @return 对象
     */
    public static Object unserialize(byte[] buf) {
        ByteArrayInputStream bais = null;
        try {
        	if(buf == null) return null;
            bais = new ByteArrayInputStream(buf);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (IOException e) {
        	log.error("CommonUtil unserialize has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SERIALZABLE, "Java对象反"));
        } catch (ClassNotFoundException e) {
        	log.error("CommonUtil unserialize has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, "Java类"));
        }
    }
	
    /**
     * 获取字符串的MD5值
     * @param value 字符串
     * @return 返回MD5值
     */
    public static String getMd5(String value) {
    	return DigestUtils.md5DigestAsHex(value.getBytes());
    }
    
    
    /**
	 * 根据文件类型获取数据类型
	 * 
	 * @param fileType
	 * @return
	 */
	public static int getDataType(String fileType) {
		int dataType = 0;
		switch (fileType) {
		case "Shape":
		case "PostGIS":
		case "kml":
			dataType = DataTypeEnum.VECTOR.getFileCode();
			break;
		case "GeoTIFF":
		case "ERDASImg":
			dataType = DataTypeEnum.RASTER.getFileCode();
			break;
		case "TDB":
		case "TMS":	
		case "FlatGis":
		case "MBTiles":
			dataType = DataTypeEnum.TILED.getFileCode(); 
			break;
		case "google_raster":
		case "google_combine":	
		case "google_map":
		case "gaode_raster":
		case "gaode_map":
		case "mapbox":
		case "OpenStreetMap":
		case "tianditu":
			dataType = DataTypeEnum.ONLINE.getFileCode(); 
			break;
		case "wms":
			dataType = DataTypeEnum.WMS.getFileCode();
		default:
			break;
		}
		return dataType;
	}
	
	
	public static String getOSName() {
		String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            return "Windows";
        }
        else if (osName.contains("Mac") || osName.contains("Darwin")) {
            return "Mac";
        }
        else if (osName.contains("Linux")) {
            return "Linux";
        }
        else {
            return osName.replaceAll("\\W", "");
        }
    }

    private static boolean isWindows() {
		String os = System.getProperty("os.name");
		if (os.startsWith("Windows")) {
			return true;
		} else if (os.startsWith("LINUX") || os.startsWith("Linux")) {
			return false;
		}
		return true;
	}

	public static String simpleMineType(String mineType) {
		if (mineType.indexOf(";") > -1) {
			if (mineType.contains("type=mapbox-vector")) {
				mineType = "protobuf";
			} else if (mineType.contains("type=geojson")) {
				mineType = "geojson";
			} else if (mineType.contains("type=topojson")) {
				mineType = "topojson";
			}
		}
		if (mineType.indexOf("/") > -1) {
			mineType = mineType.split("/")[1];
		}
		return mineType;
	}
	public static File getRealFile(String uri,File parentFile){
		String parentPath = parentFile.getAbsolutePath();
		File binFile = new File(uri);
		binFile = !binFile.exists() ? new File(FileUtil.decodeFilePath(parentPath + File.separator + uri)) : binFile;
		String absolutePath = binFile.getAbsolutePath();
		//如果是./就同级目录 ../指向上级
		String realPath = absolutePath.contains(".." + File.separator) ? parentFile.getParent() + uri.replace("..","") : absolutePath;
		File file = new File(realPath);
		if (file.exists()){
			return file;
		}
		return null;
	}
}
