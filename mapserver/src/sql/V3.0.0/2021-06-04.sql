/*切图任务表添加任务类型字段*/
alter table tile_task_entity_info add column task_type int;
/*切图任务表添加任务类型字段*/
alter table tile_task_entity_info add column level_range varchar(20);

alter table auto_tile_config add column export_path varchar(200);