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

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.jcr.property.ValuesProperty;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.value.ValueFormat;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import java.io.IOException;

/**
 * <code>PropertyInfoImpl</code>...
 */
public class PropertyInfoImpl extends ItemInfoImpl implements PropertyInfo {

    private static Logger log = LoggerFactory.getLogger(PropertyInfoImpl.class);

    private final PropertyId id;

    private int type;
    private boolean isMultiValued;
    private QValue[] values;

    public PropertyInfoImpl(PropertyId id, NodeId parentId, DavPropertySet propSet,
                            NamespaceResolver nsResolver, ValueFactory valueFactory,
                            QValueFactory qValueFactory)
        throws RepositoryException, DavException, IOException, MalformedPathException {

        super(parentId, propSet, nsResolver);
        // set id
        this.id = id;

        // retrieve properties
        String typeName = propSet.get(ItemResourceConstants.JCR_TYPE).getValue().toString();
        type = PropertyType.valueFromName(typeName);

        // values from jcr-server must be converted to qualified values.
        if (propSet.contains(ItemResourceConstants.JCR_VALUE)) {
            ValuesProperty vp = new ValuesProperty(propSet.get(ItemResourceConstants.JCR_VALUE), type, valueFactory);
            Value jcrValue = vp.getJcrValue(type, valueFactory);
            if (jcrValue == null) {
                // TODO: should never occur. since 'null' single values are not allowed. rather throw?
                values = QValue.EMPTY_ARRAY;
            } else {
                QValue qv;
                if (type == PropertyType.BINARY) {
                    qv = qValueFactory.create(jcrValue.getStream());
                } else {
                    qv = ValueFormat.getQValue(jcrValue, nsResolver, qValueFactory);
                }
                values = new QValue[] {qv};
            }
        } else {
            isMultiValued = true;
            ValuesProperty vp = new ValuesProperty(propSet.get(ItemResourceConstants.JCR_VALUES), type, valueFactory);
            Value[] jcrValues = vp.getJcrValues(type, valueFactory);
            values = new QValue[jcrValues.length];
            for (int i = 0; i < jcrValues.length; i++) {
                if (type == PropertyType.BINARY) {
                    values[i] = qValueFactory.create(jcrValues[i].getStream());
                } else {
                    values[i] = ValueFormat.getQValue(jcrValues[i], nsResolver, qValueFactory);
                }
            }
        }
    }

    //-----------------------------------------------------------< ItemInfo >---
    public boolean denotesNode() {
        return false;
    }

    public QName getQName() {
        return id.getQName();
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
