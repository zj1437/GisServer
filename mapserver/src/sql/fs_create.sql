/*地图数据服务*/
DROP TABLE IF EXISTS map_server_info;
CREATE TABLE map_server_info(
	server_id serial primary key,  		-- 服务id,主键id
	server_name varchar(50) NOT NULL, 	-- 服务名称
	layer_name varchar(50) NOT NULL,  	-- 图层名称
	file_type varchar(20) NOT NULL, 	-- 文件类型
	server_status SMALLINT NOT NULL, 	-- 服务状态0 暂停中 1 正常 2 异常
	publishor varchar(50) NOT NULL DEFAULT 'admin', -- 发布人
	publish_time timestamp NOT NULL,      	-- 发布日期
	update_time timestamp NOT NULL,     	-- 更新时间
	data_type SMALLINT NOT NULL,     		--  数据类型  0，矢量数据 1，栅格数据，2，瓦片数据(texx和demx)
	file_url varchar(500) NOT NULL, 		-- 服务数据存储位置
	description varchar(200),        		-- 服务描述
	open_tiled SMALLINT DEFAULT 0,   		-- 是否开启切图  默认为0 不开启   1是开启
	encode_name varchar(10),             	-- 编码格式
	is_public SMALLINT DEFAULT 1,         	-- 服务是否公开，0是不公开  1是公开
	server_token varchar(40),               -- 私有服务的token访问
	is_layer_group smallint NOT NULL DEFAULT 0, -- 是否为图层组，0 不是 1是
	pulished_proctol varchar(40),               -- 当前已发布的协议
	server_show smallint default 0,             --批量聚合图层，只显示已有图层
	fail_reson varchar(200),                    -- 失败原因
	over_view smallint DEFAULT 0                --是否开启影像金字塔
);

DROP TABLE IF EXISTS server_protocol_info;
CREATE TABLE server_protocol_info(
	protocol_id serial primary key,     -- 协议主键id
	protocol_name varchar(20) NOT NULL,          -- OGC 协议名称
	version_list varchar(50) NOT NULL,       -- 版本信息
	tile_grid_set varchar(200)  NOT NULL,   -- 切片策略名称
	support_format varchar(150)            -- 支持格式/切片格式
);
	
/*服务特征信息数据*/
DROP TABLE IF EXISTS map_feature_info;
CREATE TABLE map_feature_info(
	feature_id serial primary key,  -- 服务id,主键id
	server_id integer NOT NULL REFERENCES map_server_info(server_id) ON delete cascade,
	resource_crs varchar(50) NOT NULL, -- 源数据投影坐标
	native_crs varchar(50) NOT NULL,   -- 本地坐标系统
	projection_policy varchar(100) NOT NULL, -- 投影策略
	native_bbox varchar(200) NOT NULL, -- 本地边界值
	resource_bbox varchar(200) NOT NULL, -- 元数据边界值
	data_format smallint,          	-- 当前矢量数据类型  1 栅格  2高程  3点 4线  5面
	feature_size int DEFAULT 0,    	-- 特征数据的信息总量
	zoom_level integer,         	-- 瓦片最大级别
	tile_width int,          		-- 瓦片宽度
	tile_height int,          		-- 瓦片高度
	tile_extension varchar(40),   	-- 瓦片数据扩展名
	sea_chart_settings text,         -- 海图还配json
	sea_chart_identifier varchar(20),      -- 海图服务标识符
	tile_origin_flag smallint default -1,  -- 瓦片起始方向  0 是坐下角（标准tms） 1 左上角（wmts）
	resource_crs_wkt text,
	sea_chart_scale text             -- 海图比例尺
);

