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
package org.apache.jackrabbit.commons.webdav;

import javax.jcr.PropertyType;
import java.util.HashMap;
import java.util.Map;

/** <code>JcrValueType</code>... */
public class JcrValueType {

    /**
     * Fragment for build the content type of request entities representing
     * a JCR-value. The fragment must be completed as follows:
     * <pre>
     * jcr-value/ + Value.getType().toLowerCase()
     * </pre>
     *
     * resulting in the following types:
     * <pre>
     * jcr-value/binary
     * jcr-value/boolean
     * jcr-value/date
     * jcr-value/decimal
     * jcr-value/double
     * jcr-value/long
     * jcr-value/name
     * jcr-value/path
     * jcr-value/reference
     * jcr-value/string
     * jcr-value/undefined
     * jcr-value/uri
     * jcr-value/weakreference
     * </pre>
     */
    private static final String VALUE_CONTENT_TYPE_FRAGMENT = "jcr-value/";

    /**
     * Hardcoded lookup from content type as created by {@link #contentTypeFromType(int)}.
     * Reason: As of JCR 2.0 there is no trivial rule to obtain the the TYPENAME
     * constant from a lower-cased string: WeakReference uses camel-case and URI
     * is all upper case...
     */
    private static final Map<String, Integer> TYPE_LOOKUP = new HashMap<String,Integer>();
    static {
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.BINARY), PropertyType.BINARY);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.BOOLEAN), PropertyType.BOOLEAN);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.DATE), PropertyType.DATE);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.DECIMAL), PropertyType.DECIMAL);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.DOUBLE), PropertyType.DOUBLE);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.LONG), PropertyType.LONG);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.NAME), PropertyType.NAME);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.PATH), PropertyType.PATH);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.REFERENCE), PropertyType.REFERENCE);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.STRING), PropertyType.STRING);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.UNDEFINED), PropertyType.UNDEFINED);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.URI), PropertyType.URI);
        TYPE_LOOKUP.put(contentTypeFromType(PropertyType.WEAKREFERENCE), PropertyType.WEAKREFERENCE);
    }

    public static String contentTypeFromType(int propertyType) {
        return VALUE_CONTENT_TYPE_FRAGMENT + PropertyType.nameFromValue(propertyType).toLowerCase();
    }

    public static int typeFromContentType(String contentType) {
        if (contentType != null) {
            // remove charset if present
            int pos = contentType.indexOf(';');
            String ct = (pos == -1) ? contentType : contentType.substring(0, pos);
            if (TYPE_LOOKUP.containsKey(ct)) {
                return TYPE_LOOKUP.get(ct);
            }
        }

        // some invalid content type argument that does not match any of the
        // strings created by contentTypeFromType(int propertyType)
        // -> Fallback to UNDEFINED
        return PropertyType.UNDEFINED;
    }
}