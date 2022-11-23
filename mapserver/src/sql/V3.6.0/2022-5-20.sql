--修改map_feature_info表中tile_extension字段长度
ALTER TABLE public.map_feature_info ALTER COLUMN tile_extension TYPE character varying(40);



--新增服务支持的投影坐标系表
DROP TABLE IF EXISTS coordinate_info;
CREATE TABLE coordinate_info(
	epsg_id serial primary key,  -- 投影坐标id，主键id
	epsg_proj varchar(20) NOT NULL,
	resource_crs text NOT NULL
);

--初始化coordinate_info表数据
INSERT INTO public.coordinate_info(epsg_id, epsg_proj, resource_crs) VALUES (DEFAULT,'4326', 'GEOGCS["WGS 84", DATUM["World Geodetic System 1984", SPHEROID["WGS 84", 6378137.0, 298.257223563, AUTHORITY["EPSG","7030"]], AUTHORITY["EPSG","6326"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4326"]]');
INSERT INTO public.coordinate_info(epsg_id, epsg_proj, resource_crs) VALUES (DEFAULT,'3857', 'PROJCS["WGS 84 / Pseudo-Mercator", GEOGCS["WGS 84", DATUM["World Geodetic System 1984", SPHEROID["WGS 84", 6378137.0, 298.257223563, AUTHORITY["EPSG","7030"]], AUTHORITY["EPSG","6326"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4326"]], PROJECTION["Popular Visualisation Pseudo Mercator", AUTHORITY["EPSG","1024"]], PARAMETER["semi_minor", 6378137.0], PARAMETER["latitude_of_origin", 0.0], PARAMETER["central_meridian", 0.0], PARAMETER["scale_factor", 1.0], PARAMETER["false_easting", 0.0], PARAMETER["false_northing", 0.0], UNIT["m", 1.0], AXIS["Easting", EAST], AXIS["Northing", NORTH], AUTHORITY["EPSG","3857"]]');
INSERT INTO public.coordinate_info(epsg_id, epsg_proj, resource_crs) VALUES (DEFAULT,'4524', 'PROJCS["CGCS2000 / 3-degree Gauss-Kruger zone 36", GEOGCS["China Geodetic Coordinate System 2000", DATUM["China 2000", SPHEROID["CGCS2000", 6378137.0, 298.257222101, AUTHORITY["EPSG","1024"]], AUTHORITY["EPSG","1043"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4490"]], PROJECTION["Transverse_Mercator", AUTHORITY["EPSG","9807"]], PARAMETER["central_meridian", 108.0], PARAMETER["latitude_of_origin", 0.0], PARAMETER["scale_factor", 1.0], PARAMETER["false_easting", 36500000.0], PARAMETER["false_northing", 0.0], UNIT["m", 1.0], AXIS["Easting", EAST], AXIS["Northing", NORTH], AUTHORITY["EPSG","4524"]]');
INSERT INTO public.coordinate_info(epsg_id, epsg_proj, resource_crs) VALUES (DEFAULT,'4490', 'GEOGCS["China Geodetic Coordinate System 2000", DATUM["China 2000", SPHEROID["CGCS2000", 6378137.0, 298.257222101, AUTHORITY["EPSG","1024"]], AUTHORITY["EPSG","1043"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4490"]]');
INSERT INTO public.coordinate_info(epsg_id, epsg_proj, resource_crs) VALUES (DEFAULT,'4610', 'GEOGCS["Xian 1980", DATUM["Xian 1980", SPHEROID["IAG 1975", 6378140.0, 298.257, AUTHORITY["EPSG","7049"]], AUTHORITY["EPSG","6610"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4610"]]');
INSERT INTO public.coordinate_info(epsg_id, epsg_proj, resource_crs) VALUES (DEFAULT,'4214', 'GEOGCS["Beijing 1954", DATUM["Beijing 1954", SPHEROID["Krassowsky 1940", 6378245.0, 298.3, AUTHORITY["EPSG","7024"]], TOWGS84[15.8, -154.4, -82.3, 0.0, 0.0, 0.0, 0.0], AUTHORITY["EPSG","6214"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4214"]]');

--删除特征样式表中的高斯属性
ALTER TABLE public.map_feature_info DROP COLUMN guass_tile_size;
ALTER TABLE public.map_feature_info DROP COLUMN guass_coordinate_bbox;

--空间分析页面导航栏适配
update function_page_info set function_name = '空间分析' , function_alias = 'spaceAnalysis' where id = 10;
update function_page_info set function_name = '几何关系' , function_alias = 'geometricRelation', function_route = '/main/service/geometricRelation'  where id = 13;
insert into function_page_info (id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values (23,'基础量算',1,23,10,'basicCalculation',4,'/main/service/basicCalculation',FALSE);
insert into function_page_info (id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values (24,'叠加分析',1,24,10,'overlayAnalysis',5,'/main/service/overlayAnalysis',FALSE);
insert into function_page_info (id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values (25,'缓冲区分析',1,25,10,'bufferAnalysis',6,'/main/service/bufferAnalysis',FALSE);
insert into function_page_info (id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values (26,'密度分析',1,26,10,'densityAnalysis',7,'/main/service/densityAnalysis',FALSE);

--修改版本信息表中WFS协议支持的格式
update server_protocol_info set support_format = 'geojson;gml' where protocol_id = 4