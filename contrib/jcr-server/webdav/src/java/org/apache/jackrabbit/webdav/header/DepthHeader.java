/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.webdav.header;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.DavConstants;

/**
 * <code>DepthHeader</code>...
 */
public class DepthHeader implements DavConstants {

    private static Logger log = Logger.getLogger(DepthHeader.class);

    private final int depth;

    /**
     * Create a new <code>DepthHeader</code> from the given integer.
     *
     * @param depth
     */
    public DepthHeader(int depth) {
	if (depth == DavConstants.DEPTH_0 || depth == DavConstants.DEPTH_1 || depth == DavConstants.DEPTH_INFINITY) {
	    this.depth = depth;
	} else {
	    throw new IllegalArgumentException("Invalid depth: " + depth);
	}
    }

    /**
     * @return integer representation of the depth indicated by the given header.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Return {@link DavConstants#HEADER_DEPTH Depth}
     *
     * @return {@link DavConstants#HEADER_DEPTH Depth}
     * @see DavConstants#HEADER_DEPTH
     */
    public String getHeaderName() {
	return DavConstants.HEADER_DEPTH;
    }

    /**
     * Returns the header value.
     *
     * @return header value
     */
    public String getHeaderValue() {
        if (depth == DavConstants.DEPTH_0 || depth == DavConstants.DEPTH_1) {
	    return depth + "";
	} else {
	    return DavConstants.DEPTH_INFINITY_S;
	}
    }

    /**
     * Parse the given header value or use the defaultValue if the header
     * string is empty or <code>null</code>.
     *
     * @param headerValue
     * @param defaultValue
     * @return a new DepthHeader
     */
    public static DepthHeader parse(String headerValue, int defaultValue) {
        if (headerValue == null || "".equals(headerValue)) {
	    return new DepthHeader(defaultValue);
	} else {
	    return new DepthHeader(depthToInt(headerValue));
	}
    }

    /**
     * Convert the String depth value to an integer.
     *
     * @param depth
     * @return integer representation of the given depth String
     * @throws IllegalArgumentException if the String does not represent a valid
     * depth.
     */
    private static int depthToInt(String depth) {
        int d;
	if (depth.equalsIgnoreCase(DavConstants.DEPTH_INFINITY_S)) {
	    d = DavConstants.DEPTH_INFINITY;
	} else if (depth.equals(DavConstants.DEPTH_0+"")) {
	    d = DavConstants.DEPTH_0;
	} else if (depth.equals(DavConstants.DEPTH_1+"")) {
	    d = DavConstants.DEPTH_1;
	} else {
	    throw new IllegalArgumentException("Invalid depth value: " + depth);
	}
        return d;
    }
}