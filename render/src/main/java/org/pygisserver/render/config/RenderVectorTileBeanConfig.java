package org.pygisserver.render.config;

import javax.annotation.Resource;

import org.pygisserver.render.factory.RenderImageBuildFactory;
import org.pygisserver.render.service.PyRenderVectoreTile;
import org.pygisserver.render.vector.GeoJsonBuilderFactory;
import org.pygisserver.render.vector.MapBoxTileBuilderFactory;
import org.pygisserver.render.vector.TopoJSONBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RenderVectorTileBeanConfig {

	@Resource
	private RenderImageBuildFactory renderImageBuild;
	
	@Bean(name="mapbox")
	public PyRenderVectoreTile getMapBoxVectorTile() {
		return renderImageBuild.createVectorTile(MapBoxTileBuilderFactory.MIME_TYPE);
	}
	
	@Bean(name="geojson")
	public PyRenderVectoreTile getGeoJsonVectorTile() {
		return renderImageBuild.createVectorTile(GeoJsonBuilderFactory.MIME_TYPE);
	}
	
	@Bean(name="topjson")
	public PyRenderVectoreTile getTopJsonVectorTile() {
		return renderImageBuild.createVectorTile(TopoJSONBuilderFactory.MIME_TYPE);
	}
}
