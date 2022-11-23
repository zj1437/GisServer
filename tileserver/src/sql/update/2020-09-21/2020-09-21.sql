/*增加瓦片数据的配置信息*/
drop table IF EXISTS tile_meta_info;
create table tile_meta_info(
	meta_x smallint,    -- 瓦片x方向的元数据
    meta_y smallint,    -- 瓦片x方向的元数据
    gutter smallint,    -- 锯齿大小
    tile_width integer, -- 瓦片宽度
    tile_height integer -- 瓦片高度
);

/*目前这个表暂时配置初始化,后期建议由专门配置页面来处理*/
insert into tile_meta_info (meta_x,meta_y,gutter,tile_width,tile_height)VALUES(4,4,0,256,256);

/*删除tile_task_info和tile_layer_info中的相关字段*/
alter table tile_task_info drop column meta_x;
alter table tile_task_info drop column meta_y;
alter table tile_task_info drop column gutter;

alter table tile_layer_info drop column meta_x;
alter table tile_layer_info drop column meta_y;
alter table tile_layer_info drop column gutter;
alter table tile_layer_info drop column tile_width;
alter table tile_layer_info drop column tile_height;

/*切图瓦片协议，直接初始化*/
drop table if EXISTS tile_version_support;

/*直接使用py_data中的额server_provider_info*/
drop table if EXISTS server_provider_info;


