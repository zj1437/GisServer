package org.pygisserver.pytc.configuration;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;

public class PytcMappingJackson2HttpMessageConvert extends MappingJackson2HttpMessageConverter {

	public PytcMappingJackson2HttpMessageConvert() {
		List<MediaType> medisTypes = new ArrayList<MediaType>();
		medisTypes.add(MediaType.TEXT_HTML);
		setSupportedMediaTypes(medisTypes);
	}
}
