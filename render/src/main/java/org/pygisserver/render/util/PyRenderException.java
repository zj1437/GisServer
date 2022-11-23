package org.pygisserver.render.util;

import java.awt.geom.NoninvertibleTransformException;

import org.geotools.renderer.RenderListener;
import org.geotools.renderer.lite.StreamingRenderer;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class PyRenderException implements RenderListener {

	private StreamingRenderer renderer;
	
	private Exception renderException;
	
	public PyRenderException(StreamingRenderer renderer) {
		this.renderer = renderer;
	}
	
	public boolean exceptionOccurred() {
        return renderException != null;
    }
	
	@Override
	public void featureRenderer(SimpleFeature feature) {
		// TODO Auto-generated method stub
		
	}

	//监控发生渲染异常时，将异常记录
	@Override
	public void errorOccurred(Exception renderException) {
		 Throwable cause = renderException;
	        while (cause != null) {
	            if (cause instanceof TransformException
	                    || cause instanceof IllegalAttributeException
	                    || cause instanceof FactoryException
	                    || cause instanceof NoninvertibleTransformException) {

                    log.info("Ignoring renderer error", renderException);
	                return;
	            }
	            cause = cause.getCause();
	        }
	        log.error( "Got an unexpected render exception {}", renderException);
	        this.renderException = renderException;
	        renderer.stopRendering();
	}

}