/*模型服务信息表*/
DROP TABLE IF EXISTS model_server_info;
CREATE TABLE model_server_info(
	id serial primary key,  -- 主键id
	server_name varchar(50) NOT NULL,  -- 服务名称
	status SMALLINT NOT NULL, -- 模型服务当前状态 0、暂停中 1、已发布
	product_num integer,      -- 服务产品数量
	publish_time timestamp NOT NULL,      -- 发布日期
	update_time timestamp NOT NULL, -- 更新时间（即模型最后一次被修改时间）
	publishor varchar(50) NOT NULL DEFAULT 'admin', -- 服务发布者
	file_url varchar(200),           -- 文件url，只有倾斜射影数据用到此字段
	product_name varchar(50),       -- 倾斜射影服务的数据名称
	description varchar(200),        -- 服务描述
	lon_position double precision,       -- 倾斜射影位置信息
	lat_position double precision,       -- 倾斜射影位置信息
	hei_position double precision,       -- 倾斜射影位置信息
	server_type SMALLINT NOT NULL,    -- 服务类型  0是模型  1是倾斜射影
	is_public SMALLINT DEFAULT 1,
	offset_height double precision      --偏移高度
);
	
/*模型数据信息表*/
DROP TABLE IF EXISTS model_product_info;
CREATE TABLE model_product_info(
	product_id serial primary key,  -- 主键id
	server_id integer NOT NULL REFERENCES model_server_info(id) ON delete cascade, -- 服务名称
	product_name varchar(20) NOT NULL, -- 产品名称
	file_type varchar(5) NOT NULL,    -- 文件类型
	file_url varchar(200) NOT NULL, -- 文件存储url
	is_directory SMALLINT NOT NULL  -- 当前文件是文件夹还是文件
);
	
