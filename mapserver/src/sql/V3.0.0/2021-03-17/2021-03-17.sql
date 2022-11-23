/*系统日志表*/
DROP TABLE IF EXISTS system_log;
CREATE TABLE system_log(
	log_id serial primary key,              -- 日志id
	sys_log_level INTEGER NOT NULL,         -- 日志级别，当前主要有info、warning、error三个级别
	sys_log_content varchar(200) NOT NULL,  -- 日志内容
	sys_log_src varchar(20) NOT NULL,       -- 系统日志来源，如数据库、文件读写等
	sys_log_user varchar(20) NOT NULL DEFAULT 'admin',      --用户
	sys_log_src_ip varchar(20),             -- 当前系统日志来源的IP地址
	sys_log_time timestamp NOT NULL         -- 系统日志发生时间
);

/*删除操作日志中的日志级别列*/
alter table operation_log drop column log_level;