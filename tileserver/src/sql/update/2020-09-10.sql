INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, support_grid, meta_x, meta_y, gutter, tile_width, tile_height)
VALUES (DEFAULT, 1, 'google影像', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,'EPSG:4326;EPSG:900913',4,4,0,256,256);
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 1, 'wmts', '1.0.0', 'png;jpeg');
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 1, 'tms', '1.0.0', 'png;jpeg');

INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, support_grid, meta_x, meta_y, gutter, tile_width, tile_height)
VALUES (DEFAULT, 2, 'google混合地图', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,'EPSG:4326;EPSG:900913',4,4,0,256,256);
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 2, 'wmts', '1.0.0', 'png;jpeg');
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 2, 'tms', '1.0.0', 'png;jpeg');

INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, support_grid, meta_x, meta_y, gutter, tile_width, tile_height)
VALUES (DEFAULT, 3, 'google地图', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,'EPSG:4326;EPSG:900913',4,4,0,256,256);
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 3, 'wmts', '1.0.0', 'png;jpeg');
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 3, 'tms', '1.0.0', 'png;jpeg');

INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, support_grid, meta_x, meta_y, gutter, tile_width, tile_height)
VALUES (DEFAULT, 4, '高德影像', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,'EPSG:4326;EPSG:900913',4,4,0,256,256);
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 4, 'wmts', '1.0.0', 'png;jpeg');
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 4, 'tms', '1.0.0', 'png;jpeg');

INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, support_grid, meta_x, meta_y, gutter, tile_width, tile_height)
VALUES (DEFAULT, 5, '高德地图', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,'EPSG:4326;EPSG:900913',4,4,0,256,256);
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 5, 'wmts', '1.0.0', 'png;jpeg');
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 5, 'tms', '1.0.0', 'png;jpeg');


INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, support_grid, meta_x, meta_y, gutter, tile_width, tile_height)
VALUES (DEFAULT, 6, 'MapBox地图', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,'EPSG:4326;EPSG:900913',4,4,0,256,256);
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 6, 'wmts', '1.0.0', 'png;jpeg');
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 6, 'tms', '1.0.0', 'png;jpeg');


INSERT INTO public.tile_layer_info(tile_id, server_id, layer_name, is_enable, layer_status, file_type,data_style, 
layer_style, native_crs, native_bounding_box, max_zoom_level, support_grid, meta_x, meta_y, gutter, tile_width, tile_height)
VALUES (DEFAULT, 7, 'OpenStreetMap', 2, -1, 'onine', 6,'栅格','EPSG:4326','-180.0;-90.0;180.0;90.0',30,'EPSG:4326;EPSG:900913',4,4,0,256,256);
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 7, 'wmts', '1.0.0', 'png;jpeg');
INSERT INTO public.tile_version_support(version_id, tile_id, protocal_name, version_list, support_format)
VALUES (DEFAULT, 7, 'tms', '1.0.0', 'png;jpeg');

alter table tile_layer_info add column file_url varchar(200);
alter table tile_layer_info add column resource_srs varchar(20);
alter table tile_layer_info add column resource_bbox varchar(100);