/*用户信息表*/
DROP TABLE IF EXISTS user_info;
CREATE TABLE user_info(
	user_id serial primary key,      -- 用户id
	user_name varchar(20) NOT NULL,  -- 用户名
	user_pass varchar(50) NOT NULL,  -- 密码
	phone_num varchar(20),           -- 用户电话
	email varchar(64),               -- 用户邮箱
	description varchar(200),        -- 用户描述
	gen_time timestamp NOT NULL,     -- 创建日期
	login_time timestamp,            -- 登录时间
	last_login_time timestamp,        -- 上次登录时间
	is_system smallint NOT NULL DEFAULT 1,  -- 默认是否为系统创建用户
	is_lock smallint NOT NULL DEFAULT 1,    -- 账户是否被锁0 锁住  1 未锁
	use_memory double precision DEFAULT 500.0             -- 用户分配的资源空间
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
	
	/*权限表*/
	DROP TABLE IF EXISTS auth_info;
	CREATE TABLE auth_info(
	id serial primary key,                -- 自增id，主键
	auth_id varchar(10) NOT NULL,         -- 权限id
	parent_auth_id varchar(10) NOT NULL,  -- 父权限id
	auth_name varchar(20) NOT NULL,       -- 权限名称
	auth_description varchar(100),        -- 权限描述
	is_checked smallint NOT NULL DEFAULT 0   -- 设置当前权限是否被选中0、为选中、1、是选中
	);
	
	/*用户组表*/
	DROP TABLE IF EXISTS user_group_info;
	CREATE TABLE user_group_info(
	group_id serial primary key,         -- 用户组id,主键自增
	group_name varchar(20) NOT NULL,     -- 用户组名称
	parent_group_id integer NOT NULL,    -- 父组id，当前无父组时，默认为-1
	gen_time timestamp NOT NULL,         -- 组创建时间
	group_description varchar(100) NOT NULL -- 当前组信息描述
	);
	
	/*角色权限关系表*/
	DROP TABLE IF EXISTS role_auth_relation;
	CREATE TABLE role_auth_relation(
	id serial primary key,       -- 角色权限表的自增id
	role_id integer NOT NULL,    -- 角色id
	auth_id varchar(10) NOT NULL, -- 权限id
	auth_status integer NOT NULL -- 权限状态表，0表示可访问，1表示可授权
	);
	
	/*组权限关系表*/
	DROP TABLE IF EXISTS group_auth_relation;
	CREATE TABLE group_auth_relation(
	id serial primary key,      -- 组权限关系表id
	group_id integer NOT NULL,  -- 组id
	auth_id varchar(10) NOT NULL, -- 权限id
	auth_status smallint NOT NULL -- 权限状态表，0表示可访问，1表示可授权
	);
	
	/*用户角色表*/
	DROP TABLE IF EXISTS user_role_info;
	CREATE TABLE user_role_info(
	id serial primary key, -- 用户权限表自增id
	user_id integer NOT NULL,  -- 用户id
	role_id integer NOT NULL  -- 角色id
	);
	
	/*组角色表*/
	DROP TABLE IF EXISTS group_role_info;
	CREATE TABLE group_role_info(
	id serial PRIMARY KEY,          -- 组角色表自增id
	role_id integer NOT NULL,  -- 角色id
	group_id integer NOT NULL  -- 组id
	);
	
	/*用户权限表*/
	DROP TABLE IF EXISTS user_auth_info;
	CREATE TABLE user_auth_info(
	id serial PRIMARY KEY,          -- 用户权限自增表
	user_id integer NOT NULL,   -- 用户id
	auth_id varchar(10) NOT NULL -- 权限id
	);
	
	/*用户组关系表*/
	DROP TABLE IF EXISTS user_group_relation_info;
	CREATE TABLE user_group_relation_info(
	id serial primary key,          -- 用户组表自增id
	user_id integer NOT NULL ,   -- 用户id
	group_id integer NOT NULL
	);
	
	/*操作日志表*/
	DROP TABLE IF EXISTS operation_log;
	CREATE TABLE operation_log(
	log_id serial primary key,         -- 日志id
	op_content varchar(200) NOT NULL,  -- 操作内容
	operator varchar(20) NOT NULL,     -- 操作人
	req_ip varchar(20),                -- 当前操作IP
	op_time timestamp NOT NULL         -- 操作时间
	);
	
	/*系统配置*/
	DROP TABLE IF EXISTS system_config_info;
	CREATE TABLE system_config_info(
	id serial primary key,
	system_username varchar(50),   -- 系统用户名
	system_password varchar(50),   -- 系统密码
	file_server varchar(100),       -- 文件服务器
	file_upload_directory varchar(100), -- 文件上传路径
	sys_time text,                    -- 系统时间
	style_path varchar(200)           --样式文件路径
	);
	
	/*服务请求统计*/
	DROP TABLE IF EXISTS server_request_statistic;
	CREATE TABLE server_request_statistic(
		id serial primary key,            -- 访问id
		server_type smallint NOT NULL,    -- 服务类型   0是地图服务，  1是模型服务
		server_name varchar(50) NOT NULL, -- 服务名称
		protocol_name varchar(10),        -- ogc协议名称
		product_name varchar(50),         -- 主要针对模型数据服务
		reponse_time integer NOT NULL,    -- 响应时间  单位是ms
		req_start_time timestamp NOT NULL,  -- 请求开始时间
		req_end_time timestamp NOT NULL,    -- 请求结束时间
		req_ip varchar(20),               -- 访问ip
		server_id integer not null        --服务ID
	);
	
	/*异常服务统计表*/
	DROP TABLE IF EXISTS server_abnormal_statistic;
	CREATE TABLE server_abnormal_statistic(
	id serial primary key,            -- 访问id
	server_type smallint NOT NULL,    -- 服务类型   0是地图服务，  1是模型服务
	server_name varchar(50) NOT NULL, -- 服务名称
	abnormal_time timestamp NOT NULL, -- 异常时间
	abnormal_reason varchar(200)     -- 异常原因
	);
	
	DROP TABLE IF EXISTS sys_resource_info;
	CREATE TABLE sys_resource_info(
	id serial primary key,            -- 系统id
	cpu_rate double precision NOT NULL, --cpu使用率
	memory_total_size double precision NOT NULL, -- 系统总内存
	memory_use_size double precision NOT NULL,   -- 系统使用内存
	memory_use_rate double precision NOT NULL,    -- 内存使用率
	record_time timestamp NOT NULL               -- 记录时间
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
	
	DROP TABLE IF EXISTS province;
	CREATE TABLE province (
	  id varchar(15) NOT NULL,
	  code_p varchar(15) NOT NULL,
	  name varchar(20) NOT NULL,
	  PRIMARY KEY (code_p)
	);
	
	DROP TABLE IF EXISTS city;
	CREATE TABLE city (
	  id varchar(15) NOT NULL,
	  code_c varchar(15) NOT NULL,
	  name varchar(20) NOT NULL,
	  code_p varchar(15) NOT NULL,
	--  geom geometry(MultiPolygon), -- web端统计已经移植到统计项目中
	  PRIMARY KEY (code_c),
	  CONSTRAINT FK_Reference_10 FOREIGN KEY (code_p) REFERENCES province (code_p)
	);
	
	/* 目前县域信息用不上
	DROP TABLE IF EXISTS area;
	CREATE TABLE area (
	  id varchar(15) NOT NULL,
	  code_a varchar(15) NOT NULL,
	  name varchar(20) NOT NULL,
	  code_c varchar(15) DEFAULT NULL,
	  PRIMARY KEY (code_a),
	  CONSTRAINT FK_Reference_10 FOREIGN KEY (code_c) REFERENCES city (code_c)
	);
	*/
	DROP TABLE IF EXISTS error_login;
	CREATE TABLE error_login(
	id serial primary key,
	user_name varchar(64) NOT NULL, 
	login_time timestamp NOT NULL
	);
	
	/*样式记录信息表*/
	DROP TABLE IF EXISTS style_info;
	CREATE TABLE style_info(
	    id serial primary key,             -- 自增id
		style_id varchar(50),              -- style_id
	    style_name varchar(50) NOT NULL,   -- 样式名称
	    file_name varchar(50) NOT NULL,    -- 文件名称
	    url varchar(200) NOT NULL,        -- url
	    type_id integer NOT NULL,         -- 类型id,
	    is_color smallint NOT NULL DEFAULT 0, -- 是否需要染色，默认0 不需要
	    is_default smallint NOT NULL DEFAULT 0,  --是否默认样式，默认样式不可以删除
	    upload_time timestamp NOT NULL,
	    upload_person varchar(20) NOT NULL DEFAULT 'admin'
	);
	
	/*服务样式*/
	DROP TABLE IF EXISTS server_style_info;
	CREATE TABLE server_style_info(
	    id serial primary key, -- 自增id
	    style_id integer,      -- 样式id
	    server_id integer REFERENCES map_server_info(server_id) ON delete cascade  --服务id 
	);
	
	/*染色图色带*/
	DROP TABLE IF EXISTS color_map;
	CREATE TABLE color_map(
	    color_map_id serial primary key, -- 色带id
	    color_map_name varchar(20) NOT NULL, -- 色带名称
	    color_map_url varchar(200) NOT NULL -- 色带url
	);
	
	/*染色图色带*/
	DROP TABLE IF EXISTS server_color_map;
	CREATE TABLE server_color_map(
	    id serial primary key, -- 自增id
	    server_id integer unique REFERENCES map_server_info(server_id) ON delete cascade,  --服务id
	    color_map_id integer NOT NULL -- 色带id
	);
	
	/*文件上传信息*/
	drop table IF EXISTS upload_file_info;
	create table upload_file_info(
	    upload_id serial primary key,
	    file_name varchar(30) NOT NULL,
	    upload_time timestamp NOT NULL,
	    update_time timestamp NOT NULL,
	    upload_user varchar(20) NOT NULL,
	    file_type smallint NOT NULL, -- 文件类型，具体是文件还是文件夹
	    file_url varchar(200), -- 文件件存储对应的url 
	    file_size double precision
	);
	
	/*自动服务发布设置*/
	drop table IF EXISTS auto_server_config;
	create table auto_server_config(
	    id serial primary key,
	    is_open smallint NOT NULL DEFAULT 0,  -- 是否开启
		monitor_path varchar(100) NOT NULL    -- 监测路径
	);
	
	/*监测数据格式*/
	drop table IF EXISTS monitor_data_format;
	create table monitor_data_format(
	    data_format varchar(20) NOT NULL,  -- 数据格式
		server_format varchar(100) NOT NULL,    -- 服务类型
	    is_tiled smallint NOT NULL DEFAULT 0  -- 投影方式
	);
	
	/*资源表，主要增加资源id和资源位置的字典表*/
	DROP TABLE IF EXISTS download_resource;
	CREATE TABLE download_resource(
	    resource_id serial primary key,   -- 资源id
	    resource_name varchar(10) NOT NULL,  -- 资源名称
	    resource_path varchar(100) NOT NULL  -- 资源路径
	);
	
	/*创建图层组信息表*/
	DROP TABLE IF EXISTS layer_group_info;
	CREATE TABLE layer_group_info(
		id serial primary key,
		group_id int NOT NULL REFERENCES map_server_info(server_id) ON delete cascade,  -- 图层组id
		layer_Name varchar(20) NOT NULL,  -- 图层名称
		remote_url varchar(500),          -- 远程服务url
		server_type varchar(10) DEFAULT 'local',  -- 服务类型  默认为本地
		ogc_protocal varchar(10),         -- OGC协议
		style_name varchar(20) NOT NULL,  -- 样式名称
		min_level int NOT NULL DEFAULT 0,  -- 最小级别
		max_level int NOT NULL             -- 最大级别
	);
	
	
/*PostGis数据库信息*/
drop table IF EXISTS post_gis_info;
create table post_gis_info(
    db_id serial primary key,
    data_source_name varchar(50) NOT NULL,      -- 数据源名称
    database_name varchar(50) NOT NULL,    -- 数据库名称
    database_ip varchar(30) NOT NULL,     -- 数据库IP
    port int NOT NULL,                    --数据库端口
    user_name varchar(20) NOT NULL,          -- 数据库用户名
    password varchar(50) NOT NULL,   -- 数据库密码
    publisher varchar(50) NOT NULL DEFAULT 'admin', -- 发布人
    db_type varchar(20) NOT NULL,          -- 数据库类型
    publish_time timestamp,      -- 发布日期
    update_time timestamp,     -- 更新时间
    db_status int NOT NULL   -- 数据库状态
);

/*PostGis数据与Server表的关联表*/
drop table IF EXISTS postgis_server_relation_info;
create table postgis_server_relation_info(
    id serial primary key,        -- 序列号
    server_id integer REFERENCES map_server_info(server_id) ON delete cascade, --服务id
    db_id integer REFERENCES post_gis_info(db_id) ON delete cascade      --数据源id
);


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


/*新增服务访问性能监控表*/
drop table if exists server_visit_statistic;
CREATE TABLE server_visit_statistic(
	id serial,           -- 自增id
	server_name varchar(50) NOT NULL, -- 服务名称
	max_reponse_time double precision not null,  -- 最大响应时间
	min_reponse_time double precision not null,  -- 最小响应时间
	ave_reponse_time double precision not null,  -- 平均响应时间
	max_concurrent integer not null,   -- 最大并发量
	visit_num integer not null,   -- 访问总量
	server_id integer not null primary key  --服务ID
);

--------------------------------------------------------------------------------------------------------------
/*将原tile_data数据库中表，优化到fs_data数据库，剔除tile_data数据库*/
/*切图任务表详情表*/
drop table IF EXISTS tile_task_info;
create table tile_task_info(
    task_id integer NOT NULL REFERENCES map_server_info(server_id) ON delete cascade,
    task_name varchar(50) NOT NULL UNIQUE,  -- 任务名称（由图层名称+投影+格式+大小组成） 
    layer_name varchar(50) NOT NULL,        -- 图层名称
    store_type smallint NOT NULL,           -- 存储类型（1，postgresql，2 sqlite，3 mongodb）
    store_path varchar(200),                 -- 存储路径，只有选择sqlite的存储方式时，开启该选项
    srs_style varchar(20) NOT NULL,         -- 投影格式
    tile_style varchar(50),       			 -- 切图格式
    seed_type varchar(10) NOT NULL,         -- 切图类型
    start_seed_time timestamp,     -- 切图开始时间
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
	source_id serial primary key,        -- 自增id
    source_key varchar(200) NOT NULL,    -- 数据库名称
    layer_name varchar(50) NOT NULL,     -- 图层名称
    user_name varchar(50) default '',    -- 用户名
    password varchar(50) default '',     -- 密码
    driver_class varchar(50),   		 -- 驱动类
    jdbc_Url varchar(200)       		 -- jdbc url
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
	tile_size integer,					 -- 瓦片大小
	grid_set varchar(20),                -- 投影信息
	tile_style varchar(50)               -- 切图格式
);

DROP TABLE IF EXISTS tile_task_entity_info;
CREATE TABLE tile_task_entity_info(
	task_id integer NOT NULL REFERENCES map_server_info(server_id) ON delete cascade,              -- 主键id
	task_name varchar(50) NOT NULL,          -- 任务名称
	layer_name varchar(50) NOT NULL,         -- 图层名称
	task_status int NOT NULL,                -- 任务状态
	progress varchar(50)  NOT NULL,          -- 任务进度
	tiled_tiles integer,					 -- 完成瓦片数
	suspend_tiles integer default 0,         -- 记录暂停前的切图总数
	total_tiles integer,					 -- 总瓦片数
	spent_time_str varchar(50) NOT NULL,     -- 切图使用时间
	spent_time bigint,     					 -- 切图使用时间
	suspend_spent_time bigint default 0,     -- 暂停时已使用时间
	time_remaining_str varchar(50) ,         -- 切图剩余时间
	task_start_time varchar(50),			 -- 任务开始时间
	task_end_time bigint,					 -- 结束时间（用于排序）
	task_type int,                            -- 任务类型，4是导出任务，-1即切图任务
	level_range varchar(20),                  --切图级别，格式为start-end
	server_show smallint default 0            --服务是否在页面展示
);	

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
    default_thread_num integer NOT NULL DEFAULT 5,       -- 任务最大的线程数
    export_path varchar(200)                             --导出任务路径
);

/*系统缓存表*/
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



/**
 * 记录全球航班飞行记录信息
 */
drop table if EXISTS flight_record_info_world;
create table flight_record_info_world(
    id serial primary key,                   --id
    flight_number varchar(20) NOT NULL,      --航班号
    latitude double precision NOT NULL,      --纬度
    longitude double precision NOT NULL,     --经度
    geom geometry(Point),                    --保存航班地理位置信息
    flight_timestamp timestamp,              --时间戳，记录某个时间点航班飞行信息
    flying_height double precision,          --飞行高度，单位是英尺
    flight_model varchar(20),                --飞机机型
    no_use_data double precision,            --当前无法识别数据-1
    azimuth double precision,                --方位角
    managerment_type varchar(20),            --管理类型，ADSB或其他
    register_number varchar(20),             --飞机注册号码
    starting_place varchar(20),              --航班始发地
    destination_place varchar(20),           --航班目的地
    airline_belongs varchar(20),             --所属航空公司
    other_info double precision              --当前无法识别数据-2
);
	
DROP TABLE IF EXISTS sea_chart_feature;
CREATE TABLE sea_chart_feature(
	code_no varchar(10) NOT NULL,  		-- 编码
	object_name varchar(50) NOT NULL, 	-- 大类名称
	object_name_acronym varchar(20) NOT NULL,  	-- 大类缩写
	attribute_A varchar(200), 	-- 属性A
	attribute_B varchar(200), 	-- 属性B
	attribute_C varchar(200), -- 属性C
	class varchar(4),      	-- 类型
	primitives varchar(50),  -- 基元
	is_standard smallint,    -- 是否是标准模式   0 不显示  1 显示
	is_base smallint    -- 是否是基础模式   0 不显示  1 显示
);

-- 创建功能页签表
drop table if exists function_page_info;
CREATE TABLE function_page_info
(
  id serial primary key,
  function_name character varying(20),
  function_state integer,
  function_id integer,
  parent_function_id integer,
  function_alias character varying(20),
  function_index integer,
  function_route character varying(50),
  function_opensubmenue boolean
);

--新增服务支持的投影坐标系表
DROP TABLE IF EXISTS coordinate_info;
CREATE TABLE coordinate_info(
	epsg_id serial primary key,  -- 投影坐标id，主键id
	epsg_proj varchar(20) NOT NULL,
	resource_crs text NOT NULL
);
