package org.pygisserver.common.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.SystemLogEntity;
import org.pygisserver.common.util.CommonUtil;
import org.springframework.stereotype.Component;

/**
 * @Description: 异常常量类 
 * @author:  
 * @date: 2019-12-12
 */
@Component
public class ExceptionsUtil {
	//运行时异常码
	public static final int ERROR_500 = 500;
	//用户权限相关异常码
	public static final int ERROR_403 = 403;
	//服务业务相关异常码
	public static final int ERROR_400 = 400;
	//状态异常码
	public static final int ERROR_301 = 301;
    
	/**
	 * 1. ArgumentException相关提示信息
	 */
    public static final String ARGGUMENT_ILLEGAL = "{0}不合法，请正确填写参数";
    public static final String ARGGUMENT_NULL = "{0}不能为空，请正确填写参数";
    public static final String ARGGUMENT_EXIST = "{0}不存在，请正确填写参数";
    public static final String ARGGUMENT_EMPTY = "{0}";
    
    
    /**
	 * 2.  BaseException 相关提示信息
	 */
    public static final String BASE_ERROR= "系统内部异常";
    public static final String BASE_SETUP = "{0}实例化失败";
    public static final String BASE_SERVER = "{0}失败";
    public static final String BASE_START = "{0}启动失败";
    public static final String BASE_STOP = "{0}停止失败";
    public static final String TERRAIN_ANALYSIS_PLUG = "当前系统无{0}插件";
    public static final String TERRAIN_ANALYSIS_PLUG_FAIL = "{0}插件加载失败";
    public static final String REMOTE_HTTP_FAIL = "远程服务请求失败或超时";
    
    /**
	 * 3. StatusException相关提示信息
	 */
    public static final String STATUS_SERVER = "服务状态异常，请确保服务正常运行";
    public static final String STATUS_SEND_MESSAGE = "{0}消息推送失败";
    public static final String STATUS_PUBLISH = "{0}同步消息失败";
    public static final String STATUS_AUTO = "自动发布服务失败，请检查数据和服务状态是否正常";
    public static final String STATUS_SERVER_PUBLISH = "自动发布服务失败，请检查数据和服务状态是否正常";
    public static final String STATUS_TIMEOUT = "{0}发布服务超时";
    public static final String STATUS_DATABASE = "数据库异常，{0}";
    public static final String STATUS_REDIS = "Redis异常，请确保Redis正常运行";
    public static final String STATUS_MONGODB = "MongoDB异常，请确保MongoDB正常运行";
    public static final String STATUS_POSTGRESQL = "Postgresql异常，请确保Postgresql正常运行";
    public static final String STATUS_PYTC = "Pytc服务异常，{0}信息同步失败";
    public static final String STATUS_PYTC_GET = "Pytc服务异常，{0}查询失败";
    
    /**
	 * 4. ResourceException相关提示信息
	 */
    public static final String RESOURCE_NOT_FOUND = "{0}不存在";
    public static final String RESOURCE_DOWNLOAD= "{0}下载失败";
    public static final String RESOURCE_CLOSE = "{0}关闭失败";
    public static final String RESOURCE_RELEASE = "{0}释放失败";
    public static final String RESOURCE_SAVE = "{0}保存失败";
    public static final String RESOURCE_GET = "{0}获取失败";
    public static final String RESOURCE_EDIT = "{0}编辑失败";
    public static final String RESOURCE_SECRET = "{0}失败";
    public static final String RESOURCE_PARSE = "{0}解析失败";
    public static final String RESOURCE_DELETE= "{0}删除失败";
    public static final String RESOURCE_DECODE = "文件路径或名称{0}解码失败"; 
    public static final String RESOURCE_UPLOAD = "文件{0}上传失败";
    public static final String RESOURCE_THUMBAL = "{0}缩略图生成失败";
    public static final String RESOURCE_UPDATE = "{0}更新失败";
    public static final String RESOURCE_TRANSFOR = "{0}转换失败";
    public static final String RESOURCE_MERGE = "{0}合并失败";
    public static final String RESOURCE_CREATE = "{0}创建失败";
    public static final String RESOURCE_FORMAT = "{0}格式化失败";
    public static final String RESOURCE_COPY = "{0}拷贝失败";
    public static final String RESOURCE_MOVE = "{0}移动失败";
    public static final String RESOURCE_SERIALZABLE = "{0}序列化失败";
    public static final String RESOURCE_COMPRESS = "{0}压缩失败";
    public static final String RESOURCE_DECOMPRESS = "{0}解压失败";
    public static final String RESOURCE_READ_WRITE = "{0}服务瓦片数据生成失败";
    public static final String RESOURCE_CAN_NOT_READ = "{0}存在，但是不可读";
    public static final String RESOURCE_NOT_IS_DIR = "{0}必须是目录";
    public static final String NATIVE_LIBRARY_NOT_EXISTS = "{0}动态库不存在";
    
