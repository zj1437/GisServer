package org.pygisserver.web.aop;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.AccountsException;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.exception.StatusException;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.websocket.WebSocketManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.alibaba.fastjson.JSONObject;

/**
 * @Description: 全局服务异常处理 
 * @author:  
 * @date: 2019-12-17
 */
@RestControllerAdvice
public class GlobalExceptionController {
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private SystemLogService systemLogService;
	
	@Autowired
	private WebSocketManager webSocketManager;

	@ExceptionHandler(BaseException.class)
	public ReponseEntity<String> handleBaseException(BaseException e,
													 HttpServletRequest request,
													 HttpServletResponse response){
		if (e.getErrorCode() == ErrorCode.NOT_FOUND.getErrorCode()) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
   			return null;
		}
		return new ReponseEntity<String>(e.getErrorCode(), e.getErrorMsg());
	}
	
	@ExceptionHandler(RuntimeException.class)
	public ReponseEntity<String> handleOtherException(RuntimeException e){
		if(ExceptionsUtil.isPostgresqlError(ExceptionsUtil.stackTraceToStr(e))) {
			Map<String,Object> msgMap = new HashMap<String,Object>();
			msgMap.put("msg", ExceptionsUtil.STATUS_POSTGRESQL);
			msgMap.put("msgKey", "exception");
			JSONObject jsonObject = new JSONObject(msgMap);
			webSocketManager.sendMessage(jsonObject.toJSONString());
		}
		return new ReponseEntity<String>(ExceptionsUtil.ERROR_400, "运行时异常：" + ExceptionsUtil.stackTraceToStr(e));
	}
	
	@ExceptionHandler(AccountsException.class)
	public ReponseEntity<String> handleAccountsException(AccountsException e){
		return new ReponseEntity<String>(e.getErrorCode(), e.getErrorMsg());
	}	
	@ExceptionHandler(ArgumentException.class)
	public ReponseEntity<String> handleArgumentException(ArgumentException e){
		return new ReponseEntity<String>(e.getErrorCode(), e.getErrorMsg());
	}
	
	@ExceptionHandler(ResourceException.class)
	public ReponseEntity<String> handleResourceException(ResourceException e){
		if(e.getErrorMsg().contains("S57海图本地库")) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.FILE_LOST,
					e.getErrorMsg()),UserUtil.getUserName(), "文件管理", ExceptionsUtil.getServerIp(null)));
		}
		return new ReponseEntity<String>(e.getErrorCode(), e.getErrorMsg());
	}
	
	@ExceptionHandler(SqlException.class)
	public ReponseEntity<String> handleSqlException(SqlException e){
		if(ExceptionsUtil.isPostgresqlError(ExceptionsUtil.stackTraceToStr(e))) {
			Map<String,Object> msgMap = new HashMap<String,Object>();
			msgMap.put("msg", ExceptionsUtil.STATUS_POSTGRESQL);
			msgMap.put("msgKey", "exception");
			JSONObject jsonObject = new JSONObject(msgMap);
			webSocketManager.sendMessage(jsonObject.toJSONString());
		}
		return new ReponseEntity<String>(e.getErrorCode(), e.getErrorMsg());
	}
	
	@ExceptionHandler(StatusException.class)
	public ReponseEntity<String> handleStatusException(StatusException e){
		
		return new ReponseEntity<String>(e.getErrorCode(), e.getErrorMsg());
	}
	
	 /***
     * 404处理
     * @param response
     * @return
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public void notFountHandler(HttpServletRequest request,HttpServletResponse response){
    	String str = request.getRequestURI();
    	if ("/pygisserver/".equals(str)) {
    		try {
    			response.sendRedirect("/pygisserver/index.html");
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}else if("/pygisserver/activation".equals(str)){
			try {
				response.sendRedirect("/pygisserver/activation.html");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
    		//404处理
    		response.setStatus(HttpStatus.NOT_FOUND.value());
    	}
		
    }
	
}
