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
package org.apache.jackrabbit.webdav.jcr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;

/** <code>JcrValueType</code>... */
public final class JcrValueType {

    private static Logger log = LoggerFactory.getLogger(JcrValueType.class);

    /**
     * Fragment for build the content type of request entities representing
     * a JCR-value. The fragment must be completed as follows:
     * <pre>
     * jcr-value/ + Value.getType().toLowerCase()
     * </pre>
     *
     * resulting in the following types:
     * <pre>
     * jcr-value/string
     * jcr-value/boolean
     * jcr-value/long
     * jcr-value/double
     * jcr-value/date
     * jcr-value/binary
     * jcr-value/date
     * jcr-value/name
     * jcr-value/path
     * </pre>
     */
    private static final String VALUE_CONTENT_TYPE_FRAGMENT = "jcr-value/";

    public static String contentTypeFromType(int propertyType) {
        return VALUE_CONTENT_TYPE_FRAGMENT + PropertyType.nameFromValue(propertyType).toLowerCase();
    }

    public static int typeFromContentType(String contentType) {
        if (contentType != null && contentType.startsWith(VALUE_CONTENT_TYPE_FRAGMENT)) {
            // no need to create value/values property. instead
            // prop-value can be retrieved directly:
            int pos = contentType.indexOf('/');
            int pos2 = contentType.indexOf(';');

            String typename = contentType.substring(pos+1, pos+2).toUpperCase() + contentType.substring(pos+2, (pos2 > -1) ? pos2 : contentType.length());
            return PropertyType.valueFromName(typename);
        }
        return PropertyType.UNDEFINED;
    }
}