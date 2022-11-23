/*角色表*/
DROP TABLE IF EXISTS role_info;
CREATE TABLE role_info(
	role_id serial primary key,       -- 角色id
	role_name varchar(20) NOT NULL,   -- 角色名称
	role_creator varchar(20) NOT NULL, -- 角色创建人
	gen_time timestamp NOT NULL,      -- 创建日期
	update_time timestamp NOT NULL,   -- 角色更新时间
	desciption varchar(200),           -- 角色描述
	is_system smallint default 1       -- 是否是系统默认角色
);

/*初始化角色表*/
INSERT INTO public.role_info(role_id,role_name,role_creator,gen_time,update_time,desciption) VALUES (DEFAULT,'系统管理员','system',now(),now(),'内置的系统管理员，此角色默认拥有整个PYServer的管理权限，可以登录服务执行对服务、监控、安全、日志、系统等配置，拥有对整个服务器的管理权限，需要慎重使用');
INSERT INTO public.role_info(role_id,role_name,role_creator,gen_time,update_time,desciption) VALUES (DEFAULT,'数据管理员','system',now(),now(),'内置的服务发布者，此角色默认拥有服务发布和服务实例管理的权限，如发布、启用、停用、删除服务实例及切图服务的所有操作等，主要用于管理系统的数据与服务');
INSERT INTO public.role_info(role_id,role_name,role_creator,gen_time,update_time,desciption) VALUES (DEFAULT,'普通用户','system',now(),now(),'内置的PYServer查看与使用者角色，具有查看PYServer发布的所有服务的权限，包括地图、模型、倾斜摄影、POI等，但其无法自行发布服务');
