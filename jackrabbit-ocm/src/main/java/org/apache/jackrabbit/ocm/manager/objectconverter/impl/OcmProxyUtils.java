/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.ocm.manager.objectconverter.impl;

/**
 * Utils class for proxy nandling
 * 
 * @author <a href="mailto:slandelle@excilys.com">Stephane LANDELLE</a>
 */
public abstract class OcmProxyUtils {

	/**
	 * Check if an object is an OCM proxy
	 * 
	 * @param object
	 *            the Object to check
	 * @return true is the object is an OCM proxy
	 */
	public static boolean isProxy(Object object) {
		return object instanceof OcmProxy;
	}

	/**
	 * Check is an Object is not an unitialized OCM proxy
	 * @see OcmProxy.isInitialized()
	 * 
	 * @param object
	 *            the Object to check
	 * @return true if the object is not an OCM proxy or if it has already been
	 *         initialized
	 */
	public static boolean isInitialized(Object object) {
		if (!isProxy(object)) {
			return true;

		} else {
			return ((OcmProxy) object).isInitialized();
		}
	}

	/**
	 * Force fetching of an abject
	 *
	 * @param <T> the type of the object to fetch
	 * @param object the object to fetch
	 * @return the fetched object
	 */
	public static <T> T fetch(T object) {

		if (isProxy(object)) {
			((OcmProxy) object).fetch();
		}
		return object;
	}
}
