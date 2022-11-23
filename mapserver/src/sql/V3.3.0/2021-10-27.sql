/*map_server_info新增是否展示字段*/
alter table public.map_server_info add column server_show smallint default 0;

/*model_server_info新增偏移高度*/
alter table public.model_server_info add column offset_height double precision;

/*权限表新增日志二级权限*/
INSERT INTO public.auth_info(id, auth_id, parent_auth_id, auth_name, auth_description, is_checked) VALUES (DEFAULT,1301,13,'系统日志','serverLog:systemLog',1);
INSERT INTO public.auth_info(id, auth_id, parent_auth_id, auth_name, auth_description, is_checked) VALUES (DEFAULT,1302,13,'操作日志','serverLog:opterateLog',1);
INSERT INTO public.auth_info(id, auth_id, parent_auth_id, auth_name, auth_description, is_checked) VALUES (DEFAULT,1303,13,'日志设置','serverLog:edit',0);
/*权限表新增关于我们权限项*/
INSERT INTO public.auth_info(id, auth_id, parent_auth_id, auth_name, auth_description, is_checked) VALUES (DEFAULT,126,14,'关于我们','aboutUs',1);

/*日志设置二级权限，系统管理员添加日志相关权限*/
INSERT INTO public.role_auth_relation(id,role_id,auth_id,auth_status) VALUES(DEFAULT,1,1301,1);
INSERT INTO public.role_auth_relation(id,role_id,auth_id,auth_status) VALUES(DEFAULT,1,1302,1);
INSERT INTO public.role_auth_relation(id,role_id,auth_id,auth_status) VALUES(DEFAULT,1,1303,1);
/*新增关于我们权限项，系统管理员添加关于我们权限项*/
INSERT INTO public.role_auth_relation(id,role_id,auth_id,auth_status) VALUES(DEFAULT,1,126,1);
/*数据管理员开放设置权限，保留数据库设置*/
INSERT INTO public.role_auth_relation(id,role_id,auth_id,auth_status) VALUES(DEFAULT,2,14,1);
/*数据管理员开放系统日志和操作日志查看权限*/
INSERT INTO public.role_auth_relation(id,role_id,auth_id,auth_status) VALUES(DEFAULT,2,1301,1);
INSERT INTO public.role_auth_relation(id,role_id,auth_id,auth_status) VALUES(DEFAULT,2,1302,1);
INSERT INTO public.role_auth_relation(id,role_id,auth_id,auth_status) VALUES(DEFAULT,2,126,1);