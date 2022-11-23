--修改map_feature_info表中tile_extension字段长度
ALTER TABLE public.map_feature_info ADD COLUMN input_transparent_color character varying(10);
ALTER TABLE public.map_feature_info ADD COLUMN output_transparent_color character varying(10);