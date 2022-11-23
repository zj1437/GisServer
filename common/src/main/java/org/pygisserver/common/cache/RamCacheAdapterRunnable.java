package org.pygisserver.common.cache;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.service.SystemLogService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RamCacheAdapterRunnable implements Runnable {
	//Redis配置文件的绝对路径
	private String redisConfPath;
	private String reqIP;
	//启动Redis服务的bat脚本
	private String startRedisBat;
	//最大缓存数，为0时表明不需要业务处理
	private long maxCacheSize;
	private SystemLogService systemLogService;
	private ExceptionsUtil exceptionsUtil;
	public RamCacheAdapterRunnable(String redisConfPath,String startRedisBat,long maxCacheSize,SystemLogService systemLogService,ExceptionsUtil exceptionsUtil,String reqIP) {
		this.redisConfPath = redisConfPath;
		this.startRedisBat = startRedisBat;
		this.maxCacheSize = maxCacheSize;
		this.systemLogService = systemLogService;
		this.exceptionsUtil = exceptionsUtil;
		this.reqIP = reqIP;
	}

	@Override
	public void run() {
		//1.先将原有配置文件删除，并创建新的空配置文件
		File file = new File(redisConfPath);
		try {
			if(file.exists()) {
				file.delete();
			}
			file.createNewFile();
		} catch (IOException e) {
			log.error("RamCacheAdapterRunnable run has error:" + ExceptionsUtil.stackTraceToStr(e));
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE,
					file.getName() + "配置文件"), "System","文件管理", reqIP));
		}
		
		//2.先将原有配置文件删
		StringBuilder redisConf = getRedisBefoConf().append("maxmemory ").append(maxCacheSize).append("mb").append("\n").append(getRedisAfteConf());
		FileWriter fWriter = null;
		try {
			fWriter = new FileWriter(redisConfPath, false);
			fWriter.write(redisConf.toString());
		} catch (IOException e) {
			log.error("RamCacheAdapterRunnable run has error:" + ExceptionsUtil.stackTraceToStr(e));
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE,
					file.getName() + "文件配置信息"), "System","文件管理", reqIP));
		} finally {
			try {
				fWriter.flush();
				fWriter.close();
			} catch (IOException e) {
				log.error("RamCacheAdapterRunnable run has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			
		}
		
		File startBat = new File(startRedisBat);
		if(!startBat.exists()) {
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, "Redis启动脚本" + startBat.getName()));
		}
		try {
			Runtime.getRuntime().exec(startRedisBat);
		} catch (IOException e) {
			log.error("RamCacheAdapterRunnable run has error:" + ExceptionsUtil.stackTraceToStr(e));
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.BASE_START,
					"Redis服务"), "System","Redis服务", reqIP));
		}
	}
	
	/**
	 * redis配置信息
	 * @return
	 */
	private StringBuilder getRedisBefoConf() {
		StringBuilder sb = new StringBuilder();
		sb.append("# include c:\\path\\to\\other.conf").append("\n")
		.append("# bind 127.0.0.1").append("\n")
		.append("port 6379").append("\n")
		.append("tcp-backlog 511").append("\n")
		.append("# unixsocketperm 700").append("\n")
		.append("timeout 0").append("\n")
		.append("tcp-keepalive 0").append("\n")
		.append("loglevel notice").append("\n")
		.append("logfile \"server_log.txt\"").append("\n")
		.append("syslog-enabled yes").append("\n")
		.append("syslog-ident redis").append("\n")
		.append("databases 16").append("\n")
		.append("save \"\"").append("\n")
		.append("stop-writes-on-bgsave-error yes").append("\n")
		.append("rdbcompression yes").append("\n")
		.append("rdbchecksum yes").append("\n")
		.append("dbfilename dump.rdb").append("\n")
		.append("dir ./").append("\n")
		.append("# slaveof <masterip> <masterport>").append("\n")
		.append("# masterauth <master-password>").append("\n")
		.append("slave-serve-stale-data yes").append("\n")
		.append("slave-read-only yes").append("\n")
		.append("repl-diskless-sync no").append("\n")
		.append("repl-diskless-sync-delay 5").append("\n")
		.append("# repl-ping-slave-period 10").append("\n")
		.append("# repl-timeout 60").append("\n")
		.append("repl-disable-tcp-nodelay no").append("\n")
		.append("# repl-backlog-size 1mb").append("\n")
		.append("# repl-backlog-ttl 3600").append("\n")
		.append("slave-priority 100").append("\n")
		.append("# min-slaves-max-lag is set to 10.").append("\n")
		.append("# maxclients 10000").append("\n")
		.append("# persistence-available [(yes)|no]").append("\n");
		return sb;
	}
	
	/**
	 * redis配置信息
	 * @return
	 */
	private StringBuilder getRedisAfteConf() {
		StringBuilder sb = new StringBuilder();
		//将缓存的清理策略打开，并设置为最近最少使用
		sb.append("maxmemory-policy volatile-lru").append("\n")
		//Redis中的LRU算法并非真正的LRU算法，而是近似于LRU的算法，可以通过修改maxmemory-samples样本数量redis
		//的LRU效果接近LRU算法，但是这会增加内存占用，因此非必要情况下尽量少修改该参数
		.append("# maxmemory-samples 5").append("\n")
		.append("appendonly no").append("\n")
		.append("appendfilename \"appendonly.aof\"").append("\n")
		.append("appendfsync everysec").append("\n")
		.append("no-appendfsync-on-rewrite no").append("\n")
		.append("auto-aof-rewrite-percentage 100").append("\n")
		.append("auto-aof-rewrite-min-size 64mb").append("\n")
		.append("aof-load-truncated yes").append("\n")
		.append("lua-time-limit 5000").append("\n")
		.append("# cluster-enabled yes").append("\n")
		.append("# cluster-config-file nodes-6379.conf").append("\n")
		.append("# cluster-node-timeout 15000").append("\n")
		.append("# cluster-slave-validity-factor 10").append("\n")
		.append("# cluster-migration-barrier 1").append("\n")
		.append("# cluster-require-full-coverage yes").append("\n")
		.append("slowlog-log-slower-than 10000").append("\n")
		.append("slowlog-max-len 128").append("\n")
		.append("latency-monitor-threshold 0").append("\n")
		.append("notify-keyspace-events \"\"").append("\n")
		.append("hash-max-ziplist-entries 512").append("\n")
		.append("hash-max-ziplist-value 64").append("\n")
		.append("list-max-ziplist-size -2").append("\n")
		.append("list-compress-depth 0").append("\n")
		.append("set-max-intset-entries 512").append("\n")
		.append("zset-max-ziplist-entries 128").append("\n")
		.append("zset-max-ziplist-value 64").append("\n")
		.append("hll-sparse-max-bytes 3000").append("\n")
		.append("activerehashing yes").append("\n")
		.append("client-output-buffer-limit normal 0 0 0").append("\n")
		.append("client-output-buffer-limit slave 256mb 64mb 60").append("\n")
		.append("client-output-buffer-limit pubsub 32mb 8mb 60").append("\n")
		.append("hz 10").append("\n")
		.append("aof-rewrite-incremental-fsync yes").append("\n")
		.append("# include /path/to/other.conf").append("\n")
		.append("").append("\n");
		return sb;
	}
}
