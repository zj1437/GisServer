package org.pygisserver.pytc.aop;

import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.exception.AccountsException;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.exception.StatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Description: 全局服务异常处理 
 * @author: 
 * @date: 2019-12-17
 */
@RestControllerAdvice
public class GlobalExceptionController {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionController.class);
	
	@ExceptionHandler(RuntimeException.class)
	public ReponseEntity<String> handleOtherException(RuntimeException e){
		log.error(e.getLocalizedMessage());
		return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(), "运行时异常："+ e.getLocalizedMessage(),e.getStackTrace()[0].toString());
	}
	
	@ExceptionHandler(BaseException.class)
	public ReponseEntity<String> handleBaseException(BaseException e){
		return new ReponseEntity<String>(e.getErrorCode(), e.getMessage());
	}
	
	@ExceptionHandler(AccountsException.class)
	public ReponseEntity<String> handleAccountsException(AccountsException e){
		return new ReponseEntity<String>(e.getErrorCode(), e.getMessage());
	}	
	@ExceptionHandler(ArgumentException.class)
	public ReponseEntity<String> handleArgumentException(ArgumentException e){
		return new ReponseEntity<String>(e.getErrorCode(), e.getMessage());
	}
	
	@ExceptionHandler(ResourceException.class)
	public ReponseEntity<String> handleResourceException(ResourceException e){
		return new ReponseEntity<String>(e.getErrorCode(), e.getMessage());
	}
	
	@ExceptionHandler(SqlException.class)
	public ReponseEntity<String> handleSqlException(SqlException e){
		return new ReponseEntity<String>(e.getErrorCode(), e.getMessage());
	}
	
	@ExceptionHandler(StatusException.class)
	public ReponseEntity<String> handleStatusException(StatusException e){
		return new ReponseEntity<String>(e.getErrorCode(), e.getMessage());
	}
}
