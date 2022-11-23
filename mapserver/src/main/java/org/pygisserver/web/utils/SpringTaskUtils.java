package org.pygisserver.web.utils;

import com.sun.management.OperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.entity.SystemLogEntity;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.web.dao.MapStatsticMapper;
import org.pygisserver.web.entity.ServerRequestInfo;
import org.pygisserver.web.entity.ServerStatisticinfo;
import org.pygisserver.web.entity.SysOperationLog;
import org.pygisserver.web.entity.SysResourceRecord;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.service.SystemService;
import org.geotools.data.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * spring定时处理存储过程
 * 
 * 
 * @author Administrator
 *
 */
@Component
@EnableScheduling
@Slf4j
public class SpringTaskUtils {

	@Autowired
	private MapStatsticService mapStatsticService;
	
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private SystemLogService systemLogService;

	@Autowired
	private SystemService systemService;
	
	@Resource
    private MapStatsticMapper mapStatsticMapper;
	
	@Autowired
	private RedisUtils redisUtils;
	
	private static final int PERCENT = 100;
	
	private static final int PERCENT_OF_ALARM = 80;
	
	private static final int FAULTLENGTH = 10;
	//系统日志保存的最长时间(单位是天)
	private static final int SYS_LOG_SAVE_MAX_TIME = 30;

	private Date saveLastEndTime;

