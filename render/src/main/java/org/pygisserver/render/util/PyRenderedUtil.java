package org.pygisserver.render.util;

import lombok.extern.slf4j.Slf4j;
import org.pygisserver.common.entity.PyRenderRequestParam;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverageio.gdal.erdasimg.ErdasImgReader;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.mbtiles.mosaic.MBTilesReader;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.util.Converters;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

import javax.media.jai.Interpolation;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Description: 渲染工具类
 * @author: 
 * @date: 2021-03-15
 */
@Slf4j
public class PyRenderedUtil {

	/**
	 * 渲染抗锯齿设置
	 */
	public static final String AA_NONE = "NONE";

	public static final String AA_TEXT = "TEXT";

	public static final String AA_FULL = "FULL";
    
	public static final List<String> AA_SETTINGS = Arrays.asList(AA_NONE, AA_TEXT, AA_FULL);
	
	/**
	 * 获取渲染读取参数
	 * 
	 * @param params  数据自身参数
	 * @param values  用户选择参数
	 * @param readGeom  是否阅读geomtry
	 * @return
	 */
	public static GeneralParameterValue[] getParameters(ParameterValueGroup params, Map values, boolean readGeom) {
		final List<ParameterValue<?>> parameters = new ArrayList<ParameterValue<?>>();
		final String readGeometryKey = AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString();
		if ((params != null) && (params.values().size() > 0)) {
			final List<GeneralParameterValue> elements = params.values();
			for (GeneralParameterValue elem : elements) {
				final ParameterValue<?> val = (ParameterValue<?>) elem;
				if (val != null) {
					final ParameterDescriptor<?> descr = val.getDescriptor();
					final String _key = descr.getName().toString();
					if ("namespace".equals(_key)) {
						continue;
					}
					// 忽略ReadGridGeometry2D参数
					if (_key.equalsIgnoreCase(readGeometryKey) && !readGeom) {
						continue;
					}
					final Object value;
					if (values != null && !values.isEmpty()) {
						value = getCvParamValue(_key, val, values);
					}else {
						value = val.getValue();
					}
					parameters.add(new DefaultParameterDescriptor(_key, descr.getValueClass(), null, value).createValue());
				}
			}
			return (!parameters.isEmpty())
					? (GeneralParameterValue[]) parameters.toArray(new GeneralParameterValue[parameters.size()])
					: new GeneralParameterValue[0];
		} else {
			return new GeneralParameterValue[0];
		}
	}

