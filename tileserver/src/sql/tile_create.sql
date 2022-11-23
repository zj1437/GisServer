/**
 * 自动切片配置表
 */
drop table IF EXISTS auto_tile_config;
create table auto_tile_config(
    store_id serial primary key,
    store_type smallint NOT NULL DEFAULT 1,  -- 瓦片存储方式
    seed_type varchar(10) NOT NULL DEFAULT '补全切片',    -- 切图方式
    store_path varchar(200),                 -- 存储路径，只有选择sqlite的存储方式时，开启该选项
    default_srs varchar(20) NOT NULL DEFAULT '同数据源',   -- 投影方式
    raster_format varchar(20) NOT NULL,                 -- 影像切片格式
    dem_format varchar(20) NOT NULL,                    -- 高程切片格式
    vector_format varchar(50) NOT NULL,                 -- 矢量切片格式
   	tile_width_size int ,           					-- 瓦片大小（宽） 如果为矢量这两个值则去掉
    tile_height_size int,           					-- 瓦片大小（高）
    tile_max_level int NOT NULL DEFAULT 6,              -- 切片最大级别
    max_task_num integer NOT NULL DEFAULT 3,            -- 最大任务数
    default_thread_num integer NOT NULL DEFAULT 5       -- 任务最大的线程数
);

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
    meta_x smallint,
    meta_y smallint,
    gutter smallint,
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
 * 表资源
 */
drop table IF EXISTS table_source;
create table table_source(
	table_id serial primary key,      -- 自增id
    source_id integer NOT NULL REFERENCES dynamic_data_source(source_id) ON delete cascade,     -- 数据库id
    table_name varchar(50) NOT NULL   -- 表名称
);

/**
 * 切图数据的元数据信息
 */
drop table if EXISTS tile_layer_info;
create table tile_layer_info(
    tile_id serial primary key,            -- 自增id
    server_id integer NOT NULL,            -- 服务id
    layer_name varchar(50) NOT NULL,       -- 图层名称
    is_enable smallint NOT NULL,           -- 是否可用  0 暂停  1可用 2针对TDB和MBTiles
    layer_status smallint NOT NULL,        -- 图层状态
    file_type varchar(10) NOT NULL, -- 文件类型
    data_style smallint,            -- 0 栅格  1 高程  2 矢量 3 切片
    layer_style varchar(50),        -- 图层样式
    native_crs varchar(20) NOT NULL,       -- 数据投影信息
    native_bounding_box varchar(100), -- 原图bbox
    max_zoom_level integer,        -- 图层最大级别
	meta_x smallint,
    meta_y smallint,
    gutter smallint,
    tile_width integer,
    tile_height integer,
    file_url varchar(200),         -- 文件url
 	resource_srs varchar(20),      -- 元数据投影信息
	resource_bbox varchar(100),    -- 元数据bbox
	publish_time timestamp,      	-- 发布日期
	server_protocol varchar(50)       -- 协议信息
);


DROP TABLE IF EXISTS server_provider_info;
CREATE TABLE server_provider_info(
	id serial primary key,           -- 系统id
	title_name varchar(50),        -- 标题
	abstract_name varchar(50),     -- 摘要
	keywords varchar(50),          -- 关键字
	online_resource varchar(100),  -- 在线资源
	contact_Organization varchar(100) NOT NULL,    --单位
	web_site varchar(100),         -- 网站
	contact_person varchar(50) NOT NULL,
	person_primary varchar(50),
	contact_telephone varchar(50) NOT NULL,
	contact_fax varchar(50),
	address varchar(200) NOT NULL,
	city varchar(50),
	province varchar(50),
	country varchar(50),
	postcode varchar(50),
	email_address varchar(50)
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
	spent_time bigint,                        -- 切图使用时间-整型
	time_remaining_str varchar(50) ,         -- 切图剩余时间
	time_remaining bigint,                    --切图剩余时间-整型
	support_grid varchar(50) ,         -- 投影坐标系
	support_format varchar(50),          -- 瓦片格式
	task_start_time varchar(50),
	task_end_time bigint
);

DROP TABLE IF EXISTS server_protocol_info;
CREATE TABLE server_protocol_info(
	protocol_id serial primary key,     -- 协议主键id
	protocol_name varchar(20) NOT NULL,          -- OGC 协议名称
	version_list varchar(50) NOT NULL,       -- 版本信息
	tile_grid_set varchar(200)  NOT NULL,   -- 切片策略名称
	support_format varchar(150)            -- 支持格式/切片格式
);
