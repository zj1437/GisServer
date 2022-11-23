package org.pygisserver.web.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.util.ExcelUtil;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.dao.PoiQueryMapper;
import org.pygisserver.web.entity.PoiQueryInfo;
import org.pygisserver.web.entity.PoiQueryResult;
import org.pygisserver.web.service.PoiQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: poi查询service实现类 
 * @author:  
 * @date: 2019-01-26
 */
@Service
@Slf4j
public class PoiQueryServiceImpl<E> implements PoiQueryService {

	@Autowired
	private PoiQueryMapper poiMapper;
	
	@Autowired
    SqlSessionFactory sqlSessionFactory;
	
	@Autowired
    RedisUtils redisUtils;
	
	private static String timestamp = String.valueOf(System.currentTimeMillis());
	
	
	@Override
	public int queryBestPoiCount(PoiQueryInfo queryParam) {
		try {
			return poiMapper.queryBestPoiCount(queryParam);
		} catch (Exception e) {
			log.error("PoiQueryServiceImpl queryBestPoiCount has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "兴趣点总数"));
		}
	}

	@Override
	public List<PoiQueryResult> queryBestPoiInfo(PoiQueryInfo queryParam) {
		try {
			return poiMapper.queryBestPoiInfo(queryParam);
		} catch (Exception e) {
			log.error("PoiQueryServiceImpl queryBestPoiInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, queryParam.getKeyWords() + "POI"));
		}
	}

	@Override
	@Transactional
	public String ExportTableToPostGis(String reqIp,MultipartFile[] files) {
		String newName = null;
		String filePath = null;
		ExcelUtil excelUtil = null;
        List<Object[]> list = null;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getOriginalFilename().endsWith(".xlsx")
                    || files[i].getOriginalFilename().endsWith(".xls")
                    || files[i].getOriginalFilename().endsWith(".csv")) {
                File file = new File(getFilePath(),files[i].getOriginalFilename());
                if (file.exists()) {
                	newName = saveFiles2CachePath(files[i],timestamp + "",reqIp);
                } else {
                	newName = saveFiles2CachePath(files[i],null,reqIp);
                }
            }
        }
        if (redisUtils.hasKey("String", newName)) {
            Map<String, String> value = (Map<String, String>) redisUtils.getValue(newName);
            filePath = FileUtil.decodeFilePath(value.get(newName)) + File.separator + newName;
        }
        File file = new File(filePath);
        String fileType = filePath.substring(filePath.lastIndexOf("."));
        if (!file.exists() || !(".xls".equals(fileType) || ".xlsx".equals(fileType))){
            //文件不存在(路径错误)
            return "导入失败;无法识别的文件";
        }
        try {
        	excelUtil = new ExcelUtil(filePath);
		} catch (Exception e) {
			 log.error("ExcelUtil getWorkbook Exception: " + ExceptionsUtil.stackTraceToStr(e));
			 return "文件格式错误";
		}
		excelUtil.setCharsetName("UTF-8");
		try {
			list = excelUtil.getPoiExcelValue();
		} catch (Exception e) {
			log.error("ExcelUtil getPoiExcelValue Exception: " + ExceptionsUtil.stackTraceToStr(e));
			 return "文件数据错误";
		}
		try(SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH,false)){
			PoiQueryMapper mapper = sqlSession.getMapper(PoiQueryMapper.class);
            for (int i = 0; i < list.size() ; i++) {
                mapper.insertPostGisTable(list.get(i));
                if (i % 3000 == 0){
                    sqlSession.commit();
                    sqlSession.clearCache();
                }
            }
            //批处理剩余记录提交
            sqlSession.commit();
            sqlSession.clearCache();
        }catch (Exception e){
            log.error("PoiQueryServiceImpl ExportTableToPostGis has error {}", e.getLocalizedMessage());
            return "导入失败;SQL异常";
        }
		
		return "导入成功";
	}
	public static String getFilePath() {
        File file = new File(Constant.TILE_CACHE_TMP,"import");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }
	
	public String saveFiles2CachePath(MultipartFile file, String timestamp,String reqIP) {
        File destFile = null;
        String shpFileName = null;
        if (StringUtils.isBlank(timestamp)) {
            destFile = new File(getShpFilePath(),file.getOriginalFilename());
            if (file.getOriginalFilename().endsWith(".xlsx")
                    || file.getOriginalFilename().endsWith(".xls")
                    || file.getOriginalFilename().endsWith(".csv")
                    || file.getOriginalFilename().endsWith(".shp")
                    || file.getOriginalFilename().endsWith(".dbf")
                    || file.getOriginalFilename().endsWith(".prj")
                    || file.getOriginalFilename().endsWith(".qix")
                    || file.getOriginalFilename().endsWith(".sbn")
                    || file.getOriginalFilename().endsWith(".sbx")
                    || file.getOriginalFilename().endsWith(".shx")) shpFileName = file.getOriginalFilename();
        } else {
            String name = file.getOriginalFilename();
            String nameWithoutSuffx = name.substring(0, name.lastIndexOf("."));
            String newName = nameWithoutSuffx + "_" + timestamp + name.replace(nameWithoutSuffx, "");
            destFile = new File(getShpFilePath(),newName);
            if (name.endsWith(".xlsx")
                    || name.endsWith(".xls")
                    || name.endsWith(".csv")
                    || name.endsWith(".shp")
                    || name.endsWith(".dbf")
                    || name.endsWith(".prj")
                    || name.endsWith(".qix")
                    || name.endsWith(".sbn")
                    || name.endsWith(".sbx")
                    || name.endsWith(".shx")) shpFileName = newName;
        }
        try {
            if (!redisUtils.hasKey(reqIP, shpFileName)) {
                Map<String, String> map = new HashMap<>();
                map.put(shpFileName, FileUtil.decodeFilePath(destFile.getParentFile().getAbsolutePath()));
                redisUtils.storeValue(reqIP, shpFileName,map );
            }
            file.transferTo(destFile);
        } catch (IOException e) {
            log.error("ToolsServerServiceImpl saveFiles2CachePath has error:" + ExceptionsUtil.stackTraceToStr(e));
            throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_UPLOAD, ""));
        }
        return shpFileName;
    }
	
	public String getShpFilePath() {
        File file = new File(Constant.TILE_CACHE_TMP,"import");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath();
    }

	
}
