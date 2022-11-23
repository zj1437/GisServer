/*修改*/
ALTER TABLE public.dynamic_data_source ALTER COLUMN source_key TYPE character varying(200);

ALTER TABLE public.dynamic_data_source  ALTER COLUMN jdbc_url TYPE character varying(200);