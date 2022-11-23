/*删除图层无用的信息*/
alter table tile_layer_info drop column meta_x;
alter table tile_layer_info drop column meta_y;
alter table tile_layer_info drop column gutter;
alter table tile_layer_info drop column tile_width;
alter table tile_layer_info drop column tile_height;
alter table tile_layer_info drop column support_grid;
/*新增样式文件的url*/
alter table tile_layer_info add column style_url varchar(200);


