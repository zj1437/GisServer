/*默认服务提供商*/
insert into server_provider_info(id,title_name,abstract_name,keywords,online_resource,contact_Organization,web_site
,contact_person,person_primary,contact_telephone,contact_fax,address,city,province,country,postcode,email_address) 
VALUES(1, '普祐科技', null, 'GIS,可视化,二三维一体化', 'www.puyougis.com','山西普祐科技有限公司','www.puyougis.com','王凯','营销总监',18551664926,'0351-7072111','山西省太原市晋源区环球金融中心B座3501',
'太原市','山西省','中国','030000','wangkai@puyougis.com');

INSERT INTO public.auto_tile_config(store_id, store_type, seed_type, store_path, default_srs, raster_format, dem_format, vector_format, tile_width_size, tile_height_size, tile_max_level, max_task_num, default_thread_num)
VALUES (1, 1, 'seed', '', '同源数据', 'image/jpeg', 'image/tiff', 'application/x-protobuf;type=mapbox-vector', 256, 256, 6, 3, 4);

INSERT INTO public.server_protocol_info(protocol_id,protocol_name,version_list,tile_grid_set,support_format) VALUES(DEFAULT,'WMTS','1.0.0','EPSG:4326;EPSG:3857','png;jpeg;tiff;osgb;terrain;geojson;mapbox-vector');
INSERT INTO public.server_protocol_info(protocol_id,protocol_name,version_list,tile_grid_set,support_format) VALUES(DEFAULT,'WMS','1.1.1;1.3.0','EPSG:4326;EPSG:3857','png;jpeg;tiff');
INSERT INTO public.server_protocol_info(protocol_id,protocol_name,version_list,tile_grid_set,support_format) VALUES(DEFAULT,'WCS','1.0.0;1.1.0;1.1.1;1.1;2.0.1','EPSG:4326;EPSG:3857','png;jpeg;tiff');
INSERT INTO public.server_protocol_info(protocol_id,protocol_name,version_list,tile_grid_set,support_format) VALUES(DEFAULT,'WFS','1.0.0;1.1.0;2.0.0','EPSG:4326;EPSG:3857','geojson;gml;mapbox-vector');
INSERT INTO public.server_protocol_info(protocol_id,protocol_name,version_list,tile_grid_set,support_format) VALUES(DEFAULT,'TMS','1.0.0','EPSG:4326;EPSG:3857','png;jpeg;tiff;osgb;terrain;geojson;mapbox-vector;heightarray');

INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level,meta_x, meta_y, gutter, tile_width, tile_height,server_protocol)
VALUES (DEFAULT, 1, 'google影像', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,4,4,0,256,256,'WMTS;TMS');

INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, meta_x, meta_y, gutter, tile_width, tile_height,server_protocol)
VALUES (DEFAULT, 2, 'google混合地图', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,4,4,0,256,256,'WMTS;TMS');

INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, meta_x, meta_y, gutter, tile_width, tile_height,server_protocol)
VALUES (DEFAULT, 3, 'google地图', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,4,4,0,256,256,'WMTS;TMS');

INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, meta_x, meta_y, gutter, tile_width, tile_height,server_protocol)
VALUES (DEFAULT, 4, '高德影像', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,4,4,0,256,256,'WMTS;TMS');

INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, meta_x, meta_y, gutter, tile_width, tile_height,server_protocol)
VALUES (DEFAULT, 5, '高德地图', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,4,4,0,256,256,'WMTS;TMS');


INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, meta_x, meta_y, gutter, tile_width, tile_height,server_protocol)
VALUES (DEFAULT, 6, 'MapBox地图', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,4,4,0,256,256,'WMTS;TMS');


INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, meta_x, meta_y, gutter, tile_width, tile_height,server_protocol)
VALUES (DEFAULT, 7, 'OpenStreetMap', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,4,4,0,256,256,'WMTS;TMS');

INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, meta_x, meta_y, gutter, tile_width, tile_height,server_protocol)
VALUES (DEFAULT, 8, '天地图', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,4,4,0,256,256,'WMTS;TMS');





