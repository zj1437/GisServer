--更改数据库协议、剔除EPSG:900913
update public.server_protocol_info set tile_grid_set='EPSG:4326;EPSG:3857'  where protocol_name = 'WMTS';
update public.server_protocol_info set tile_grid_set='EPSG:4326;EPSG:3857'  where protocol_name = 'WMS';
update public.server_protocol_info set tile_grid_set='EPSG:4326;EPSG:3857'  where protocol_name = 'WCS';
update public.server_protocol_info set tile_grid_set='EPSG:4326;EPSG:3857'  where protocol_name = 'WFS';
update public.server_protocol_info set tile_grid_set='EPSG:4326;EPSG:3857'  where protocol_name = 'TMS';