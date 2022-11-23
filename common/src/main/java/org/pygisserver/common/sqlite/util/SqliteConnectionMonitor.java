package org.pygisserver.common.sqlite.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class SqliteConnectionMonitor {
	
	/**
	 * 定时清理无用的连接
	 */
	@Scheduled(cron="0 0/1 * * * ?")
	public void closeSqliteConnection() {
		log.debug("开始监控sqlite连接池");
		Map<String, SQLiteConnectionPool> allConnectionPool = SQLitePoolManage.getSqliteConnectionMap();
		if (allConnectionPool == null || allConnectionPool.isEmpty()) {
			return;
		}
		Set<SQLiteConnectionPool> recentUsed = SQLitePoolManage.getUsedConnectionUsed();
		if (recentUsed == null || recentUsed.isEmpty()) {
			SQLitePoolManage.releaseSqliteConnectionMap();
		}
		
		Iterator<Map.Entry<String, SQLiteConnectionPool>> it = allConnectionPool.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, SQLiteConnectionPool> entry = it.next();
			SQLiteConnectionPool pool = entry.getValue();
			if (!recentUsed.contains(pool)) {
				SQLitePoolManage.releaseSqliteConnectionMap(pool);
			}
		}
		SQLitePoolManage.releaseConnection();
	}
}