	/**
	 * 每隔1min执行一次数据保存
	 * 保存的时候查看是否有脏数据存储进去
	 * 1、如果删除时当前服务没有新建同名服务，则直接删除
	 * 2、如果删除时当前服务已有同名服务，则删除的统计信息为
	 *
	 */
	@Scheduled(cron="0 0/1 * * * ?")
	@Async
	public void saveReqEntity() {
//		//每隔三分钟从redis中获取数据
		List<ServerRequestInfo> reqInfos = (List<ServerRequestInfo>) redisUtils.getList(ExceptionsUtil.getServerIp(null),"request-static");
		mapStatsticService.saveServerRequest(reqInfos);

		//成功调用后，处理性能统计
		if (reqInfos != null && reqInfos.size() > 0) {
			List<ServerStatisticinfo> serverStatisticinfos = new ArrayList<>();
			// 将此次更新数据的最大响应时间、最小响应时间、平均响应时间、并发量、访问量
			Map<String, List<ServerRequestInfo>> groupData = reqInfos.stream().collect(Collectors.groupingBy(ServerRequestInfo::getServerName));
			groupData.forEach((serverName, value) ->{
				ServerStatisticinfo serverStatisticinfo = new ServerStatisticinfo();
				serverStatisticinfo.setServerId(value.get(0).getServerId());
				serverStatisticinfo.setServerName(serverName);
				long maxTime = value.stream().max(Comparator.comparingLong(ServerRequestInfo::getReponseTime)).get().getReponseTime();
				serverStatisticinfo.setMaxReponseTime(maxTime);
				long minTime = value.stream().min(Comparator.comparingLong(ServerRequestInfo::getReponseTime)).get().getReponseTime();
				serverStatisticinfo.setMinReponseTime(minTime);
				double avgTime = value.stream().mapToLong(ServerRequestInfo::getReponseTime).average().getAsDouble();
				serverStatisticinfo.setAverageTime(Math.round(avgTime));
				value.forEach(doc ->{
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					doc.setRequetStartTimeStr(sdf.format(doc.getRequetStartTime()));
				});
				Map<String, List<ServerRequestInfo>> collectConcurrents = value.stream().collect(Collectors.groupingBy(ServerRequestInfo::getRequetStartTimeStr));
				int maxConcurrent = 0;
				for (String requestTime : collectConcurrents.keySet()) {
					if (maxConcurrent < collectConcurrents.get(requestTime).size()) {
						maxConcurrent = collectConcurrents.get(requestTime).size();
					}
				}
				serverStatisticinfo.setMaxConcurrent(maxConcurrent);
				serverStatisticinfo.setAccessNum(value.size());
				serverStatisticinfos.add(serverStatisticinfo);
			});
			mapStatsticService.saveStatisticServerRequest(serverStatisticinfos);
		}
	}

	
	/**
	 * 每隔5分钟计算一次系统资源消耗
	 *
	 * @throws InterruptedException
	 */
	@Scheduled(cron="0 0/5 * * * ?")
	public void saveSysResource() throws InterruptedException {
		double cpuReource = getCpuRation();
		if(cpuReource > PERCENT_OF_ALARM) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, ExceptionsUtil.SYS_CPU_RESOURCE_ALARM, 
					"System","系统资源监控", ExceptionsUtil.getServerIp(null)));
		}
		Map<String, Object> memoryUse = getUseMemorySize();
		double totalSize = (double) memoryUse.get("totalSize");
		double useSize = (double) memoryUse.get("useSize");
		double usrPer = (double) memoryUse.get("perSize");
		if(cpuReource > PERCENT_OF_ALARM) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, ExceptionsUtil.SYS_MEMORY_RESOURCE_ALARM, 
					"System","系统资源监控", ExceptionsUtil.getServerIp(null)));
		}
		SysResourceRecord sysRecord = new SysResourceRecord(cpuReource, totalSize, useSize, usrPer, new Date());
		mapStatsticService.saveSysResource(sysRecord);
	}


	private double getCpuRation() throws InterruptedException{
		SystemInfo systemInfo = new SystemInfo();
		CentralProcessor processor = systemInfo.getHardware().getProcessor();
		long[] cpuLoadTicks = processor.getSystemCpuLoadTicks();
		TimeUnit.SECONDS.sleep(1);
		long[] cpuLoadTicks1 = processor.getSystemCpuLoadTicks();
		long nice = cpuLoadTicks[CentralProcessor.TickType.NICE.getIndex()] - cpuLoadTicks1[CentralProcessor.TickType.NICE.getIndex()];
		long user = cpuLoadTicks[CentralProcessor.TickType.USER.getIndex()] - cpuLoadTicks1[CentralProcessor.TickType.USER.getIndex()];
		long system = cpuLoadTicks[CentralProcessor.TickType.SYSTEM.getIndex()] - cpuLoadTicks1[CentralProcessor.TickType.SYSTEM.getIndex()];
		long idle = cpuLoadTicks[CentralProcessor.TickType.IDLE.getIndex()] - cpuLoadTicks1[CentralProcessor.TickType.IDLE.getIndex()];
		long iowait = cpuLoadTicks[CentralProcessor.TickType.IOWAIT.getIndex()] - cpuLoadTicks1[CentralProcessor.TickType.IOWAIT.getIndex()];
		long irq = cpuLoadTicks[CentralProcessor.TickType.IRQ.getIndex()] - cpuLoadTicks1[CentralProcessor.TickType.IRQ.getIndex()];
		long softirq = cpuLoadTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - cpuLoadTicks1[CentralProcessor.TickType.SOFTIRQ.getIndex()];
		long steal = cpuLoadTicks[CentralProcessor.TickType.STEAL.getIndex()] - cpuLoadTicks1[CentralProcessor.TickType.STEAL.getIndex()];
		long total = nice + user + system + idle + iowait + irq + softirq + steal;
		long used = total - idle;

		return CommonUtil.format2Decimal(Math.abs(used * PERCENT * 1.0 / total), 2);
	}
	
	
	/**
	 * 获取CPU的时间
	 * @param proc 进程信息
	 * @return
	 */
	private long[] readCpu(final Process proc) {
		long[] retn = new long[2];
		try {
			proc.getOutputStream().close();
			InputStreamReader isr = new InputStreamReader(proc.getInputStream());
			LineNumberReader input = new LineNumberReader(isr);
			String line = input.readLine();
			if (line == null || line.length() < FAULTLENGTH) {
				return null;
			}

			int capidx = line.indexOf("Caption");
			int cmdidx = line.indexOf("CommandLine");
			int rocidx = line.indexOf("ReadOperationCount");
			int kmtidx = line.indexOf("KernelModeTime");
			int umtidx = line.indexOf("UserModeTime");
			int wocidx = line.indexOf("WriteOperationCount");
			long idletime = 0;
			long kneltime = 0;
			long usertime = 0;
			while ((line = input.readLine()) != null) {
				if (line.length() < wocidx) {
					continue;
				}

				String caption = substring(line, capidx, cmdidx - 1).trim();
				String cmd = substring(line, cmdidx, kmtidx - 1).trim();
				if (cmd.indexOf("wmic.exe") >= 0) {
					continue;
				}

				if (caption.equals("System Idle Process") || caption.equals("System")) {
					idletime += Long.valueOf(substring(line, kmtidx, rocidx - 1).trim()).longValue();
					idletime += Long.valueOf(substring(line, umtidx, wocidx - 1).trim()).longValue();
					continue;
				}
				String kneltimeStr = substring(line,kmtidx,rocidx - 1).trim();
				String usertimeStr = substring(line,umtidx,wocidx - 1).trim();
				if (StringUtils.isNotBlank(kneltimeStr)) {
					kneltime += Long.valueOf(kneltimeStr).longValue();
				}
				if (StringUtils.isNotBlank(usertimeStr)) {
					usertime += Long.valueOf(usertimeStr.replace(" ", "")).longValue();
				}
			}
			retn[0] = idletime;
			retn[1] = kneltime + usertime;
			return retn;

		} catch (IOException e) {
			log.error("SpringTaskUtils readCpu has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "系统资源统计信息"));
		} finally {
			try {
				proc.getInputStream().close();
			} catch (IOException e) {
				log.error("SpringTaskUtils readCpu has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
		}
	}

	private String substring(String src, int start_idx, int end_idx) {
		String tgt = "";
		byte[] buf = src.getBytes();
		for (int i = start_idx; i <= end_idx; i++) {
			tgt += (char) buf[i];
		}
		return tgt;
	}

	
	/**
	 * 获取总的物理内存
	 * @return
	 */
	private Map<String,Object> getUseMemorySize() {
		Map<String,Object> param = new HashMap<String,Object>();
		OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		// 获取物理总内存
		long totalMemorySize = osmxb.getTotalPhysicalMemorySize();
		param.put("totalSize", CommonUtil.format2Decimal(totalMemorySize / (1024.0 * 1024.0 * 1024.0),2));
		// 获取当前的空闲内存
		long freeMemorySize = osmxb.getFreePhysicalMemorySize();
		param.put("useSize", CommonUtil.format2Decimal((totalMemorySize - freeMemorySize) / (1024.0 * 1024.0 * 1024.0),2));
		// 使用的空闲时百分比
		double userPercent = CommonUtil.format2Decimal(PERCENT * (totalMemorySize - freeMemorySize) * 1.0 / totalMemorySize, 2);
		param.put("perSize", userPercent);
		return param;
	}
	
	
	/**
	 * 每隔10分钟更更新系统最近使用时间
	 *
	 * @throws IOException 
	 */
	@Scheduled(cron="0 0/10 * * * ?")
	public void updateSysTime() {
		systemService.updateSysTime();
	}
	
	
	/**
	 * 每小时检查一次数据连接情况，如果从redis中获取的连接不可用，就将其移除并重新创建连接，如果创建的连接可用就存入redis，否则
	 *
	 * @throws IOException
	 */
	@Scheduled(cron="0 0 * * * ?")
	public void checkPostGisConnectStatus() throws IOException {
		List<PostGisConnectEntity> entities = systemService.queryPostGisById(-1);
		if(entities.size() == 0) {
			return;
		}
		DataStore store = null;
		String sourceName = null;
		for (PostGisConnectEntity postGisConnectEntity : entities) {
			try {
				//数据库连接检查时先清理数据库连接缓存，重新获取数据库连接再测试
				sourceName = postGisConnectEntity.getDataSourceName();
				PostGisStoreUtils.deletePostGisFromCache(sourceName);
				postGisConnectEntity.setPassword(PostGisStoreUtils.getDecodePass(postGisConnectEntity.getPassword()));
				store = PostGisStoreUtils.getPostGiDataStore(postGisConnectEntity);
				//如果数据库连接失败，获取postgis图层时会抛出异常，只是获取postgis连接并不准确
				String[] layers = store.getTypeNames();
				//如果数据库连接成功，且数据库原来状态为异常，则将数据库状态恢复为正常状态。
				if(postGisConnectEntity.getDbStatus() == 2) {
					systemService.updatePostGisStatus(postGisConnectEntity.getDbId(),1);
				}
			} catch (Exception e) {
				try {
					PostGisStoreUtils.deletePostGisFromCache(sourceName);
				} catch (Exception e2) {
					// do nothing
				}
				//抛出异常时，该数据库已经是异常状态时，就不再更新数据库
				if(postGisConnectEntity.getDbStatus() == 1) {
					systemService.updatePostGisStatus(postGisConnectEntity.getDbId(),2);
				}
			}
			
		}
	}
	
	
	/**
	 *系统日志和操作日志清除任务。定时任务每天0点开始执行，清除30天之前的日志信息
	 */
	@Scheduled(cron="0 0 0 * * ?")
	public void clearSysLogRecords() {
		LocalDate now = LocalDate.now().minusDays(SYS_LOG_SAVE_MAX_TIME);
		Date d = Date.from(now.atStartOfDay(ZoneId.systemDefault()).toInstant());
		List<Integer> gcLogIds = null;
		//清除系统日志
		List<SystemLogEntity> sysLogs = mapStatsticService.querySystemLogs(null);
		if(sysLogs.size() > 0) {
			gcLogIds = sysLogs.stream().filter(new Predicate<SystemLogEntity>() {

				@Override
				public boolean test(SystemLogEntity t) {
					if(t.getLogHappenedTime().before(d)) {
						return true;
					}
					return false;
				}
			}).map(SystemLogEntity::getLogId).distinct().collect(Collectors.toList());
		}
		if(gcLogIds != null && gcLogIds.size() > 0) {
			mapStatsticService.deleteSystemLog(gcLogIds);
		}
		//清除操作日志
		gcLogIds = null;
		List<SysOperationLog> oprLogs = mapStatsticService.queryAllSysOprationLog();
		if(oprLogs.size() > 0) {
			gcLogIds = oprLogs.stream().filter(new Predicate<SysOperationLog>() {

				@Override
				public boolean test(SysOperationLog t) {
					if(t.getOperateTime().before(d)) {
						return true;
					}
					return false;
				}
			}).map(SysOperationLog::getId).distinct().collect(Collectors.toList());
		}
		if(gcLogIds != null && gcLogIds.size() > 0) {
			mapStatsticService.deleteOprationLog(gcLogIds);
		}
	}
	
	
//	@Scheduled(cron="0 0/1 * * * ?")
//	public void closeStream() {
//		Map<String,FileInputStream> map = FileUtil.getCloseStream();
//		if (map != null && !map.isEmpty()) {
//			try {
//				FileUtil.closeStream(map);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			FileUtil.clearStream(map);
//		}
//		
//	}

}
