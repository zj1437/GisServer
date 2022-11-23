package org.pygisserver.common.entity;

/**
 * @Description: 常量类 
 * @author: 
 * @date: 2019-12-12
 */
public class Constant {
	
	public static final String SQLITE_DRIVER_CLASS = "org.sqlite.JDBC";
	
	public static final String PSQL_DRIVER_CLASS = "org.postgresql.Driver";
	
	public static final String MODEL_CACHE_KEY = "modelManage";
	
	public static final String STYLE_PREFIX = "Style_";
	    
    public static final int PUBLISHED_STATUS = 1;
    
    public static final int UNPUBLISHED_STATUS = 0;
    
    public static final int EXISTS_PYT_FILE = 1;
    
    public static final String OSGB_TYPE = "osgb";
    
    public static final String FILE_NOT_EXISTS = "文件不存在";
    
    public static final String IMAGE_TYPE = "png";
    
    public static final int BUFFER_ZIZE = 100000;
    
    public static final String DEFAULT_NAME = "markdata.sbl";
    
    public static final String TEMP_FILE_NAME = "tilt.pyt";
    
    public static final String LOCAL_IP = "127.0.0.1";
    
    public static final String DEM_STYLEID = "StyleInfoImpl--570ae188:124761b8d78:-7ff2";
    
    public static final String COMMON_STYLE = "/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=30&HEIGHT=30&STRICT=false&style=%s&TRANSPARENT=true";

    public static final String SPECIAL_CHARACTER = "[`~!@#$%^&*\\-+={}':;,()\\[\\].<>/?%￥……+|【】‘；：“”。，？、、\\\\]";

    public static final String TILE_CACHE_TMP = System.getProperty("java.io.tmpdir") ;
	
	public static final String EXPORT_RUN_ENV = "EXPORT_RUN_ENV";
	
	public static final String SQL = "select tile_data from tiles where tile_column = %d and tile_row = %d and zoom_level = %d";

    public static final String WGS_84_WKT = "GEOGCS[\"WGS 84\", \r\n" +
            "DATUM[\"World Geodetic System 1984\", \r\n" +
            "SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], \r\n" +
            "AUTHORITY[\"EPSG\",\"6326\"]], \r\n" +
            "PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], \r\n" +
            "UNIT[\"degree\", 0.017453292519943295], \r\n" +
            "AXIS[\"Geodetic longitude\", EAST], \r\n" +
            "AXIS[\"Geodetic latitude\", NORTH], \r\n" +
            "AUTHORITY[\"EPSG\",\"4326\"]]";

    public static final String WEB_Mercator  =  "PROJCS[\"WGS 84 / Pseudo-Mercator\", \r\n" +
            "GEOGCS[\"WGS 84\", \r\n" +
            "DATUM[\"World Geodetic System 1984\", \r\n" +
            "SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], \r\n" +
            "AUTHORITY[\"EPSG\",\"6326\"]], \r\n" +
            "PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], \r\n" +
            "UNIT[\"degree\", 0.017453292519943295], \r\n" +
            "AXIS[\"Geodetic longitude\", EAST], \r\n" +
            "AXIS[\"Geodetic latitude\", NORTH], \r\n" +
            "AUTHORITY[\"EPSG\",\"4326\"]], \r\n" +
            "PROJECTION[\"Popular Visualisation Pseudo Mercator\", AUTHORITY[\"EPSG\",\"1024\"]], \r\n" +
            "PARAMETER[\"semi_minor\", 6378137.0], \r\n" +
            "PARAMETER[\"latitude_of_origin\", 0.0], \r\n" +
            "PARAMETER[\"central_meridian\", 0.0], \r\n" +
            "PARAMETER[\"scale_factor\", 1.0], \r\n" +
            "PARAMETER[\"false_easting\", 0.0], \r\n" +
            "PARAMETER[\"false_northing\", 0.0], \r\n" +
            "UNIT[\"m\", 1.0], \r\n" +
            "AXIS[\"Easting\", EAST], \r\n" +
            "AXIS[\"Northing\", NORTH], \r\n" +
            "AUTHORITY[\"EPSG\",\"3857\"]]";

