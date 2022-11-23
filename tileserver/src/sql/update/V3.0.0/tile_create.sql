/*切图任务表详情表*/
drop table IF EXISTS tile_task_info;
    create table tile_task_info(
    task_id serial primary key,
    task_name varchar(50) NOT NULL UNIQUE,         -- 任务名称 
    layer_style varchar(20),                -- 图层样式
    store_type smallint NOT NULL,           -- 存储类型（1，postgresql，2 sqlite，3 mongodb）
    store_path varchar(200),                 -- 存储路径，只有选择sqlite的存储方式时，开启该选项
    srs_style varchar(20) NOT NULL,         -- 投影格式
    task_status smallint NOT NULL,          -- 任务状态
    tile_style varchar(50),       			 -- 切图格式
    seed_type varchar(10) NOT NULL,         -- 切图类型
    start_seed_time timestamp NOT NULL,     -- 切图开始时间
    max_task_num integer NOT NULL,          -- 最大任务数
    thread_num integer NOT NULL,            -- 切图线程总数
    zoom_start integer NOT NULL DEFAULT 0,  -- 起始级别
    zoom_end integer NOT NULL,              -- 结束级别
    min_lon double precision NOT NULL, 		-- 最小经度
    min_lat double precision NOT NULL, 		-- 最小纬度
	max_lon double precision NOT NULL,      --最大经度
    max_lat double precision NOT NULL,  	--最大纬度
    tile_width integer,
    tile_height integer
);

/**
 * 数据库存储资源信息
 */
drop table IF EXISTS dynamic_data_source;
create table dynamic_data_source(
	source_id serial primary key,      -- 自增id
    source_key varchar(100) NOT NULL,  -- 数据库名称
    layer_name varchar(50) NOT NULL,     -- 图层名称
    user_name varchar(50) default '',    -- 用户名
    password varchar(50) default '',     -- 密码
    driver_class varchar(50),   		 -- 驱动类
    jdbc_Url varchar(100)       		 -- jdbc url
);

/**
 * psql和sqlite对应切图数据库的表
 */
drop table IF EXISTS table_source;
create table table_source(
	table_id serial primary key,      -- 自增id
    source_id integer NOT NULL REFERENCES dynamic_data_source(source_id) ON delete cascade,     -- 数据库id
    table_name varchar(50) NOT NULL   -- 表名称
);

/*增加当前图层以及当前请求类型的存储方式*/
drop table IF EXISTS layer_store_info;
create table layer_store_info(
    source_name varchar(100) NOT NULL,   -- 数据库名称或者表名（psql和sqlite是数据库，mongodb是collection   tdb是文件夹名称）
    layer_name varchar(50) NOT NULL,     -- 图层名称
	store_type smallint NOT NULL,        -- 存储类型（1，postgresql，2 sqlite，3 mongodb,4 tdb）
	store_path varchar(200),             -- 存储路径，只有选择sqlite，tdb的存储方式时，开启该选项,
	tile_style varchar(50)               -- 切图格式
);

DROP TABLE IF EXISTS tile_task_entity_info;
CREATE TABLE tile_task_entity_info(
	task_id serial primary key,              -- 主键id
	task_name varchar(50) NOT NULL,          -- 任务名称
	task_status int NOT NULL,                -- 任务状态
	progress varchar(50)  NOT NULL,          -- 任务进度
	tiled_tiles integer,
	total_tiles integer,
	spent_time_str varchar(50)   NOT NULL,   -- 切图使用时间
	spent_time bigint,                       -- 切图使用时间-整型
	time_remaining_str varchar(50) ,         -- 切图剩余时间
	time_remaining bigint,                   --切图剩余时间-整型
	support_grid varchar(50) ,         		 -- 投影坐标系
	support_format varchar(50),          	 -- 瓦片格式
	task_start_time varchar(50),
	task_end_time bigint
);
