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

/*添加海图比例尺信息*/
alter table map_feature_info add column sea_chart_scale text;

/*系统管理表添加样式文件路径字段*/
alter table system_config_info add column style_path varchar(200);

DROP TABLE IF EXISTS server_cache_config;
CREATE TABLE server_cache_config(
	id serial primary key,   -- id
	min_ram INTEGER,         -- 内存最小配置
	max_ram INTEGER,         -- 内存最大配置
	actual_ram INTEGER,      -- 用户设定的内存配置
	min_disk INTEGER,        -- 磁盘最小配置
	max_disk INTEGER,        -- 磁盘最大配置
	actual_disk INTEGER,      -- 用户设定的磁盘配置
	cache_path varchar(100),        -- 磁盘路径
	use_redis_cache smallint NOT NULL --内存缓存使用开关，0是true；1是false
);

insert into server_cache_config(id,min_ram,max_ram,actual_ram,min_disk,max_disk,actual_disk,cache_path,use_redis_cache) values(default,100,0,100,1,0,0,'',1);

/*移除服务访问量统计表*/
drop table if exists sys_visit_num;
/*新增服务访问性能监控表*/
drop table if exists server_visit_statistic;
CREATE TABLE server_visit_statistic(
	id serial,           -- 自增id
	server_name varchar(50) NOT NULL primary key, -- 服务名称
	max_reponse_time double precision not null,  -- 最大响应时间
	min_reponse_time double precision not null,  -- 最小响应时间
	ave_reponse_time double precision not null,  -- 平均响应时间
	max_concurrent integer not null,   -- 最大并发量
	visit_num integer not null   -- 访问总量
);

/*角色表*/
DROP TABLE IF EXISTS role_info;
CREATE TABLE role_info(
	role_id serial primary key,       -- 角色id
	role_name varchar(20) NOT NULL,   -- 角色名称
	role_creator varchar(20) NOT NULL, -- 角色创建人
	gen_time timestamp NOT NULL,      -- 创建日期
	update_time timestamp NOT NULL,   -- 角色更新时间
	desciption varchar(200),           -- 角色描述
	is_system smallint default 1       -- 是否是系统默认角色
);

/*初始化角色表*/
INSERT INTO public.role_info(role_id,role_name,role_creator,gen_time,update_time,desciption) VALUES (DEFAULT,'系统管理员','system',now(),now(),'内置的系统管理员，此角色默认拥有整个PYGISServer的管理权限，可以登录服务执行对服务、监控、安全、日志、系统等配置，拥有对整个服务器的管理权限，需要慎重使用');
INSERT INTO public.role_info(role_id,role_name,role_creator,gen_time,update_time,desciption) VALUES (DEFAULT,'数据管理员','system',now(),now(),'内置的服务发布者，此角色默认拥有服务发布和服务实例管理的权限，如发布、启用、停用、删除服务实例及切图服务的所有操作等，主要用于管理系统的数据与服务');
INSERT INTO public.role_info(role_id,role_name,role_creator,gen_time,update_time,desciption) VALUES (DEFAULT,'普通用户','system',now(),now(),'内置的PYGISServer查看与使用者角色，具有查看PYGISServer发布的所有服务的权限，包括地图、模型、倾斜摄影、POI等，但其无法自行发布服务');