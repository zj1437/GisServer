DROP TABLE IF EXISTS tile_version_support;

alter table tile_layer_info add column server_protocol varchar(50);
alter table tile_layer_info drop column support_grid;
update tile_layer_info set server_protocol = 'WMTS;TMS' where server_id in (1,2,3,4,5,6,7,8);

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
	support_grid varchar(20) ,         -- 投影坐标系
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
	support_format varchar(50)            -- 支持格式/切片格式
);

INSERT INTO public.server_protocol_info(protocol_id,protocol_name,version_list,tile_grid_set,support_format) VALUES(DEFAULT,'WMTS','1.0.0','EPSG:4326;EPSG:900913','png;jpeg;tiff;osgb;terrain;geojson;mapbox-vector');
INSERT INTO public.server_protocol_info(protocol_id,protocol_name,version_list,tile_grid_set,support_format) VALUES(DEFAULT,'WMS','1.0.0;1.3.0','EPSG:4326;EPSG:900913','png;jpeg;tiff');
INSERT INTO public.server_protocol_info(protocol_id,protocol_name,version_list,tile_grid_set,support_format) VALUES(DEFAULT,'WCS','1.0.0;1.1.0;1.1.1;1.1;2.0.1','EPSG:4326;EPSG:900913','png;jpeg;tiff');
INSERT INTO public.server_protocol_info(protocol_id,protocol_name,version_list,tile_grid_set,support_format) VALUES(DEFAULT,'WFS','1.0.0;1.1.0;2.0.0','EPSG:4326;EPSG:900913','geojson;gml;mapbox-vector');
INSERT INTO public.server_protocol_info(protocol_id,protocol_name,version_list,tile_grid_set,support_format) VALUES(DEFAULT,'TMS','1.0.0','EPSG:4326;EPSG:900913','png;jpeg;tiff;osgb;terrain;geojson;mapbox-vector');

