package org.apache.jackrabbit.ocm.manager.objectconverter.impl;

import java.io.Serializable;

/**
 * Interface implemented by lazy loading proxies
 * 
 * @author <a href="mailto:slandelle@excilys.com">Stephane LANDELLE</a>
 */
public interface OcmProxy extends Serializable {

	/**
	 * Check is the proxy has been loaded
	 *
	 * @return true is the proxy has been loaded
	 */
	boolean isInitialized();

	/**
	 * Force proxy fetching
	 */
	void fetch();
}
