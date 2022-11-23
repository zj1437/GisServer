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