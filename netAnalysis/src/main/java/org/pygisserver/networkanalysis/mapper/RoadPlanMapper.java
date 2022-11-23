package org.pygisserver.networkanalysis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.common.entity.RoadPlanEntity;
import org.springframework.stereotype.Repository;

/**
 * @Description: 路径规划mapper
 * @author: 刘洋
 * @date: 2022-02-09
 */
@Repository
public interface RoadPlanMapper {

    /**
     * 获取请求规划的最短路径
     * @param startLon  起点经度
     * @param startLat  起点纬度
     * @param endLon    终点经度
     * @param endLat    终点纬度
     * @param rationValue 限制宽度
     * @return
     */
    String queryShortestRoad(@Param("startLon") double startLon,
                             @Param("startLat") double startLat,
                             @Param("endLon") double endLon,
                             @Param("endLat") double endLat,
                             @Param("rationValue") double rationValue);



    /**
     * 获取请求规划的最优路径
     * @param startLon  起点经度
     * @param startLat  起点纬度
     * @param endLon    终点经度
     * @param endLat    终点纬度
     * @param rationValue 限制宽度
     * @return
     */
    String queryBestRoad(@Param("startLon") double startLon,
                         @Param("startLat") double startLat,
                         @Param("endLon") double endLon,
                         @Param("endLat") double endLat,
                         @Param("rationValue") double rationValue);
    
    
    
    /**
     * 获取请求规划的最短路径
     * @param startLon  起点经度
     * @param startLat  起点纬度
     * @param endLon    终点经度
     * @param endLat    终点纬度
     * @param rationValue 限制宽度
     * @return
     */
    List<RoadPlanEntity> queryShortestRoads(@Param("startLon") double startLon,
			                                @Param("startLat") double startLat,
			                                @Param("endLon") double endLon,
			                                @Param("endLat") double endLat,
			                                @Param("rationValue") double rationValue);



    /**
     * 获取请求规划的最优路径
     * @param startLon  起点经度
     * @param startLat  起点纬度
     * @param endLon    终点经度
     * @param endLat    终点纬度
     * @param rationValue 限制宽度
     * @return
     */
    List<RoadPlanEntity> queryBestRoads(@Param("startLon") double startLon,
										@Param("startLat") double startLat,
										@Param("endLon") double endLon,
									    @Param("endLat") double endLat,
									    @Param("rationValue") double rationValue);

}
