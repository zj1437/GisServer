package org.pygisserver.common.util;

import lombok.extern.slf4j.Slf4j;
import org.pygisserver.common.exception.ExceptionsUtil;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

/**
 * CSV导出utils类
 *
 * @author 张亨特
 * @since 2021-10-18
 */
@Slf4j
public class ExportUtils {
    /**
     * csv文件导出utils方法
     *
     * @param response response
     * @param fileName 文件名称
     * @param exportInfo 导出数据
     */
    public static void doExportCsv(HttpServletResponse response, String fileName, String exportInfo) {
        try (ServletOutputStream outputStream = response.getOutputStream()){
            response.setContentType("application/csv");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Pragma", "public");
            response.setHeader("Cache-Control", "max-age=30");
            response.setHeader("Cotext-Disposition", "attachment; filaName=" + URLEncoder.encode(fileName, "UTF-8"));
            outputStream.write(exportInfo.getBytes("GBK"));
            outputStream.flush();
        } catch (IOException e) {
            log.error("Csv Exprot Exception: " + ExceptionsUtil.stackTraceToStr(e));
        }
    }

    /**
     * GeoJson文件导出utils方法
     *
     * @param response response
     * @param fileName 文件名称
     * @param exportInfo 导出数据
     */
    public static void doExportGeoJson(HttpServletResponse response, String fileName, String exportInfo){
        try (ServletOutputStream outputStream = response.getOutputStream()){
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Pragma", "public");
            response.setHeader("Cache-Control", "max-age=30");
            response.setHeader("Cotext-Disposition", "attachment; filaName=" + URLEncoder.encode(fileName, "UTF-8"));
            outputStream.write(exportInfo.getBytes("GBK"));
            outputStream.flush();
        } catch (IOException e) {
            log.error("GeoJson Exprot Exception: " + ExceptionsUtil.stackTraceToStr(e));
        }
    }
}