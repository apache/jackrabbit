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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.jcr.property.ValuesProperty;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.value.ValueFormat;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.SessionInfo;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

/**
 * <code>PropertyInfoImpl</code>...
 */
public class PropertyInfoImpl extends ItemInfoImpl implements PropertyInfo {

    private static Logger log = LoggerFactory.getLogger(PropertyInfoImpl.class);

    private final PropertyId id;

    private int type;
    private boolean isMultiValued;
    private Object[] values;

    public PropertyInfoImpl(MultiStatusResponse response, URIResolver uriResolver,
                            NamespaceResolver nsResolver, SessionInfo sessionInfo,
                            ValueFactory valueFactory)
        throws RepositoryException, DavException {
        super(response, uriResolver, sessionInfo);

        id = uriResolver.getPropertyId(getParentId(), response);

        DavPropertySet propSet = response.getProperties(DavServletResponse.SC_OK);
        String typeName = propSet.get(ItemResourceConstants.JCR_TYPE).getValue().toString();
        type = PropertyType.valueFromName(typeName);

        if (propSet.contains(ItemResourceConstants.JCR_VALUE)) {
            // TODO: jcr-server sends jcr values not qualified
            ValuesProperty vp = new ValuesProperty(propSet.get(ItemResourceConstants.JCR_VALUE), type, valueFactory);
            Value jcrValue = vp.getJcrValue(type, valueFactory);
            if (type == PropertyType.BINARY) {
                values = (jcrValue == null) ?  new InputStream[0] : new InputStream[] {jcrValue.getStream()};
            } else {
                String vStr = (jcrValue == null) ? "" : ValueFormat.getQValue(jcrValue, nsResolver).getString();
                values = new String[] {vStr};
            }
        } else {
            isMultiValued = true;
            ValuesProperty vp = new ValuesProperty(propSet.get(ItemResourceConstants.JCR_VALUES), type, valueFactory);
            Value[] jcrValues = vp.getJcrValues(type, valueFactory);
            if (type == PropertyType.BINARY) {
                values = new InputStream[jcrValues.length];
                for (int i = 0; i < jcrValues.length; i++) {
                    values[i] = jcrValues[i].getStream();
                }
            } else {
                values = new String[jcrValues.length];
                for (int i = 0; i < jcrValues.length; i++) {
                    values[i] = ValueFormat.getQValue(jcrValues[i], nsResolver);
                }
            }
        }
    }

    public boolean denotesNode() {
        return false;
    }

    public PropertyId getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public boolean isMultiValued() {
        return isMultiValued;
    }

    public String[] getValues() {
        if (values instanceof InputStream[]) {
            // TODO
            throw new UnsupportedOperationException("use getValueAsStream");
        } else {
            return (String[])values;
        }
    }

    public InputStream[] getValuesAsStream() {
        if (values instanceof InputStream[]) {
            return (InputStream[]) values;
        } else {
            InputStream[] ins = new InputStream[values.length];
            for (int i = 0; i < values.length; i++) {
                String v = (String)values[i];
                try {
                    ins[i] = (v != null) ? new ByteArrayInputStream(v.getBytes("UTF-8")) : null;
                } catch (UnsupportedEncodingException e) {
                    log.error("Error while converting values", e);
                }
            }
            return ins;
        }
    }
}