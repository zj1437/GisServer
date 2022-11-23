package org.pygisserver.common.data.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 写shape文件 
 * @author: 刘洋 
 * @date: 2022-05-17
 */
@Slf4j
public class PyShapeFileWriter {

	private String fileName;
	
	private DataStore dataStore;
	
	private SimpleFeatureType simpleFatureType;
	
	private FeatureWriter<SimpleFeatureType,SimpleFeature> featureWriter;
	
	public PyShapeFileWriter(String shpfile) {
		this(shpfile,null);
	}

	public PyShapeFileWriter(String shpfile,Map<String,Object> param) {
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		try {
			File shpFile = new File(shpfile);
			this.fileName = shpFile.getName().split("\\.")[0];
			if (!shpFile.getParentFile().exists()) {
				shpFile.getParentFile().mkdirs();
			}
			if (param == null) {
				param = new HashMap<String,Object>();
			}
			param.put("url",shpFile.toURI().toURL());
			this.dataStore = dataStoreFactory.createDataStore(param);
		} catch (IOException e) {
			log.error("FeShapeFileWriter contrustor has error : " + e.getLocalizedMessage());
		}
	}
	
	/**
	 * 构造矢量数据的schema
	 * @param simpleFeatureSource
	 * @throws IOException 
	 */
	public void createScheam(Collection<AttributeDescriptor> attributes,String srs) throws IOException {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setSRS(srs);
		for (AttributeDescriptor attributeDescriptor : attributes) {
			builder.add(attributeDescriptor.getLocalName(), attributeDescriptor.getType().getBinding());
		}
		builder.setName(fileName);
		this.simpleFatureType = builder.buildFeatureType();
		dataStore.createSchema(simpleFatureType);
		this.featureWriter = dataStore.getFeatureWriter(fileName, Transaction.AUTO_COMMIT);
	}

	public void setBuilder(SimpleFeatureTypeBuilder builder) throws IOException {
		builder.setName(fileName);
		this.simpleFatureType = builder.buildFeatureType();
		dataStore.createSchema(simpleFatureType);
		this.featureWriter = dataStore.getFeatureWriter(fileName, Transaction.AUTO_COMMIT);
	}

	public FeatureWriter<SimpleFeatureType,SimpleFeature> getWriter(){
		return this.featureWriter;
	}
	
	
	public SimpleFeatureType getSimpleFeatureType() {
		return simpleFatureType;
	}
	
	/**
	 * 写shape文件
	 * @param simpleFeatures
	 * @throws IOException 
	 */
	public void writeShape(Collection<SimpleFeature> simpleFeatures) throws IOException {
		if (simpleFeatures != null && simpleFeatures.size() > 0) {
			List<AttributeDescriptor> attributes = simpleFeatures.iterator().next().getFeatureType().getAttributeDescriptors();
			for (SimpleFeature simpleFeature : simpleFeatures) {
				SimpleFeature feature = featureWriter.next();
				//属性相同的赋值
				for (AttributeDescriptor attributeDescriptor : attributes) {
					feature.setAttribute(attributeDescriptor.getLocalName(), simpleFeature.getAttribute(attributeDescriptor.getLocalName()));
				}
			}
			featureWriter.write();
			featureWriter.close();
		}
	}
	
	
	/**
	 * 写shape文件
	 * @param simpleFeatures
	 * @throws IOException 
	 */
	public void writeShape(SimpleFeatureCollection simpleFeatures,String targetGeomKeyName,boolean isCloseWriter) throws IOException {
		SimpleFeatureIterator simleFeatureIterator = simpleFeatures.features();
		//插入数据有的属性
		String souceGeomKey = simpleFeatures.getSchema().getGeometryDescriptor().getLocalName();
		List<AttributeDescriptor> attributes = simpleFeatures.getSchema().getAttributeDescriptors();
		while(simleFeatureIterator.hasNext()) {
			SimpleFeature feature = featureWriter.next();
			SimpleFeature feature1  = simleFeatureIterator.next();
			//属性相同的赋值
			for (AttributeDescriptor attributeDescriptor : attributes) {
				feature.setAttribute(attributeDescriptor.getLocalName().equals(souceGeomKey) ? targetGeomKeyName : attributeDescriptor.getLocalName(), 
						feature1.getAttribute(attributeDescriptor.getLocalName()));
			}
		}
		featureWriter.write();
		if (isCloseWriter) {
			featureWriter.close();
		}
	}
	
	public void setDataStoreCharset() {
		ShapefileDataStore dataStore = (ShapefileDataStore) this.dataStore;
		dataStore.setCharset(Charset.forName("UTF-8"));

		this.dataStore = dataStore;
	}
}
