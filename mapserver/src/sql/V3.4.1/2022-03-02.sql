/*特征样式添加高斯属性*/
ALTER TABLE public.map_feature_info ADD COLUMN guass_tile_size character varying(200);
ALTER TABLE public.map_feature_info ADD COLUMN guass_coordinate_bbox character varying(200);