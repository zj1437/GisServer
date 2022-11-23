package org.pygisserver.render.entity;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;
import org.geotools.styling.RasterSymbolizer;
import org.opengis.filter.expression.Expression;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import lombok.Data;

@Data
public class PyRenderedParam {
	
	private CoordinateReferenceSystem crs;
	
	private ReferencedEnvelope envelope;
	
	private Rectangle rectangle;
	
	private AffineTransform affineTransform;
	
	private GridCoverage2DReader reader;
	
	private RasterSymbolizer symbolizer;
	
	private GeneralParameterValue[] generalParam;
	
	private Expression transformation;
	
	private MapContent mapcontent;
	
	/**
	 * 渲染插值算法
	 */
	private String interpolation;
	
	private int renderWidth;
	
	private int renderHeight;
	
	/**
	 * 是否支持透明
	 */
	private boolean transparent = true;
	
	/**
	 * 渲染格式
	 */
	private String format;
	
	/**
	 * 渲染背景色
	 */
	private Color bgcolor = Color.white;
	
	/**
	 * 抗锯齿设置()
	 */
	private String antialias;
	
	/**
	 * 旋转角度
	 */
	private double angle;
	
	public PyRenderedParam(boolean transparent, 
						   String format, 
						   Color bgcolor, 
						   String antialias, 
						   double angle,
						   int mapWidth,
						   int mapHeight) {
		super();
		this.transparent = transparent;
		this.format = format;
		this.bgcolor = bgcolor;
		this.antialias = antialias;
		this.angle = angle;
		this.renderWidth = mapWidth == 0 ? 256 : mapWidth;
		this.renderHeight = mapHeight == 0 ? 256 : mapHeight;
	}
	
	
	public PyRenderedParam() {
	}
	
}
