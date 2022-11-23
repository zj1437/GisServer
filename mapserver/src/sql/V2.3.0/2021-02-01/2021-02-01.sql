	/*染色图色带*/
	DROP TABLE IF EXISTS color_map;
	CREATE TABLE color_map(
	    color_map_id serial primary key, -- 色带id
	    color_map_name varchar(20) NOT NULL, -- 色带名称
	    color_map_url varchar(200) NOT NULL -- 色带url
	);
	
	/*插入色带信息*/
INSERT INTO color_map(color_map_id,color_map_name,color_map_url) VALUES(DEFAULT,'染色图1','/style/legend/染色图1.jpg');
INSERT INTO color_map(color_map_id,color_map_name,color_map_url) VALUES(DEFAULT,'染色图2','/style/legend/染色图2.jpg');
INSERT INTO color_map(color_map_id,color_map_name,color_map_url) VALUES(DEFAULT,'染色图3','/style/legend/染色图3.jpg');
INSERT INTO color_map(color_map_id,color_map_name,color_map_url) VALUES(DEFAULT,'染色图4','/style/legend/染色图4.jpg');
INSERT INTO color_map(color_map_id,color_map_name,color_map_url) VALUES(DEFAULT,'染色图5','/style/legend/染色图5.jpg');
INSERT INTO color_map(color_map_id,color_map_name,color_map_url) VALUES(DEFAULT,'染色图6','/style/legend/染色图6.jpg');
INSERT INTO color_map(color_map_id,color_map_name,color_map_url) VALUES(DEFAULT,'染色图7','/style/legend/染色图7.jpg');
INSERT INTO color_map(color_map_id,color_map_name,color_map_url) VALUES(DEFAULT,'染色图8','/style/legend/染色图8.jpg');