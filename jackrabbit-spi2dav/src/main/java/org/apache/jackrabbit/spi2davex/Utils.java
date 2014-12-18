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
package org.apache.jackrabbit.spi2davex;

import java.util.Iterator;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartBase;
import org.apache.jackrabbit.commons.webdav.JcrValueType;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.commons.json.JsonUtil;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

final class Utils {

    private static final String DEFAULT_CHARSET = "UTF-8";

    private Utils() {};

    static String getJsonKey(String str) {
        return JsonUtil.getJsonString(str) + ":";
    }

    static String getJsonString(QValue value) throws RepositoryException {
        String str;
        switch (value.getType()) {
            case PropertyType.STRING:
                str = JsonUtil.getJsonString(value.getString());
                break;
            case PropertyType.BOOLEAN:
            case PropertyType.LONG:
                str = value.getString();
                break;
            case PropertyType.DOUBLE:
                double d = value.getDouble();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    // JSON cannot specifically handle this property type...
                    str = null;
                } else {
                    str = value.getString();
                    if (str.indexOf('.') == -1) {
                        str += ".0";
                    }
                }
                break;
            default:
                // JSON cannot specifically handle this property type...
                str = null;
        }
        return str;
    }

    /**
     *
     * @param paramName
     * @param value
     */
    static void addPart(String paramName, String value, List<Part> parts) {
        parts.add(new StringPart(paramName, value, DEFAULT_CHARSET));
    }

    /**
     *
     * @param paramName
     * @param value
     * @param resolver
     * @throws RepositoryException
     */
    static void addPart(String paramName, QValue value, NamePathResolver resolver, List<Part> parts) throws RepositoryException {
        Part part;
        switch (value.getType()) {
            case PropertyType.BINARY:
                part = new BinaryPart(paramName, new BinaryPartSource(value), JcrValueType.contentTypeFromType(PropertyType.BINARY), DEFAULT_CHARSET);
                break;
            case PropertyType.NAME:
                part = new StringPart(paramName, resolver.getJCRName(value.getName()), DEFAULT_CHARSET);
                break;
            case PropertyType.PATH:
                part = new StringPart(paramName, resolver.getJCRPath(value.getPath()), DEFAULT_CHARSET);
                break;
            default:
                part = new StringPart(paramName, value.getString(), DEFAULT_CHARSET);
        }
        String ctype = JcrValueType.contentTypeFromType(value.getType());
        ((PartBase) part).setContentType(ctype);

        parts.add(part);
    }

    static void removeParts(String paramName, List<Part> parts) {
        for (Iterator<Part> it = parts.iterator(); it.hasNext();) {
            Part part = it.next();
            if (part.getName().equals(paramName)) {
                it.remove();
            }
        }
    }
}