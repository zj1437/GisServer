DROP TABLE IF EXISTS map_version_info;
DROP TABLE IF EXISTS server_version_info;

update map_server_info set pulished_proctol = 'WMTS;TMS' where server_id in (1,2,3,4,5,6,7,8);
update map_server_info set file_url = 'http://a.tiles.mapbox.com/v4/mapbox.streets/%s/%s/%s.png?access_token=pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpejY4M29iazA2Z2gycXA4N2pmbDZmangifQ.-g_vE53SD2WrJ6tFX7QHmA' where server_name='MapBox地图';

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