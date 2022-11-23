package org.pygisserver.web.interceptor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.web.license.LicenseVerify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * LicenseCheckInterceptor
 *
 * @author zifangsky
 * @date 2018/4/25
 * @since 1.0.0
 */
@Component
public class LicenseCheckInterceptor extends HandlerInterceptorAdapter{
    private static Logger logger = LogManager.getLogger(LicenseCheckInterceptor.class);
    @Autowired
    private RedisUtils redisUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        LicenseVerify licenseVerify = new LicenseVerify();
//        //校验证书是否有效
//        boolean verifyResult = licenseVerify.verify();
//        if(verifyResult){
//            return true;
//        }else{
//            response.setContentType("application/json;charset=UTF-8");
//            Map<String,String> result = new HashMap<>(1);
//            result.put("result","您的证书无效，请核查服务器是否取得授权或重新申请证书！");
//            response.sendRedirect(request.getContextPath()+"/activation");
////            response.setCharacterEncoding("utf-8");
////            response.getWriter().write(JSON.toJSONString(result));
////            response.sendRedirect("/pygisserver/expire");
//            return false;
//        }
//        return true;
//        return  true;
        String requestURI = request.getRequestURI();
        if("/pygisserver/activation.html".equals(requestURI)) {
            return true;
        }

        LicenseVerify licenseVerify = new LicenseVerify();
        String license = redisUtils.getStrValue("LICENSE");
        if("true".equals(license)){
            return true;
        }

        //校验证书是否有效
        boolean verifyResult = licenseVerify.verify();
        if(verifyResult){
            redisUtils.storeStrValue("LICENSE","LICENSE","true",600L);
            return true;
        }else{
            response.setContentType("application/json;charset=UTF-8");
            Map<String,String> result = new HashMap<>(1);
            result.put("result","您的证书无效，请核查服务器是否取得授权或重新申请证书！");
            response.sendRedirect(request.getContextPath()+"/activation");
            redisUtils.storeStrValue("LICENSE","LICENSE","false",600L);
//            response.setCharacterEncoding("utf-8");
//            response.getWriter().write(JSON.toJSONString(result));
//            response.sendRedirect("/pyserver/expire");
//            response.sendRedirect("/pygisserver/expire");
            return false;
        }
    }

}

