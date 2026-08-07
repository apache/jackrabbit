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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.client5.http.entity.mime.FormBodyPart;
import org.apache.hc.client5.http.entity.mime.FormBodyPartBuilder;
import org.apache.hc.client5.http.entity.mime.InputStreamBody;
import org.apache.hc.client5.http.entity.mime.MimeField;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.jackrabbit.commons.json.JsonUtil;
import org.apache.jackrabbit.commons.webdav.JcrValueType;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

final class Utils {

    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final ContentType DEFAULT_TYPE = ContentType.create("text/plain", DEFAULT_CHARSET);

    private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    private static final String TRANSFER_ENCODING_TEXT = "8bit";
    private static final String TRANSFER_ENCODING_BINARY = "binary";

    private Utils() {};

    /**
     * HttpClient 5 no longer derives a Content-Transfer-Encoding header from the body,
     * whereas HttpClient 4 emitted 8bit for string bodies and binary for stream bodies.
     * Adding it after the part is built keeps it in the same position as before, i.e.
     * after Content-Disposition and Content-Type.
     */
    private static FormBodyPart withTransferEncoding(FormBodyPart part, String encoding) {
        part.getHeader().addField(new MimeField(CONTENT_TRANSFER_ENCODING, encoding));
        return part;
    }

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
    static void addPart(String paramName, String value, List<FormBodyPart> parts) {
        parts.add(withTransferEncoding(
                FormBodyPartBuilder.create().setName(paramName).setBody(new StringBody(value, DEFAULT_TYPE)).build(),
                TRANSFER_ENCODING_TEXT));
    }

    /**
     *
     * @param paramName
     * @param value
     * @param resolver
     * @throws RepositoryException
     */
    static void addPart(String paramName, QValue value, NamePathResolver resolver, List<FormBodyPart> parts, List<QValue> binaries) throws RepositoryException {
        FormBodyPartBuilder builder = FormBodyPartBuilder.create().setName(paramName);
        ContentType ctype = ContentType.create(JcrValueType.contentTypeFromType(value.getType()), DEFAULT_CHARSET);

        FormBodyPart part;
        switch (value.getType()) {
            case PropertyType.BINARY:
                binaries.add(value);
                // server detects binaries based on presence of filename parameters (JCR-4154)
                part = withTransferEncoding(
                        builder.setBody(new InputStreamBody(value.getStream(), ctype, paramName)).build(),
                        TRANSFER_ENCODING_BINARY);
                break;
            case PropertyType.NAME:
                part = withTransferEncoding(
                        builder.setBody(new StringBody(resolver.getJCRName(value.getName()), ctype)).build(), TRANSFER_ENCODING_TEXT);
                break;
            case PropertyType.PATH:
                part = withTransferEncoding(
                        builder.setBody(new StringBody(resolver.getJCRPath(value.getPath()), ctype)).build(), TRANSFER_ENCODING_TEXT);
                break;
            default:
                part = withTransferEncoding(
                        builder.setBody(new StringBody(value.getString(), ctype)).build(), TRANSFER_ENCODING_TEXT);
        }

        parts.add(part);
    }

    static void removeParts(String paramName, List<FormBodyPart> parts) {
        for (Iterator<FormBodyPart> it = parts.iterator(); it.hasNext();) {
            FormBodyPart part = it.next();
            if (part.getName().equals(paramName)) {
                it.remove();
                if (part.getBody() instanceof InputStreamBody) {
                    try {
                        ((InputStreamBody) part.getBody()).getInputStream().close();
                    } catch (IOException ex) {
                        // best effort
                    }
                }
            }
        }
    }
}