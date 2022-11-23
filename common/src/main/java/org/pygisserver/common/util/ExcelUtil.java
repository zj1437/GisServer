package org.pygisserver.common.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class ExcelUtil {

    private static String url;

    private static String fileType;

    private static Workbook workbook;

    private static String charsetName;
    
    private GeometryFactory geometryFactory = new GeometryFactory();

    public final Map<Object,Integer> Column_Map = new HashMap<>();

    public ExcelUtil(String url) {
        this.url = url;
        fileType = url.substring(url.lastIndexOf("."));
        if (".csv".equals(fileType)){
            this.workbook = null;
        }else {
            this.workbook = getWorkbook();
        }
    }

    public ExcelUtil() {
    }

    public static void setCharsetName(String charsetName) {
        ExcelUtil.charsetName = charsetName;
    }

    /*
     * 读取excel文件类型
     * */
    public static Workbook getWorkbook(){
        ZipSecureFile.setMinInflateRatio(0);
        Workbook workbook = null;
        InputStream is = null;
        try {
            is = new FileInputStream(url);
            if (".xls".equals(fileType)){
                workbook = new HSSFWorkbook(is);
            }else if (".xlsx".equals(fileType)){
                workbook = new XSSFWorkbook(is);
            }else {
                workbook = null;
            }
        } catch (IOException e ) {
            log.error("ExcelUtil readExcel Exception: " + ExceptionsUtil.stackTraceToStr(e));
        }finally {
            try {
                is.close();
            } catch (IOException e) {
                log.error("文件流关闭异常" + ExceptionsUtil.stackTraceToStr(e) );
            }
        }

        return workbook;
    }
    
    public Workbook getWorkbookBean() {
		return this.workbook;
    }
    
    //获取excel表格第一行建库参数
    public List<Object> getExcelTableColumns(){
        Sheet sheet = workbook.getSheetAt(0);
        List<Object> columnList = new ArrayList<Object>();
        if (sheet == null){
            //表格为空
            return null;
        }
        //第一行的参数作为表头用于建表入库
        Row columns = sheet.getRow(0);
        for (int i =0; i < columns.getLastCellNum(); i++){
            if (!StringUtils.isEmpty((CharSequence) getCellFormatValue(columns.getCell(i)))) {
                //重复列名处理
                if (Column_Map.containsKey(getCellFormatValue(columns.getCell(i)))){
                    Column_Map.replace(getCellFormatValue(columns.getCell(i)), Column_Map.get(getCellFormatValue(columns.getCell(i))), Column_Map.get(getCellFormatValue(columns.getCell(i))) + 1);
                    columnList.add(getCellFormatValue(columns.getCell(i)) + "_" + i);
                }else {
                    Column_Map.put(getCellFormatValue(columns.getCell(i)), 1);
                    columnList.add(getCellFormatValue(columns.getCell(i)));
                }
            }else {
                Column_Map.put("_unknow_" + i,1);
                columnList.add("_unknow_"+i);
            }
        }
        return columnList;
    }

    //参数类型处理
    public Object getCellFormatValue(Cell cell){
        Object value = null;
        if (cell != null){
            switch (cell.getCellType()){
                case NUMERIC:{
                    if (HSSFDateUtil.isCellDateFormatted(cell)){
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = HSSFDateUtil.getJavaDate(cell.getNumericCellValue());
                        return dateFormat.format(date).toString();
                    }
                    value = cell.getNumericCellValue();
                    break;
                }
                case FORMULA:{
                    if (DateUtil.isCellDateFormatted(cell)){
                        value = cell.getDateCellValue();
                    }else {
                        value = String.valueOf(cell.getNumericCellValue());
                    }
                    break;
                }
                case STRING:{
                    try {
                        value = new String(cell.getRichStringCellValue().getString().getBytes(),charsetName);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                //Double与boolean无处理
                default: value = null;
            }
        }else {
            value = null;
        }
        return value;
    }

    //参数类型处理
    public Object getCellFormatValuePoi(Cell cell){
        Object value = null;
        if (cell != null){
            switch (cell.getCellType()){
                case NUMERIC:{
                    if (HSSFDateUtil.isCellDateFormatted(cell)){
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = HSSFDateUtil.getJavaDate(cell.getNumericCellValue());
                        return dateFormat.format(date).toString();
                    }
                    value = cell.getNumericCellValue();
                    break;
                }
                case FORMULA:{
                    if (DateUtil.isCellDateFormatted(cell)){
                        value = cell.getDateCellValue();
                    }else {
                        value = String.valueOf(cell.getNumericCellValue());
                    }
                    break;
                }
                case STRING:{
                    value = new String(cell.getRichStringCellValue().getString());
                    break;
                }
                //Double与boolean无处理
                default: value = null;
            }
        }else {
            value = null;
        }
        return value;
    }
    
    //获取表格数据(第一行除外)
    public List<Object[]> getExcelValue(){
        List<Object[]> valueList = new ArrayList<>();
        Sheet sheet = workbook.getSheetAt(0);
        int lastRowNum = sheet.getLastRowNum();
        //按照第一行建表参数入库，避免参数溢出
        Row firstRow = sheet.getRow(0);
        for (int rowNum = 1; rowNum <= lastRowNum; rowNum++){
            Row row = sheet.getRow(rowNum);
            Object value[] = new Object[firstRow.getLastCellNum()];
            if (row != null){
                for (int colNum = 0; colNum < firstRow.getLastCellNum() ;colNum++){
                    if (row.getCell(colNum) != null) {
                        value[colNum] = getCellFormatValue(row.getCell(colNum));
                    }
                }
                valueList.add(value);
            }
        }
        return valueList;
    }

    //获取表格数据包含空行
    public List<Object[]> getExcelValueBrank(){
        List<Object[]> valueList = new ArrayList<>();;
        Sheet sheet = workbook.getSheetAt(0);
        int lastRowNum = sheet.getLastRowNum();
        //按照第一行建表参数入库，避免参数溢出
        Row firstRow = sheet.getRow(0);
        for (int rowNum = 1; rowNum <= lastRowNum; rowNum++){
            Row row = sheet.getRow(rowNum);
            Object value[] = new Object[firstRow.getLastCellNum()];
            if (row != null){
                for (int colNum = 0; colNum < firstRow.getLastCellNum() ;colNum++){
                    if (row.getCell(colNum) != null) {
                        value[colNum] = getCellFormatValue(row.getCell(colNum));
                    }
                }
                valueList.add(value);
            }else {
                valueList.add(null);
            }
        }
        return valueList;
    }

  //读取poi数据
    public List<Object[]> getPoiExcelValue(){
        List<Object[]> valueList = new ArrayList<>();
        Sheet sheet = workbook.getSheetAt(0);
        int lastRowNum = sheet.getLastRowNum();
        //按照第一行建表参数入库，避免参数溢出
        Row firstRow = sheet.getRow(0);
        for (int rowNum = 2; rowNum <= lastRowNum; rowNum++){
            Row row = sheet.getRow(rowNum);
            Object value[] = new Object[firstRow.getLastCellNum() - 1];
            Double lon = null;
            Double lat = null;
            if (row != null){
                for (int colNum = 0; colNum < firstRow.getLastCellNum() ;colNum++){
                	if(colNum == firstRow.getLastCellNum() - 2) {
                    	lon = (Double)getCellFormatValuePoi(row.getCell(colNum));
                    	continue;
                    }
                    if(colNum == firstRow.getLastCellNum() - 1) {
                    	lat = (Double)getCellFormatValuePoi(row.getCell(colNum));
                    	Coordinate coordinate = new Coordinate(lon,lat);
                    	value[colNum-1] = geometryFactory.createPoint(coordinate);
                    	continue;
                    }
                    if (row.getCell(colNum) != null) {
                        value[colNum] = getCellFormatValuePoi(row.getCell(colNum));
                    }else {
                        value[colNum] = null;
                    }
                    
                }
                valueList.add(value);
            }
        }
        return valueList;
    }
    
    /*
    * 读取csv
    * */
    public List<Object[]> getCsvValue(){
        List<Object[]> values = new ArrayList<Object[]>();
        String line = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(url),charsetName));
            String firstLine = reader.readLine();//跳过第一行,获取入库参数长度
            if (!StringUtils.isEmpty(firstLine)){
                int length = firstLine.split(",").length;
                long byteSize = firstLine.getBytes().length;
                long csvFileByte = new File(url).length();
                while (byteSize <= csvFileByte){
                    line = reader.readLine();
                    byteSize += line.getBytes().length;
                    if (line.getBytes().length != 0) {
                        String[] columns = line.split(",");
                        if (columns.length <= length) {
                            if (columns.length < length){
                                String[] temp = new String[length];
                                for (int i = 0; i < columns.length; i++) {
                                    temp[i] = columns[i];
                                }
                                for (int i = columns.length ; i < length; i++) {
                                    // csv长度不同时，末尾添加" "
                                    temp[i] = "";
                                }
                                columns = temp;
                            }
                            values.add(columns);
                        }else if (columns.length == 0){
                            //,,,,解析为null
                            values.add(null);
                        }else {
                            String[] temp = new String[length];
                            for (int i=0; i<length ; i++){
                                temp[i] = columns[i];
                            }
                            values.add(temp);
                        }
                    }else {
                        values.add(null);
                    }
                }
            }
        }catch (Exception e){
            log.error("ExcelUtil getCsvValue has Exception: " + ExceptionsUtil.stackTraceToStr(e));
        }finally {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("流关闭异常 : " + ExceptionsUtil.stackTraceToStr(e) );
            }
        }

        return values;
    }

    //获取csv表格第一行建库参数
    public List<Object> getCsvTableColumns(){
        List<Object> columnList = new ArrayList<Object>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(url),charsetName));
            String[] columns = reader.readLine().split(",");
            for (int i = 0; i < columns.length; i++){
                if (!columns[i].isEmpty()) {
                    if (Column_Map.containsKey(columns[i])){
                        Column_Map.replace(columns[i], Column_Map.get(columns[i]), Column_Map.get(columns[i]) + 1);
                        columnList.add(columns[i] + "_" + i);
                    }else {
                        Column_Map.put(columns[i], 1);
                        columnList.add(columns[i]);
                    }
                }else {
                    Column_Map.put("_unknow_" + i,1);
                    columnList.add("_unknow_"+i);
                }
            }
        }catch (Exception e){
            log.error("ExcelUtil getCsvTableColumns has Exception: " + ExceptionsUtil.stackTraceToStr(e));
        }finally {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("流关闭异常 : " + ExceptionsUtil.stackTraceToStr(e) );
            }
        }
        return columnList;
    }

    public static void exportExcel(HttpServletResponse response, String fileName , File file) {
    	BufferedInputStream bi = null;
    	OutputStream os = null;
    	try {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
            bi = new BufferedInputStream(new FileInputStream(file));
            os = response.getOutputStream();
            byte[] bytes = new byte[1024];
            int i = bi.read(bytes);
            while (i != -1) {
				os.write(bytes, 0, i);
				i = bi.read(bytes);
			}
    	} catch (IOException e) {
    		 log.error("ExcelUtil exportExcel has Exception: " + ExceptionsUtil.stackTraceToStr(e));
		}finally {
            if (bi != null) FileUtil.closeStream(bi);
            if (os != null) FileUtil.closeStream(os);
        }
    }

}