	private static Object getCvParamValue(final String key, ParameterValue param, final Map params) {
		Object value = null;
		try {
			if (key.equalsIgnoreCase("crs")) {
				if ((params.get(key) != null) && (((String) params.get(key)).length() > 0)) {
					value = CRS.parseWKT((String) params.get(key));
				} else {
					log.warn("Unable to find a crs for the coverage param, using EPSG:4326");
					value = CRS.decode("EPSG:4326");
				}
			} else if (key.equalsIgnoreCase("envelope")) {
				if ((params.get(key) != null) && (((String) params.get(key)).length() > 0)) {
					String tmp = (String) params.get(key);
					if ((tmp.indexOf("[") > 0) && (tmp.indexOf("]") > tmp.indexOf("["))) {
						tmp = tmp.substring(tmp.indexOf("[") + 1, tmp.indexOf("]")).trim();
						tmp = tmp.replaceAll(",", "");
						String[] strCoords = tmp.split(" ");
						double[] coords = new double[strCoords.length];
						if (strCoords.length == 4) {
							for (int iT = 0; iT < 4; iT++) {
								coords[iT] = Double.parseDouble(strCoords[iT].trim());
							}
							value = (org.opengis.geometry.Envelope) new GeneralEnvelope(
									new double[] { coords[0], coords[1] }, new double[] { coords[2], coords[3] });
						}
					}
				}
			} else if (key.equalsIgnoreCase(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString())) {
				if ((params.get(key) != null) && params.get(key) instanceof String
						&& (((String) params.get(key)).length() > 0)) {
					String tmp = (String) params.get(key);
					if ((tmp.indexOf("[") > 0) && (tmp.indexOf("]") > tmp.indexOf("["))) {
						tmp = tmp.substring(tmp.indexOf("[") + 1, tmp.indexOf("]")).trim();
						tmp = tmp.replaceAll(",", "");
						String[] strCoords = tmp.split(" ");
						double[] coords = new double[strCoords.length];
						if (strCoords.length == 4) {
							for (int iT = 0; iT < 4; iT++) {
								coords[iT] = Double.parseDouble(strCoords[iT].trim());
							}
							value = (org.opengis.geometry.Envelope) new GeneralEnvelope(
									new double[] { coords[0], coords[1] }, new double[] { coords[2], coords[3] });
						}
					}
				} else if ((params.get(key) != null) && params.get(key) instanceof GeneralGridGeometry) {
					value = params.get(key);
				}
			} else {
				final Class<? extends Object> target = param.getDescriptor().getValueClass();
				if (key.equalsIgnoreCase("InputTransparentColor") || key.equalsIgnoreCase("OutputTransparentColor")) {
					if (params.get(key) != null) {
						value = Color.decode((String) params.get(key));
					} else {
						Class[] clArray = { Color.class };
						Object[] inArray = { params.get(key) };
						value = target.getConstructor(clArray).newInstance(inArray);
					}
				} else if (key.equalsIgnoreCase("BackgroundValues")) {
					if (params.get(key) != null) {
						String temp = (String) params.get(key);
						String[] elements = temp.split(",");
						final double[] backgroundValues = new double[elements.length];
						for (int i = 0; i < elements.length; i++)
							backgroundValues[i] = Double.valueOf(elements[i]);
						value = backgroundValues;
					}
				} else if (key.equalsIgnoreCase("InputImageThresholdValue")) {
					if (params.get(key) != null) {
						String temp = (String) params.get(key);
						value = Double.valueOf(temp);
					}
				} else if (key.equalsIgnoreCase("Filter")) {
					Object sfilter = params.get(key);
					if (sfilter != null) {
						if (sfilter instanceof String) {
							value = ECQL.toFilter((String) sfilter);
						} else if (sfilter instanceof Filter) {
							value = (Filter) sfilter;
						}
					} else {
						value = param.getValue();
					}
				} else {
					value = params.get(key);
					if (value != null) {
						Object converted = Converters.convert(value, target);
						if (converted == null) {
							throw new RuntimeException("Failed to convert " + value + " to " + target.getName());
						} else {
							value = converted;
						}
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			value = param.getValue();
		}
		return value;
	}

	/**
	 * 获取gridcoverage2DReader
	 * 
	 * @return
	 */
	public static GridCoverage2DReader getGridCoverage2DReader(PyRenderRequestParam requestParam) throws IOException {
		switch (requestParam.getDataFormat()) {
		case "GeoTIFF":
			return new GeoTiffReader(new File(requestParam.getFileUrl()));
		case "MBTiles":
			return new MBTilesReader(new File(requestParam.getFileUrl()), null);
		case "ERDASImg":
			return new ErdasImgReader(new File(requestParam.getFileUrl()));
		case "ImageMosaic":
			return new ImageMosaicReader(new File(requestParam.getFileUrl()));
		default:
			return new GeoTiffReader(new File(requestParam.getFileUrl()));
		}
	}


	/**
	 * 获取读请求参数
	 * 
	 * @param params
	 * @param interpolation
	 * @param bgColor
	 * @param bandIndices
	 * @return
	 */
	public static GeneralParameterValue[] getReadParameters(final GeneralParameterValue[] params, 
															final ReferencedEnvelope envelope,
															final Rectangle requestedRasterArea, 
															final Interpolation interpolation, 
															final Color bgColor,
															int[] bandIndices) {
		Parameter<GridGeometry2D> readGG = null;
		if (envelope != null) {
			readGG = (Parameter<GridGeometry2D>) AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
			readGG.setValue(new GridGeometry2D(new GridEnvelope2D(requestedRasterArea), envelope));
		}
		final Parameter<Interpolation> readInterpolation = (Parameter<Interpolation>) ImageMosaicFormat.INTERPOLATION
				.createValue();
		readInterpolation.setValue(interpolation);
		final Parameter<Color> bgColorParam;
		if (bgColor != null) {
			bgColorParam = (Parameter<Color>) AbstractGridFormat.BACKGROUND_COLOR.createValue();
			bgColorParam.setValue(bgColor);
		} else {
			bgColorParam = null;
		}
		Parameter<int[]> bandIndicesParam = null;
		if (bandIndices != null) {
			bandIndicesParam = (Parameter<int[]>) AbstractGridFormat.BANDS.createValue();
			bandIndicesParam.setValue(bandIndices);
		}
		GeneralParameterValue[] readParams = (GeneralParameterValue[]) params;
		final int length = readParams == null ? 0 : readParams.length;
		if (length > 0) {
			final String readGGName = AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString();
			final String readInterpolationName = ImageMosaicFormat.INTERPOLATION.getName().toString();
			final String bgColorName = AbstractGridFormat.BACKGROUND_COLOR.getName().toString();
			final String bandsListName = AbstractGridFormat.BANDS.getName().toString();
			int i = 0;
			boolean foundInterpolation = false;
			boolean foundGG = false;
			boolean foundBgColor = false;
			boolean foundBandIndices = false;
			for (; i < length; i++) {
				final String paramName = readParams[i].getDescriptor().getName().toString();
				if (paramName.equalsIgnoreCase(readGGName) && readGG != null) {
					((Parameter) readParams[i]).setValue(readGG);
					foundGG = true;
				} else if (paramName.equalsIgnoreCase(readInterpolationName)) {
					((Parameter) readParams[i]).setValue(interpolation);
					foundInterpolation = true;
				} else if (paramName.equalsIgnoreCase(bgColorName) && bgColor != null) {
					((Parameter) readParams[i]).setValue(bgColor);
					foundBgColor = true;
				} else if (paramName.equalsIgnoreCase(bandsListName) && bandIndices != null) {
					((Parameter) readParams[i]).setValue(bandIndices);
					foundBandIndices = true;
				}
			}

			if (!foundGG || !foundInterpolation || !(foundBgColor && bgColor != null) || !foundBandIndices) {
				List<GeneralParameterValue> paramList = new ArrayList<GeneralParameterValue>();
				paramList.addAll(Arrays.asList(readParams));
				if (!foundGG && readGG != null) {
					paramList.add(readGG);
				}
				if (!foundInterpolation) {
					paramList.add(readInterpolation);
				}
				if (!foundBgColor && bgColor != null) {
					paramList.add(bgColorParam);
				}
				if (!foundBandIndices && bandIndices != null) {
					paramList.add(bandIndicesParam);
				}
//				paramList.add(inputTransparentColor);
				readParams = paramList.toArray(new GeneralParameterValue[paramList.size()]);
			}
		} else {
			List<GeneralParameterValue> paramList = new ArrayList<>();
			if (readGG != null) {
				paramList.add(readGG);
			}
			if (bgColor != null) {
				paramList.add(bgColorParam);
			}
			if (bandIndices != null) {
				paramList.add(bandIndicesParam);
			}
			paramList.add(readInterpolation);
//			paramList.add(inputTransparentColor);
			readParams = paramList.toArray(new GeneralParameterValue[paramList.size()]);
		}
		return readParams;
	}

	/**
	 * 获取
	 * @param paintArea
	 * @param dataArea
	 * @param angle
	 * @return
	 */
	public static AffineTransform createRenderingTransform(Rectangle paintArea,
														   ReferencedEnvelope dataArea,
														   double angle) {
        AffineTransform tx;
        if (angle != 0.0) {
            tx = new AffineTransform();
            tx.translate(paintArea.width / 2, paintArea.height / 2);
            tx.rotate(Math.toRadians(angle));
            tx.translate(-paintArea.width / 2, -paintArea.height / 2);
            tx.concatenate(RendererUtilities.worldToScreenTransform(dataArea, paintArea));
        } else {
            tx = RendererUtilities.worldToScreenTransform(dataArea, paintArea);
        }
        return tx;
    }
}
