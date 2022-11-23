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

