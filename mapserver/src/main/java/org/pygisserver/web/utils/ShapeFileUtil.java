package org.pygisserver.web.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.exception.BaseException;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.locationtech.jts.io.WKTConstants.*;

@Slf4j
public class ShapeFileUtil {

    public final static List<String> shpFileType = new ArrayList<>();

    static {
        shpFileType.add(".shp");
        shpFileType.add(".dbf");
        shpFileType.add(".prj");
        shpFileType.add(".qix");
        shpFileType.add(".shx");
        shpFileType.add(".sbn");
        shpFileType.add(".sbx");
        shpFileType.add(".fix");
    }

    public Map<String,List<?>> readShpFile(String filePath,String charsetName) {
        Map<String, List<?>> result = new HashMap<>();
        List<Object> columns = new ArrayList<>();
        List<Object[]> values = new ArrayList<>();
        String geomType = null;
        String geomIndex = null;
        try {
            File file = new File(filePath);
            Map<String, Object> map = new HashMap<>();
            map.put("url", file.toURI().toURL());
            DataStore dataStore = DataStoreFinder.getDataStore(map);
            ((ShapefileDataStore) dataStore).setCharset(Charset.forName(charsetName));
            String typeName = dataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
            SimpleFeatureCollection collection = featureSource.getFeatures();
            SimpleFeatureIterator iterator = collection.features();
            while (iterator.hasNext()) {
                SimpleFeature simpleFeature = iterator.next();
                List<Object> value = simpleFeature.getAttributes();
                List<Object> tmp = new ArrayList<>();
                for (int i = 0; i < value.size(); i++) {
                    if (value.get(i) != null) {
                        //处理数据含有单引号无法入库问题
                        if (value.get(i).toString().split("\\'").length != 1) {
                            int num = value.get(i).toString().split("\\'").length;
                            StringBuffer tmpString = new StringBuffer();
                            for (int j = 0; j < num; j++) {
                                if (j == num - 1) {
                                    tmpString.append(value.get(i).toString().split("\\'")[j]);
                                } else {
                                    tmpString.append(value.get(i).toString().split("\\'")[j]).append("''");
                                }
                            }
                            tmp.add(tmpString.toString());
                        } else {
                            tmp.add(value.get(i));
                        }
                    }else {
                        tmp.add(null);
                    }
                }
                values.add(tmp.toArray());
                if (StringUtils.isEmpty(geomType)) {
                    geomType = String.valueOf(simpleFeature.getProperties().iterator().next().getType().getName());
                }
                if (StringUtils.isEmpty(geomIndex)){
                    geomIndex = String.valueOf(simpleFeature.getFeatureType().indexOf("the_geom"));
                }
            }
            List<Object> dbf = null;
            try {
                dbf = readDBFFile(filePath.substring(0,filePath.lastIndexOf("."))+".dbf", charsetName);
            }catch (Exception e){
                log.error("ShapeFileUtil readShpFile has error : .shp文件同级目录下缺少.dbf文件");
            }
            //避免点矢量缩略图name大小写问题，入库即为大写，但只针对拥有name字段的数据
            if (Integer.parseInt(geomIndex) != 0) {
                for (int i = 0; i < dbf.size(); i++) {
                    if (i == Integer.parseInt(geomIndex)) {
                        columns.add(Integer.parseInt(geomIndex), "the_geom");
                        if ("name".equalsIgnoreCase(dbf.get(i).toString())) {
                            columns.add("NAME");
                        }else {
                            columns.add(dbf.get(i));
                        }
                    }else {
                        if ("name".equalsIgnoreCase(dbf.get(i).toString())) {
                            columns.add("NAME");
                        }else {
                            columns.add(dbf.get(i));
                        }
                    }
                }
            }else {
                columns.add(Integer.parseInt(geomIndex), "the_geom");
                for (int i = 0; i < dbf.size(); i++) {
                    if ("name".equalsIgnoreCase(dbf.get(i).toString())) {
                        columns.add("NAME");
                    }else {
                        columns.add(dbf.get(i));
                    }
                }
            }
        } catch (Exception e) {
            log.error("ShapeFileUtil readShpFile has error : 无法解析.shp文件");
        }
        List<Object> geomInfo = new ArrayList<>();
        geomInfo.add(geomType);
        geomInfo.add(geomIndex);
        result.put("index",columns);
        result.put("values",values);
        result.put("geomInfo",geomInfo);
        return result;
    }

    private List<Object> readDBFFile(String filePath,String charsetName){
        List<Object> columns = new ArrayList<>();
        try {
            FileChannel in = new FileInputStream(filePath).getChannel();
            DbaseFileReader dbfReader = new DbaseFileReader(in,false,Charset.forName(charsetName));
            DbaseFileHeader header = dbfReader.getHeader();
            int fields = header.getNumFields();
//            DbaseFileReader.Row row = dbfReader.readRow();
            for (int i = 0; i < fields; i++) {
                columns.add(header.getFieldName(i));
            }
            dbfReader.close();
            in.close();
        } catch (Exception e) {
            log.error("ShapeFileUtil readDBFFile has error : 读取.tdb文件流失败");
        }
        return columns;
    }

