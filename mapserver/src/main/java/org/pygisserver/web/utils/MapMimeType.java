package org.pygisserver.web.utils;

import org.geowebcache.mime.MimeType;

public class MapMimeType extends MimeType {

    public MapMimeType(String mimeType, String fileExtension, String internalName, String format,
                       boolean supportsTiling) {
        super(mimeType, fileExtension, internalName, format, supportsTiling);
    }
}
