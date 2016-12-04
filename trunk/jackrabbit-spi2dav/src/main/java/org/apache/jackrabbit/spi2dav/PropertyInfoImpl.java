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

import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.commons.webdav.ValueUtil;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertySet;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import java.io.IOException;

/**
 * <code>PropertyInfoImpl</code>...
 */
public class PropertyInfoImpl extends ItemInfoImpl implements PropertyInfo {

    private final PropertyId id;

    private int type;
    private boolean isMultiValued;
    private QValue[] values;

    public PropertyInfoImpl(PropertyId id, Path path, int type,
                            boolean isMultiValued, QValue[] values) {
        super(path);
        this.id = id;
        this.type = type;
        this.isMultiValued = isMultiValued;
        this.values = values;
    }

    public PropertyInfoImpl(PropertyId id, DavPropertySet propSet,
                            NamePathResolver resolver, ValueFactory valueFactory,
                            QValueFactory qValueFactory)
            throws RepositoryException, IOException, NameException {

        super(propSet, resolver);
        // set id
        this.id = id;

        // retrieve properties
        String typeName = propSet.get(JcrRemotingConstants.JCR_TYPE_LN, ItemResourceConstants.NAMESPACE).getValue().toString();
        type = PropertyType.valueFromName(typeName);

        // values from jcr-server must be converted to SPI values.
        DavProperty<?> prop = propSet.get(JcrRemotingConstants.JCR_VALUE_LN, ItemResourceConstants.NAMESPACE);
        if (prop != null) {
            Value[] jcrValues = ValueUtil.valuesFromXml(prop.getValue(), type, valueFactory);
            if (jcrValues == null || jcrValues.length == 0) {
                // TODO: should never occur. since 'null' single values are not allowed. rather throw?
                values = QValue.EMPTY_ARRAY;
            } else {
                QValue qv;
                if (type == PropertyType.BINARY) {
                    qv = qValueFactory.create(jcrValues[0].getStream());
                } else {
                    qv = ValueFormat.getQValue(jcrValues[0], resolver, qValueFactory);
                }
                values = new QValue[] {qv};
            }
        } else {
            isMultiValued = true;
            prop = propSet.get(JcrRemotingConstants.JCR_VALUES_LN, ItemResourceConstants.NAMESPACE);
            if (prop == null) {
                throw new RepositoryException("Item with id " + id.toString() + " doesn't represent a valid property.");
            }
            Value[] jcrValues = ValueUtil.valuesFromXml(prop.getValue(), type, valueFactory);
            values = new QValue[jcrValues.length];
            for (int i = 0; i < jcrValues.length; i++) {
                if (type == PropertyType.BINARY) {
                    values[i] = qValueFactory.create(jcrValues[i].getStream());
                } else {
                    values[i] = ValueFormat.getQValue(jcrValues[i], resolver, qValueFactory);
                }
            }
        }
    }

    //-----------------------------------------------------------< ItemInfo >---
    public boolean denotesNode() {
        return false;
    }

    //-------------------------------------------------------< PropertyInfo >---
    public PropertyId getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public boolean isMultiValued() {
        return isMultiValued;
    }

    public QValue[] getValues() {
        return values;
    }
}
