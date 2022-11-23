package org.pygisserver.web.startegy;

import lombok.extern.slf4j.Slf4j;
import org.pygisserver.common.enumm.DataTypeEnum;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.web.service.ReadDataInfo;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;

/**
 * @Description: 文件数据类型工厂类 
 * @author:  
 * @date: 2019-07-18
 */
@Slf4j
public class FileDataInfoFactory {

	public static ReadDataInfo getDataInfo(String fileUrl, String fileType, DataStore store) {
		File file = new File(fileUrl);
		String fileName = file.getName();
		try {
			//0表示矢量，1表示栅格，6表示栅格，5表示远程
			if (DataTypeEnum.getFileCode(fileType) == 0) {
				if (DataTypeEnum.SHAPE.getFileType().equalsIgnoreCase(fileType)) {
					ShapefileDataStore dataStore = new ShapefileDataStore(new File(fileUrl).toURI().toURL()); 
					SimpleFeatureSource featureSource = dataStore.getFeatureSource();
					return new VectorDataInfo(featureSource);
				} else if (DataTypeEnum.PostGIS.getFileType().equalsIgnoreCase(fileType)) {
					FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = store.getFeatureSource(fileUrl.split(";")[1]);
					return new VectorDataInfo(featureSource);
				}
			}else if (DataTypeEnum.getFileCode(fileType) == 1 || DataTypeEnum.MBTILES.getFileType().equals(fileType)) {
				GridCoverage2DReader coverageReader = CoverageReaderFactory.getCoverageReader(fileUrl,fileType);
				return new CoverageDataInfo(coverageReader,fileType,fileUrl);
			}else if (DataTypeEnum.getFileCode(fileType) == 6) {
				//解析tdb数据中的数据资源信息
				return new CustomDataInfo(fileUrl, fileType);
			}else if (DataTypeEnum.getFileCode(fileType) == 5) {
				//解析远程数据
				return new RemoteDataInfo(fileUrl, fileType);
			}
			//默认返回值
			return new CustomDataInfo(fileUrl, fileType);
		} catch (IOException e) {
			log.error("FileDataInfoFactory getDataInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, fileName + "图层基础数据信息"));
		}
		
	}
}
