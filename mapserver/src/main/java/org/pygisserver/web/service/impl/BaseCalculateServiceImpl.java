package org.pygisserver.web.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.checkerframework.checker.units.qual.A;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.util.ExcelUtil;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.dao.BaseCalculateMapper;
import org.pygisserver.web.dao.PoiQueryMapper;
import org.pygisserver.web.entity.PoiQueryInfo;
import org.pygisserver.web.entity.PoiQueryResult;
import org.pygisserver.web.service.BaseCalculateService;
import org.pygisserver.web.service.PoiQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 基础量算service实现类
 * @author:  
 * @date: 2019-01-26
 */
@Service
@Slf4j
public class BaseCalculateServiceImpl<E> implements BaseCalculateService {


	@Autowired
    SqlSessionFactory sqlSessionFactory;
	
	@Autowired
    RedisUtils redisUtils;

	@Autowired
    BaseCalculateMapper baseCalculateMapper;


    @Override
    public String projection(String startPoint, String endPoint, String srid, String fromSrid) {
        return baseCalculateMapper.projection(startPoint,endPoint,srid,fromSrid);
    }
}
