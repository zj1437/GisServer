package org.pygisserver.web.wmsprotocol.entity;

import net.opengis.wfs.FeatureCollectionType;
import org.pygisserver.common.util.SpringContextUtil;
import org.pygisserver.common.util.SpringUtils;
import org.pygisserver.web.wmsprotocol.format.PyGetFeatureInfoOutputFormat;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class PyGetFeatureInfoResponse extends Response {

    private PyGetFeatureInfoOutputFormat defaultOutputFormat;

    public PyGetFeatureInfoResponse(PyGetFeatureInfoOutputFormat defaultOutputFormat) {
        super(FeatureCollectionType.class);
        this.defaultOutputFormat = defaultOutputFormat;
    }


    @Override
    public boolean canHandle(Operation operation) {
        return "GetFeatureInfo".equalsIgnoreCase(operation.getId());
    }

    @Override
    public String getMimeType(final Object value, final Operation operation)
            throws ServiceException {
        Assert.notNull(value, "value is null");
        Assert.notNull(operation, "operation is null");
        Assert.isTrue(value instanceof FeatureCollectionType, "unrecognized result type:");

        PyGetFeatureInfoRequest request =
                OwsUtils.parameter(operation.getParameters(), PyGetFeatureInfoRequest.class);

        Assert.notNull(request, "request");

        PyGetFeatureInfoOutputFormat outputFormat = SpringUtils.getBean("feGetFeatureInfoOutputFormat");

        return outputFormat.getContentType();
    }

    @Override
    public void write(final Object value, final OutputStream output, final Operation operation)
            throws IOException, ServiceException {

        Assert.notNull(value, "value is null");
        Assert.notNull(operation, "operation is null");
        Assert.isTrue(value instanceof FeatureCollectionType, "unrecognized result type:");
        Assert.isTrue(
                operation.getParameters() != null
                        && operation.getParameters().length == 1
                        && operation.getParameters()[0] instanceof PyGetFeatureInfoRequest,
                "Operation parameters should be a single GetFeatureInfoRequest");

        PyGetFeatureInfoRequest request = (PyGetFeatureInfoRequest) operation.getParameters()[0];
        FeatureCollectionType results = (FeatureCollectionType) value;
        PyGetFeatureInfoOutputFormat outputFormat = getRequestedOutputFormat(request);
        outputFormat.write(results, request, output);
    }

    private PyGetFeatureInfoOutputFormat getRequestedOutputFormat(PyGetFeatureInfoRequest request)
            throws ServiceException {

        String requestFormat = request.getInfoFormat();

        PyGetFeatureInfoOutputFormat format = getFeatureInfoOutputFormat(requestFormat);
        if (format == null) {
            format = defaultOutputFormat;
        }

        return format;
    }

    private PyGetFeatureInfoOutputFormat getFeatureInfoOutputFormat(String requestFormat) {
        List<PyGetFeatureInfoOutputFormat> outputFormats;
        outputFormats = GeoServerExtensions.extensions(PyGetFeatureInfoOutputFormat.class, SpringContextUtil.getApplicationContext());

        for (PyGetFeatureInfoOutputFormat format : outputFormats) {
            if (format.canProduce(requestFormat)) {
                return format;
            }
        }
        return null;
    }


    @Override
    public String getCharset(Operation operation) {
        Assert.notNull(operation, "operation is null");
        PyGetFeatureInfoRequest request =
                OwsUtils.parameter(operation.getParameters(), PyGetFeatureInfoRequest.class);
        PyGetFeatureInfoOutputFormat outputFormat = SpringUtils.getBean("pyGetFeatureInfoOutputFormat");
        return outputFormat.getCharset();
    }
}
