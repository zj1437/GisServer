package org.pygisserver.web.service;

@FunctionalInterface
public interface FileExtensionFilter<T> {

	public boolean accept(T t);
}