    public List<Object> readShpGeometry(String filePath){
        List<Object> list = new ArrayList<>();
        try {
            ShpFiles shpFiles = new ShpFiles(filePath);
            ShapefileReader reader = new ShapefileReader(shpFiles,false,false,new GeometryFactory());
            while (reader.hasNext()){
                Geometry shape = (Geometry) reader.nextRecord().shape();
                list.add(shape.toString());
            }
        }catch (Exception e){
            log.error("ShapeFileUtil readShpGeometry has error : 无法解析.shp文件");
        }
        return list;
    }


    public File exportShpFile(List<Object> columnsList , List<Map<String,Object>> dataList , String exportPath,String geomType,String fileName,String srs){
        File dirFile = new File(exportPath + File.separator+ fileName);
        if (!dirFile.exists()){
            dirFile.mkdirs();
        }
        File shpFile = new File(dirFile + File.separator + fileName + ".shp");

        try {
            Map<String, Serializable> params = new HashMap<>();
            params.put(ShapefileDataStoreFactory.URLP.key, shpFile.toURI().toURL());
            ShapefileDataStore dataStore = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);
            //定义图形信息和属性信息
            dataStore.setCharset(StandardCharsets.UTF_8);
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            //修复导出shp schema对象转换投影坐标错误问题
            if ("3857".equals(srs)) {
                builder.setCRS(CRS.decode("EPSG:3857"));
            } else {
                builder.setCRS(DefaultGeographicCRS.WGS84);
            }
            builder.setName(fileName);
            for (int i = 0; i < columnsList.size(); i++) {
                String column = String.valueOf(columnsList.get(i));
                //大小写未校验
                if ("geom".equals(column) || "the_geom".equals(column) || "geometry".equals(column)) {
                    switch (geomType.toUpperCase()) {
                        case POINT:
                            builder.add(column, Point.class);
                            break;
                        case MULTIPOINT:
                            builder.add(column, MultiPoint.class);
                            break;
                        case LINESTRING:
                            builder.add(column, LineString.class);
                            break;
                        case MULTILINESTRING:
                            builder.add(column, MultiLineString.class);
                            break;
                        case POLYGON:
                            builder.add(column, Polygon.class);
                            break;
                        case MULTIPOLYGON:
                            builder.add(column, MultiPolygon.class);
                            break;
                    }
                } else {
                    builder.add(String.valueOf(column), String.class);
                }
            }
            dataStore.createSchema(builder.buildFeatureType());
            //设置writer
            FeatureWriter<SimpleFeatureType, SimpleFeature> writer = dataStore.getFeatureWriter(dataStore.getTypeNames()[0], Transaction.AUTO_COMMIT);
            generateCoordinateData(dataList, writer);
            writer.write();
            writer.close();
            dataStore.dispose();
            //最后生成.cpg矢量编码文件，用于发布的编码格式
            generateCpgFile(dirFile.getAbsolutePath(), fileName, Charset.forName("utf-8").name());
            return dirFile;
        } catch (Exception e) {
            shpFile.delete();
            log.error("ShapeFileUtil exportShpFile has error : 导出矢量文件异常");
            throw new BaseException("ShapeFileUtil exportShpFile has error :" + e.getLocalizedMessage());
        }
    }

    private void generateCpgFile(String localDirPath, String tableName , String chartSetName){
        File file = new File(localDirPath+File.separator+tableName+".cpg");
        try{
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(chartSetName);
            writer.flush();
            writer.close();
        }catch (Exception e){
            log.error("ShapeFileUtil generateCoordinateData has error : 写入.cpg文件错误");
        }
    }

    private void generateCoordinateData(List<Map<String,Object>> dataList,FeatureWriter<SimpleFeatureType,SimpleFeature> writer){
        try {
            SimpleFeature feature;
            WKTReader wktReader = new WKTReader(JTSFactoryFinder.getGeometryFactory());
            for (Map<String,Object> map : dataList){
                feature = writer.next();
                for (Map.Entry<String,Object> entry : map.entrySet()){
                    String key = entry.getKey();
                    if (key == null) {
                        continue;
                    }
                    if (key.length()>10){
                        key = key.substring(0,10);
                    }
                    String value = entry.getValue() == null ? "":entry.getValue().toString();
                    if ("geom".equals(key) || "the_geom".equals(key) || "geometry".equals(key)){
                        Geometry geometry = wktReader.read(value);
                        feature.setAttribute("the_geom",geometry);
                    }else {
                        feature.setAttribute(key,value);
                    }
                }
            }
        }catch (Exception e){
            log.error("ShapeFileUtil generateCoordinateData has error : 写入.shp文件错误");
        }
    }

    public void dealShapeFileCache(String shpFilePath){
        String filePath = shpFilePath.substring(0, shpFilePath.lastIndexOf("."));
        shpFileType.parallelStream().forEach(type ->{
            File file = new File(String.valueOf(filePath+type));
            if (file.exists()){
                file.delete();
            }
        });
    }

}

