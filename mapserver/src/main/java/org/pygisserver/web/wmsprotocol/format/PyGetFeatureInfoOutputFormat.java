package org.pygisserver.web.wmsprotocol.format;

import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.pygisserver.web.wmsprotocol.entity.PyGetFeatureInfoRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

public abstract class PyGetFeatureInfoOutputFormat {

    protected static final Logger LOGGER = Logging.getLogger(PyGetFeatureInfoOutputFormat.class);

    private final String contentType;

    public PyGetFeatureInfoOutputFormat(final String contentType) {
        this.contentType = contentType;
    }

    public abstract void write(
            FeatureCollectionType results, PyGetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException;

    public boolean canProduce(String mapFormat) {
        return this.contentType.equalsIgnoreCase(mapFormat);
    }

    public String getContentType() {
        return contentType;
    }

    public String getCharset() {
        return null;
    }
}
