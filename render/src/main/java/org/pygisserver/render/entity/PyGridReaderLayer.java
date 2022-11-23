package org.pygisserver.render.entity;

import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.map.GridReaderLayer;
import org.geotools.styling.Style;
import org.opengis.parameter.GeneralParameterValue;

/**
 * @Description: pygisserver grid图层 
 * @author: 
 * @date: 2021-03-17
 */
public class PyGridReaderLayer extends GridReaderLayer {
	
	public PyGridReaderLayer(GridCoverage2DReader reader, Style style) {
        super(reader, style);
    }

    public PyGridReaderLayer(GridCoverage2DReader reader, Style style, String title) {
        super(reader, style, title);
    }

    public PyGridReaderLayer(GridCoverage2DReader reader, Style style, GeneralParameterValue[] params) {
        super(reader, style, params);
    }

    public PyGridReaderLayer(GridCoverage2DReader reader,
            				 Style style,
            				 String title,
            				 GeneralParameterValue[] params) {
        super(reader, style, title, params);
    }
    
    @Override
    public void dispose() {
        this.reader = null;
        this.style = null;
        this.params = null;
        super.dispose();
    }
}
