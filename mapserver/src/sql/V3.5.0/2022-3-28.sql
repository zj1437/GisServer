-- 创建功能页签表
CREATE TABLE public.function_page_info
(
  id serial primary key,
  function_name character varying(20),
  function_state integer,
  function_id integer,
  parent_function_id integer,
  function_alias character varying(20),
  function_index integer,
  function_route character varying(50),
  function_opensubmenue boolean
);
-- 功能页签表初始化
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(1,'数据服务',1,1,0,'dataService',1,'',true);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(2,'地图服务',1,2,1,'map',1,'/main/service/map',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(3,'模型服务',1,3,1,'model',2,'/main/service/model',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(4,'倾斜摄影服务',1,4,1,'slantphoto',3,'/main/service/slantphoto',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(5,'样式列表',1,5,1,'style',4,'/main/service/style',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(6,'切图服务',1,6,0,'cutService',2,'',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(7,'图层列表',1,7,6,'cutList',1,'/main/service/cutList',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(8,'切图任务',1,8,6,'cutTask',2,'/main/service/cutTask',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(9,'切图设置',1,9,6,'cutAutoSetting',3,'/main/service/cutAutoSetting',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(10,'分析服务',1,10,0,'anlysisService',3,'',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(11,'网络分析',1,11,10,'networkAnalysis',1,'/main/service/networkAnalysis',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(12,'地形分析',1,12,10,'terrainAnalysis',2,'/main/service/terrainAnalysis',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(13,'空间分析',1,13,10,'spaceAnlysis',3,'/main/service/spaceAnlysis',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(14,'专题服务',1,14,0,'subjectService',4,'',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(15,'态势服务',1,15,14,'situation',1,'/main/service/situation',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(16,'标号服务',1,16,14,'grade',2,'/main/service/grade',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(17,'协同服务',1,17,14,'synergismgrade',3,'/main/service/synergismgrade',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(18,'小工具',1,18,0,'littleTool',5,'/main/service/littleTool',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(19,'监控',1,19,-1,'monitor',1,'/main/monitor/efficient',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(20,'日志',1,20,-1,'log',2,'/main/log/systemLogList',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(21,'安全',1,21,-1,'security',3,'/main/security/users',false);
insert into function_page_info(id,function_name,function_state,function_id,parent_function_id,function_alias,function_index,function_route,function_opensubmenue) values(22,'设置',1,22,-1,'setting',4,'/main/setting/serviceAutoSetting',false);