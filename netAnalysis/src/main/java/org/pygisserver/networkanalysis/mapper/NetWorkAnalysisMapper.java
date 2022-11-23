package org.pygisserver.networkanalysis.mapper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NetWorkAnalysisMapper {
	 /**
     * 地理编码
     * @param address  待分析地址
     *
     * @return
     */
    String geocoding(String address);
    
    /**
     * 逆向地理编码
     * @param lng  经度
     * @param lat  纬度
     */
    String reverseGeocoding(@Param("lng") Double lng,@Param("lat")Double lat);
}
