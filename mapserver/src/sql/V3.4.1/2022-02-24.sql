-- Table: public.poi_region
-- admincode子查询关联表
DROP TABLE IF EXISTS public.poi_region;

CREATE TABLE public.poi_region
(
  gid integer NOT NULL,
  province_name character varying(254),
  city_name character varying(254),
  region_name character varying(254),
  phone character varying(254),
  rescue_telephone character varying(254),
  admincode character varying(254),
  CONSTRAINT poi_region_pkey PRIMARY KEY (gid)
)
WITH (
  OIDS=FALSE
);