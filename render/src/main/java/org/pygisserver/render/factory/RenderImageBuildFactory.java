package org.pygisserver.render.factory;

import org.pygisserver.render.service.PyRenderImage;
import org.pygisserver.render.service.PyRenderVectoreTile;
import org.pygisserver.render.service.impl.PyRenderVectoreTileImpl;
import org.pygisserver.render.vector.GeoJsonBuilderFactory;
import org.pygisserver.render.vector.MapBoxTileBuilderFactory;
import org.pygisserver.render.vector.TopoJSONBuilderFactory;
import org.springframework.stereotype.Component;

/**
 * @Description: 
 * @author:  
 * @date: 2021-03-23
 */
@Component
public class RenderImageBuildFactory implements RenderImageBuild {

	@Override
	public PyRenderImage createRenderImage() {
		return null;
	}

	@Override
	public PyRenderVectoreTile createVectorTile(String mineType) {
		
		switch (mineType) {
		case MapBoxTileBuilderFactory.MIME_TYPE:
			return new PyRenderVectoreTileImpl(new MapBoxTileBuilderFactory(),true,2.0,true);
		case GeoJsonBuilderFactory.MIME_TYPE:
			return new PyRenderVectoreTileImpl(new GeoJsonBuilderFactory(),true,2.0,false);
		case TopoJSONBuilderFactory.MIME_TYPE:
			return new PyRenderVectoreTileImpl(new TopoJSONBuilderFactory(),true,2.0,true);
		default:
			break;
		}
		return null;
	}
}