    public static final String CGCS2000_3_Degree_GK_Zone_36 = "PROJCS[\"CGCS2000_3_Degree_GK_Zone_36\", \r\n" +
            "GEOGCS[\"GCS_China_Geodetic_Coordinate_System_2000\", \r\n" +
            "DATUM[\"D_China_2000\", \r\n" +
            "SPHEROID[\"CGCS2000\", 6378137.0, 298.257222101]], \r\n" +
            "PRIMEM[\"Greenwich\", 0.0], \r\n" +
            "UNIT[\"degree\", 0.017453292519943295], \r\n" +
            "AXIS[\"Longitude\", EAST], \r\n" +
            "AXIS[\"Latitude\", NORTH]], \r\n" +
            "PROJECTION[\"Transverse_Mercator\"], \r\n" +
            "PARAMETER[\"central_meridian\", 108.0], \r\n" +
            "PARAMETER[\"latitude_of_origin\", 0.0], \r\n" +
            "PARAMETER[\"scale_factor\", 1.0], \r\n" +
            "PARAMETER[\"false_easting\", 36500000.0], \r\n" +
            "PARAMETER[\"false_northing\", 0.0], \r\n" +
            "UNIT[\"m\", 1.0],  \r\n" +
            "AXIS[\"x\", EAST], \r\n" +
            "AXIS[\"y\", NORTH]]";

    public static final String QUERY_TABLE_NAMES = "select TABLE_NAME " +
            "from (SELECT " +
            "n.nspname AS TABLE_SCHEM, c.relname AS TABLE_NAME, " +
            "CASE n.nspname ~ '^pg_' OR n.nspname = 'information_schema'  WHEN true THEN " +
            "CASE  WHEN n.nspname = 'pg_catalog' OR n.nspname = 'information_schema' THEN " +
            "CASE c.relkind  WHEN 'r' THEN 'SYSTEM TABLE'   WHEN 'v' THEN 'SYSTEM VIEW'   WHEN 'i' THEN 'SYSTEM INDEX' " +
            "ELSE NULL END WHEN n.nspname = 'pg_toast' THEN " +
            "CASE c.relkind  WHEN 'r' THEN 'SYSTEM TOAST TABLE' WHEN 'i' THEN 'SYSTEM TOAST INDEX' " +
            "ELSE NULL END ELSE " +
            "CASE c.relkind  WHEN 'r' THEN 'TEMPORARY TABLE' WHEN 'p' THEN 'TEMPORARY TABLE' WHEN 'i' THEN 'TEMPORARY INDEX' WHEN 'S' THEN 'TEMPORARY SEQUENCE'  WHEN 'v' THEN 'TEMPORARY VIEW' " +
            "ELSE NULL END END  WHEN false THEN " +
            "CASE c.relkind  WHEN 'r' THEN 'TABLE' WHEN 'p' THEN 'PARTITIONED TABLE' WHEN 'i' THEN 'INDEX' WHEN 'S' THEN 'SEQUENCE' WHEN 'v' THEN 'VIEW' WHEN 'c' THEN 'TYPE' WHEN 'f' THEN 'FOREIGN TABLE' WHEN 'm' THEN 'MATERIALIZED VIEW' " +
            "ELSE NULL  END  ELSE NULL  END  AS TABLE_TYPE " +
            "FROM pg_catalog.pg_namespace n, pg_catalog.pg_class c " +
            "LEFT JOIN pg_catalog.pg_description d ON (c.oid = d.objoid AND d.objsubid = 0) " +
            "LEFT JOIN pg_catalog.pg_class dc ON (d.classoid=dc.oid AND dc.relname='pg_class') " +
            "LEFT JOIN pg_catalog.pg_namespace dn ON (dn.oid=dc.relnamespace AND dn.nspname='pg_catalog') " +
            "WHERE c.relnamespace = n.oid  AND c.relname LIKE '%' and c.relname!='spatial_ref_sys' " +
            "AND (false  OR ( c.relkind = 'r' AND n.nspname !~ '^pg_' AND n.nspname <> 'information_schema' )  OR ( c.relkind = 'p' AND n.nspname !~ '^pg_' AND n.nspname <> 'information_schema' )  OR ( c.relkind = 'm' )  OR ( c.relkind = 'v' AND n.nspname <> 'pg_catalog' AND n.nspname <> 'information_schema' ) ) " +
            "ORDER BY TABLE_TYPE,TABLE_SCHEM,TABLE_NAME) ta " +
            "where TABLE_SCHEM = 'public' and TABLE_TYPE = 'TABLE'";

}