    /**
	 * 5. SqlException相关提示信息
	 */
    public static final String SQL_GET = "{0}信息查询失败";
    public static final String SQL_CREATE = "{0}创建失败";
    public static final String SQL_ARGGUMENT = "Sql执行失败，请确定sql字段中路径超过限制";
    public static final String SQL_EXCUTION = "Sql执行失败，请确保服务和数据库正常运行";
    public static final String SQL_SQLITE = "Sql执行失败，请确保Sqlite正常运行";
    public static final String SQL_SAVE = "Sql执行失败，{0}保存失败";
    public static final String SQL_DELETE = "Sql执行失败，{0}删除失败";
    public static final String SQL_UPDATE = "Sql执行失败，{0}更新失败";
    public static final String SQL_OPEN = "Sql执行失败，{0}打开失败";
    public static final String SQL_CLOSE = "Sql执行失败，{0}关闭失败";
    public static final String SQL_VALUE_TOO_LONG = "Sql执行失败，请确保sql字段满足数据库字段长度";
    
    /**
   	 * 6. AccountsException相关提示信息
   	 */
    public static final String LOGIN_ERROR = "用户{0}登陆失败";
    public static final String ROLE_ERROR = "操作{0}失败，请检操作权限是否正确";
    
    /**
     * 文件解析失败
     */
    public static final String ANALYSIS_SHAPE_FILE = "矢量文件{0}解析失败";
    
    /**
     * 异常信息转换类，优先返回异常的message信息，否则将堆栈转换为字符串并完整返回
     * @param e
     * @return
     */
    public static String stackTraceToStr(Throwable e) {
    	String errorInfo = e.getMessage();
    	if(StringUtils.isBlank(errorInfo)) {
    		StringWriter sw = new StringWriter();
        	e.printStackTrace(new PrintWriter(sw));
        	errorInfo = sw.toString();
    	}
    	return errorInfo;
    }
    
    /**
     * 异常信息封装方法，将静态的提示信息与具体业务信息结合起来
     * @param srcInfo,tips
     * @return
     */
    public static String formatInfo(String srcInfo,String...tips) {
    	String tipsInfo = null;
    	if(StringUtils.isNoneBlank(srcInfo)) {
    		tipsInfo = MessageFormat.format(srcInfo, tips);
    	}
    	return tipsInfo;
    }
    
    //数据库异常的判断条件
    public static final String SYS_DB_ERROR_INFO = "An I/O error occurred while sending to the backend.";
    public static final String SYS_DB_LOG_INFO = "PostgreSQL服务异常，数据库连接失败！";
    //系统日志级别
    public static final Integer SYS_LOG_LEVEL_INFO = 0;
    public static final Integer SYS_LOG_LEVEL_WARN = 1;
    public static final Integer SYS_LOG_LEVEL_ERROR = 2;
    //本地IP标识
    public static final String LOCAL_HOST = "localhost";
    //系统试用相关
    public static final String CUS_CAN_USE_DATE_7 = "系统试用期不足7天";
    public static final String CUS_CAN_USE_DATE_3 = "系统试用期不足3天";
    public static final String CUS_CAN_USE_DATE_0 = "系统试用期已到";
    //系统资源相关
    public static final String SYS_CPU_RESOURCE_ALARM = "系统CPU使用率已超过80%";
    public static final String SYS_MEMORY_RESOURCE_ALARM = "系统内存使用率已超过80%";
    //系统安全相关
    public static final String SYS_LOGIN_LOCK = "用户{0}已经被锁定";
    public static final String SYS_LOGIN = "用户{0}成功登陆服务";
    public static final String SYS_LOGOUT = "用户{0}成功登出服务";
    public static final String SYS_LOGIN_FAIL = "用户{0}登陆服务失败";
    public static final String SYS_ILLEGAL_LOGIN_FAIL = "未知用户{0}登陆服务失败";
    
    public static final String SQLITE_DB_INIT_FAIL = "Sqlite数据库{0}初始化失败";
    
    public static final String FILE_READ_FAIL = "{0}文件数据读取失败";
    public static final String FILE_WRITE_FAIL = "{0}文件写数据失败";
    public static final String FILE_CREATE_FAIL = "{0}文件创建失败";
    public static final String FILE_DECOMPRESS_FAIL = "{0}文件解压失败";
    public static final String FILE_COMPRESS_FAIL = "{0}文件压缩失败";
    public static final String FILE_NOT_FOUND = "{0}数据文件不存在";
    
    public static final String WEB_SOCKET_ERROR = "WebSocket服务异常，无法推送消息";
    
    public static final String PYTC_SERVER_ERROR_INFO_1 = "org.apache.http.conn.HttpHostConnectException";
    public static final String PYTC_SERVER_ERROR_INFO_2 = ":8091";
    public static final String PYTC_SERVER_ERROR_INFO_3 = "No provider available from registry";
    public static final String PYTC_SERVER_ERROR_INFO_4 = "Failed to invoke the method";
    public static final String PYTC_SERVER_ERROR = "Pytc服务异常，{0}同步失败";
    
    public static final String POSTGRESQL_SERVER_ERROR_INFO_1 = "Failed to obtain JDBC Connection";
    public static final String POSTGRESQL_SERVER_ERROR_INFO_3 ="An I/O error occurred while sending to the backend";
    public static final String POSTGRESQL_SERVER_ERROR_INFO_2 = ":5436";
    public static final String POSTGRESQL_SERVER_ERROR = "Postgresql服务异常，{0}同步失败";
    
