/*修改列参数，增加对应列*/
--alter table map_feature_info alter column native_crs to native_srs;
--alter table map_feature_info alter column resource_crs to resource_srs;
alter table map_feature_info add column resource_crs_wkt text;
alter table map_feature_info add column tile_origin_flag smallint default -1;


drop table IF EXISTS highway_toll_station;
create table highway_toll_station(
    id serial primary key,    -- 自增id
    name varchar(50),         -- 高速收费站
    tg_type character varying(1), -- 出口和入口标识 2 出口 3 入口
    geom geometry(Point)      -- 切图格式
);

-- 创建索引
CREATE INDEX highway_toll_station_geom_idx
  ON public.highway_toll_station
  USING gist
  (geom);

--初始化表
insert into highway_toll_station(name,tg_type,geom) select name,tg_type,geom from (select name,tg_type,geom from shannxi where kind = '230209')as t