package org.pygisserver.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 日期处理工具类
 * @author: 
 * @date: 2019-11-28
 */
@Slf4j
public class DateUtil {

	private static String HOUR_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	private static String ACCUTE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	
	private static String DAY_FORMAT = "yyyy-MM-dd";
	
	
	/**
	 * 将当前日期转换为字符串，并且追加至当前日期的00:00:00
	 * @param date
	 * @return
	 */
	public static String coverTimeToStart(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DAY_FORMAT);
		String dateStr = dateFormat.format(date);
		return dateStr + " 00:00:00";
	}
	
	
	public static Date covertStringToDate(String dateStr) {
		try {
			return new SimpleDateFormat(DAY_FORMAT).parse(dateStr);
		} catch (ParseException e) {
			log.error("DateUtil covertStringToDate has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "字符串转换为Date"));
		}
	}
	
	/**
	 * 将时间字符串转换为日期格式
	 * @param dateStr
	 * @return
	 * @throws ParseException 
	 */
	public static Date covertStringToDateHour(String dateStr) {
		try {
			return new SimpleDateFormat(HOUR_FORMAT).parse(dateStr);
		} catch (ParseException e) {
			log.error("DateUtil covertStringToDateHour has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "字符串转换为Date"));
		}
	}
	
	/**
	 * 将时间字符串转换为日期格式
	 * @param dateStr
	 * @return
	 * @throws ParseException 
	 */
	public static long covertStringToTimestamp(String dateStr) {
		try {
			return new SimpleDateFormat(HOUR_FORMAT).parse(dateStr).getTime();
		} catch (ParseException e) {
			log.error("DateUtil covertStringToDateHour has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "字符串转换为Date"));
		}
	}
	
	
	/**
	 * 将时间戳转换为日期类型
	 * @param time
	 * @return Date
	 * @throws ParseException
	 */
	public static Date convertTimestampToDate(String timeStr)  {
		Date date = null;
		Date d = new Date(Long.valueOf(timeStr));
		SimpleDateFormat sdFormat = new SimpleDateFormat(HOUR_FORMAT);
		String formattedTime = sdFormat.format(d);
		try {
			date = sdFormat.parse(formattedTime);
		} catch (ParseException e) {
			log.error("DateUtil convertTimestampToDate has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "时间戳转换为Date"));
		}
		return date;
	}
	
	/**
	 * 将当前日期转换为字符串
	 * @param date
	 * @return
	 */
	public static String covertTimeTostr(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(HOUR_FORMAT);
		String dateStr = dateFormat.format(date);
		return dateStr;
	}
	
	/**
	 * 将当前日期转换为字符串
	 * @param date
	 * @return
	 */
	public static String covertAccuteTimeTostr(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(ACCUTE_TIME_FORMAT);
		String dateStr = dateFormat.format(date);
		return dateStr;
	}
	
	/**
	 * 将当前日期转换为字符串
	 * @param date
	 * @return
	 */
	public static String covertTimeToDayStr(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DAY_FORMAT);
		String dateStr = dateFormat.format(date);
		return dateStr;
	}
	
	
	
	/**
	 * 将当前日期转换为字符串
	 * @param date
	 * @return
	 */
	public static String covertTimeTostr(long dateTime) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(HOUR_FORMAT);
		String dateStr = dateFormat.format(new Date(dateTime));
		return dateStr;
	}
	
	/**
	 * 获取当前月的第一天
	 * @return
	 */
	public static String getFirstDayOfMouth() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		SimpleDateFormat dateFormat = new SimpleDateFormat(DAY_FORMAT);
		return dateFormat.format(calendar.getTime());
	}
	
	
	/**
	 * 获取当前月的第一天
	 * @return
	 */
	public static Date getFirstDayOfMouthDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}
	
	/**
	 * 获取当天的0点0分0秒
	 * @return
	 */
	public static Date getFirstTimeOfDay() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	/**
	 * 获取当天的0点0分0秒
	 * @return
	 */
	public static Date getNowTimeOfDay() {
		Calendar calendar = Calendar.getInstance();
		return calendar.getTime();
	}
	
	/**
	 * 获取给定时间的23点59分59秒
	 * @return
	 */
	public static Date getEndTimeOfDay(String dateTime) {
		Date date = covertStringToDateHour(dateTime);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY,23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}
}
