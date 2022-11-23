--删除feature_info表中的resource_crs_wkt字段
ALTER TABLE public.map_feature_info DROP COLUMN resource_crs_wkt;

--修改chinapoi中表格'恒歌科技(西安)'的geom值--修改poi检索结果位置不对的问题
update chinapoi set geom = '010100000057CF49EF1B355B4033333333331B4140' where gid = 493966;

--将server_request_statistic表和server_abnormal_statistic表中server_name字段长度改为50
ALTER TABLE public.server_request_statistic ALTER COLUMN server_name TYPE character varying(50);
ALTER TABLE public.server_abnormal_statistic ALTER COLUMN server_name TYPE character varying(50);
