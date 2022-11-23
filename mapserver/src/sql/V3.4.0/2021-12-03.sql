/*删除请求统计表中的主键*/
alter table server_visit_statistic drop CONSTRAINT server_visit_statistic_pkey;
/*请求表及请求统计表新增server_id字段*/
alter table public.server_request_statistic add column server_id integer NOT NULL;
alter table public.server_visit_statistic add column server_id integer NOT NULL primary key;