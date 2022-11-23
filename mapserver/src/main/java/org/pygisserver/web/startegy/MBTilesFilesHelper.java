package org.pygisserver.web.startegy;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.SqlException;
import org.geotools.mbtiles.MBTilesFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MBTilesFilesHelper extends MBTilesFile {
    private static String viewSql = "create view tiles as select map.zoom_level as zoom_level, map.tile_column as tile_column, map.tile_row as tile_row images.tile_data as tile_data from map join images on images.tile_id = map.tile_id";
    private static String tileSql = "select count(0) from sqlite_master where name='tiles'";
    private static String TBL_NAME_SQL = "SELECT name FROM sqlite_master where type='table' and name in ('map','tiles')";
    private static String QUERY_INDEX = "select sql from sqlite_master where type='index' and tbl_name = ?";
    private static String CREATE_INDEX = "CREATE UNIQUE INDEX tile_index ON %s (zoom_level,tile_column,tile_row)";
    private static String OPEN_WAL = "pragma journal_mode = WAL";
    
    private DataSource connPool;
	
	public MBTilesFilesHelper(File file) throws IOException {
		super(file);
		this.connPool = super.connPool;
		//查看当前数据中是否有tiles的表或者视图
		checkMBTilesDataType();
		//其次查看当前数据库对应的表是否创建了索引
		String tblName = queryExistsTblName();
		boolean existsIndex = checkIndex(tblName);
		if (!existsIndex) {
			createIndex(tblName);
		}
		//开启WAL
		openWAL();
	}
	
	private void openWAL() {
		Connection cx = null;
		PreparedStatement ps = null;
		try {
			cx = connPool.getConnection();
			ps = cx.prepareStatement(OPEN_WAL);
			ps.execute();
        } catch (SQLException e) {
        	log.error("MBTilesFilesHelper openWAL has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_OPEN, "Sqlite数据库预写日志功能"));
		}finally {
			try {
				ps.close();
			} catch (SQLException e) {
				log.error("MBTilesFilesHelper openWAL has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			try {
				cx.close();
			} catch (SQLException e) {
				log.error("MBTilesFilesHelper openWAL has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
		}
	}
	
	private void createIndex(String tblName) {
		Connection cx = null;
		PreparedStatement ps = null;
		try {
			cx = connPool.getConnection();
			CREATE_INDEX = String.format(CREATE_INDEX, tblName);
			ps = cx.prepareStatement(CREATE_INDEX);
			ps.execute();
        } catch (SQLException e) {
        	log.error("MBTilesFilesHelper createIndex has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_CREATE, "MBTiles图层数据索引"));
		}finally {
			try {
				ps.close();
			} catch (SQLException e) {
				log.error("MBTilesFilesHelper createIndex has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			try {
				cx.close();
			} catch (SQLException e) {
				log.error("MBTilesFilesHelper createIndex has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
		}
	}
	
	private boolean checkIndex(String tblName) {
		Connection cx = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String indexSql = null;
		try {
			cx = connPool.getConnection();
			ps = cx.prepareStatement(QUERY_INDEX);
			ps.setString(1, tblName);
			rs = ps.executeQuery();
			while(rs.next()) {
				indexSql = rs.getString(1);
				if (indexSql != null 
						&& indexSql.contains("zoom_level") 
						&& indexSql.contains("tile_column")
						&& indexSql.contains("tile_row")) {
					return true;
				}
			}
        } catch (SQLException e) {
        	log.error("MBTilesFilesHelper checkIndex has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "MBTiles图层数据"));
		}finally {
			try {
				rs.close();
			} catch (SQLException e) {
				log.error("MBTilesFilesHelper checkIndex has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			try {
				ps.close();
			} catch (SQLException e) {
				log.error("MBTilesFilesHelper checkIndex has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			try {
				cx.close();
			} catch (SQLException e) {
				log.error("MBTilesFilesHelper checkIndex has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
		}
		return false;
	}
	
	private String queryExistsTblName() {
		Connection cx = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String tblName = null;
		try {
			cx = connPool.getConnection();
			ps = cx.prepareStatement(TBL_NAME_SQL);
			rs = ps.executeQuery();
			while(rs.next()) {
				tblName = rs.getString(1);
			}
        } catch (SQLException e) {
        	log.error("MBTilesFilesHelper queryExistsTblName has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "MBTiles瓦片数据表"));
		}finally {
			try {
				rs.close();
			} catch (SQLException e) {
				log.error("MBTilesFilesHelper queryExistsTblName has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			try {
				ps.close();
			} catch (SQLException e) {
				log.error("MBTilesFilesHelper queryExistsTblName has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			try {
				cx.close();
			} catch (SQLException e) {
				log.error("MBTilesFilesHelper queryExistsTblName has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
		}
		return tblName;
	}
	
	
	private void checkMBTilesDataType() {
		Connection cx = null;
		try {
			cx = connPool.getConnection();
			PreparedStatement ps = cx.prepareStatement(tileSql);
			ResultSet sResultSet = ps.executeQuery();
			sResultSet.next();
			if (sResultSet.getInt(1) > 0) {
				return;
			} else {
				ps = cx.prepareStatement(viewSql);
				ps.execute();
			}
        } catch (SQLException e) {
        	log.error("MBTilesFilesHelper checkMBTilesDataType has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "MBTiles瓦片数据类型"));
        } finally {
        	try {
				cx.close();
			} catch (SQLException e) {
				log.error("MBTilesFilesHelper checkMBTilesDataType has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
		}
	}
}
