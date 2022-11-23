/*切图开始时间改为可以为空，在开始切图时再将该字段回填*/
alter table public.tile_task_info alter start_seed_time DROP NOT NULL;