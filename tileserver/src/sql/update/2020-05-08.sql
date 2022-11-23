/*增加表资源，分库分表策略修改后，需要缓存当前数据库下的表*/
drop table IF EXISTS table_source;
create table table_source(
	table_id serial primary key,      -- 自增id
    source_id integer NOT NULL REFERENCES dynamic_data_source(source_id) ON delete cascade,     -- 数据库名称
    table_name varchar(50) NOT NULL   -- 表名称
);

/*增加当前图层以及当前请求类型的存储方式*/
drop table IF EXISTS layer_store_info;
create table layer_store_info(
    source_name varchar(100) NOT NULL,   -- 数据库名称或者表名（psql和sqlite是数据库，mongodb是collection   tdb是文件夹名称）
    layer_name varchar(50) NOT NULL,     -- 图层名称
	store_type smallint NOT NULL,        -- 存储类型（1，postgresql，2 sqlite，3 mongodb）
	store_path varchar(200),            -- 存储路径，只有选择sqlite，tdb的存储方式时，开启该选项,
	tile_style varchar(50)               -- 切图格式
);
