--map_server_info添加over_view字段表示是否生成金字塔缓存
ALTER TABLE public.map_server_info ADD COLUMN over_view smallint DEFAULT 0;