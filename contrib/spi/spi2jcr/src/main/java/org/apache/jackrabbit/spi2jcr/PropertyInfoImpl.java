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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.value.ValueFormat;

import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.Value;

/**
 * <code>PropertyInfoImpl</code> implements a <code>PropertyInfo</code> on top
 * of a JCR repository.
 */
class PropertyInfoImpl extends ItemInfoImpl implements PropertyInfo {

    /**
     * The property info of the underlying property.
     */
    private final PropertyId propertyId;

    /**
     * The type of the property.
     */
    private final int type;

    /**
     * The multiValued flag.
     */
    private final boolean isMultiValued;

    /**
     * The values of this property info.
     */
    private final QValue[] values;

    /**
     * Creates a new property info for the given <code>property</code>.
     *
     * @param property      the JCR property.
     * @param idFactory     the id factory.
     * @param nsResolver    the namespace resolver in use.
     * @param qValueFactory the QValue factory.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>property</code>.
     */
    public PropertyInfoImpl(Property property,
                            IdFactoryImpl idFactory,
                            NamespaceResolver nsResolver,
                            QValueFactory qValueFactory) throws RepositoryException {
        super(property, idFactory, nsResolver);
        this.propertyId = idFactory.createPropertyId(property, nsResolver);
        // TODO: build QValues upon (first) usage only.
        this.type = property.getType();
        this.isMultiValued = property.getDefinition().isMultiple();
        if (isMultiValued) {
            Value[] jcrValues = property.getValues();
            this.values = new QValue[jcrValues.length];
            for (int i = 0; i < jcrValues.length; i++) {
                this.values[i] = ValueFormat.getQValue(jcrValues[i],
                        nsResolver, qValueFactory);
            }
        } else {
            this.values = new QValue[]{
                ValueFormat.getQValue(property.getValue(), nsResolver, qValueFactory)};
        }
    }

    /**
     * {@inheritDoc}
     */
    public PropertyId getId() {
        return propertyId;
    }

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiValued() {
        return isMultiValued;
    }

    /**
     * {@inheritDoc}
     */
    public QValue[] getValues() {
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public boolean denotesNode() {
        return false;
    }
}
