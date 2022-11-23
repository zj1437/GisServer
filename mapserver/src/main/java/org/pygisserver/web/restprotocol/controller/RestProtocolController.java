package org.pygisserver.web.restprotocol.controller;

import com.alibaba.fastjson.JSONArray;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.web.restprotocol.entity.RestRequestParam;
import org.pygisserver.web.restprotocol.service.RestProtocolService;
import org.pygisserver.web.service.MapServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Api(value = "rest服务接口", tags = "切图服务接口")
@RestController
@RequestMapping("/pytc/service")
@Slf4j
public class RestProtocolController {

    @Autowired
    private RestProtocolService restProtocolService;
    @Autowired
    private MapServerService mapServerService;

    /**
     * rest获取
     * @param req 请求参数
     * @param rep 响应参数
     */
    @ApiOperation(value = "rest获取", notes = "rest获取")
    @ApiImplicitParams({ @ApiImplicitParam(name = "sourceName", value = "数据库连接名", required = true, paramType = "query"),
            @ApiImplicitParam(name = "restServerName", value = "服务名（表名）", required = false, paramType = "query"),
            @ApiImplicitParam(name = "pageNum", value = "页面", required = false, paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页数 传0默认查全部", required = false, paramType = "query") })
    @GetMapping(value = { "/rest/{restServerName}/{pageNum}/{pageSize}" })
    public ReponseEntity tmsTile(@PathVariable(value = "restServerName", required = true) String restServerName,
                                            @PathVariable(value = "pageNum", required = true) int pageNum,
                                            @PathVariable(value = "pageSize", required = true) int pageSize,
                                            HttpServletRequest req,
                                            HttpServletResponse rep) {

        RestRequestParam requestInfo = new RestRequestParam();
        requestInfo.setRestServerName(restServerName);
        requestInfo.setPageNum(pageNum);
        requestInfo.setPageSize(pageSize);
        return restProtocolService.getRestFeatureInfo(requestInfo);

    }

}