    public static final String REDIS_SERVER_ERROR_INFO_1 = "io.lettuce.core.RedisConnectionException";
    public static final String REDIS_SERVER_ERROR_INFO_2 = ":6379";
    public static final String REDIS_SERVER_ERROR_INFO_3 = "io.lettuce.core.RedisCommandTimeoutException";
    public static final String REDIS_SERVER_ERROR = "Redis服务异常，{0}获取失败";
    public static final String REDIS_SERVER_ERROR_GET = "Redis服务异常，{0}获取失败";
    public static final String REDIS_SERVER_ERROR_SAVE = "Redis服务异常，{0}保存失败";
    
    public static final String FILE_LOST = "{0}文件丢失";
    public static final String DATABASE_TABLE_LOST = "{0}数据表丢失";
    
    public static final String MONITOR_START = "自动发布文件监听器启动失败";
    public static final String MONITOR_STOP = "自动发布文件监听器停止失败";
    /**
     * 数据库异常和普通sql异常的判断工具方法，当前以如上两个信息作为判断依据
     * @param srcInfo,tips
     * @return
     */
    public static boolean isSysLog(String srcInfo) {
    	if(srcInfo.contains(SYS_DB_ERROR_INFO)) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * 通过异常信息关键字判断是否是Pytc服务异常
     * @param srcInfo
     * @return
     */
    public static boolean isPytcError(String srcInfo) {
    	if(srcInfo.contains(PYTC_SERVER_ERROR_INFO_3) || srcInfo.contains(PYTC_SERVER_ERROR_INFO_2)
    			|| srcInfo.contains(PYTC_SERVER_ERROR_INFO_1) || srcInfo.contains(PYTC_SERVER_ERROR_INFO_4)) {
    		return true;
    	}
    	return false;
    }
    
    public static final String MONGO_SERVER_ERROR_INFO_1 = "com.mongodb.MongoSocketOpenException";
    public static final String MONGO_SERVER_ERROR_INFO_2 = ":27017";
    public static final String MONGO_SERVER_ERROR_GET = "MongoDB服务异常，{0}获取失败";
    public static final String MONGO_SERVER_ERROR_SAVE = "MongoDB服务异常，{0}保存失败";
    
    /**
     * 通过异常信息关键字判断是否是MongoDB服务异常
     * @param srcInfo
     * @return
     */
    public static boolean isMongoDBError(String srcInfo) {
    	if(srcInfo.contains(MONGO_SERVER_ERROR_INFO_1) || srcInfo.contains(MONGO_SERVER_ERROR_INFO_2)) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * 通过异常信息关键字判断是否是Redis服务异常
     * @param srcInfo
     * @return
     */
    public static boolean isRedisError(String srcInfo) {
    	if(srcInfo.contains(REDIS_SERVER_ERROR_INFO_1) || srcInfo.contains(REDIS_SERVER_ERROR_INFO_2)
    			|| srcInfo.contains(REDIS_SERVER_ERROR_INFO_3)) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * 通过异常信息关键字判断是否是postgresql服务异常
     * @param srcInfo
     * @return
     */
    public static boolean isPostgresqlError(String srcInfo) {
    	if(srcInfo.contains(POSTGRESQL_SERVER_ERROR_INFO_1) || srcInfo.contains(POSTGRESQL_SERVER_ERROR_INFO_2)
    			|| srcInfo.contains(POSTGRESQL_SERVER_ERROR_INFO_3)) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * 数据库异常和普通sql异常的判断工具方法，当前以如上两个信息作为判断依据
     * @param srcInfo
     * @return
     */
    public static String getServerIp(String srcInfo) {
    	if(StringUtils.isBlank(srcInfo)) 
    		return CommonUtil.getLocalHost();
    	if(srcInfo.contains(LOCAL_HOST)) {
    		return CommonUtil.getLocalHost();
    	} else {
    		return "localhost";
    	}
    }
    
    /**
     * 数据库相关异常处理
     * @param errCode
     * @param errInfo
     * @param stackInfo
     * @return
     */
    public static SqlException createSqlExcep(int errCode,String errInfo,String stackInfo) {
    	if(isSysLog(stackInfo)) {
//    		buildSysLogEntity(SYS_LOG_LEVEL_ERROR,SYS_DB_LOG_INFO,"PostgreSQL",getServerIp(LOCAL_HOST));
    		return null;
    	} else {
    		return new SqlException(errCode,errInfo);
    	}
    }
    /**
     * 构造系统异常实体并入库
     * @param logLevel
     * @param logContent
     * @param logSrc
     * @param logSrcIP
     */
    public SystemLogEntity buildSysLogEntity(int logLevel, String logContent, String sysLogUser, String logSrc, String logSrcIP) {
    	SystemLogEntity slEntity = new SystemLogEntity(logLevel,logContent,sysLogUser,logSrc,logSrcIP,new Date());
    	return slEntity;
    }
    
}
