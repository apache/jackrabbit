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
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

import java.io.Serializable;

/**
 * <code>PropertyInfoImpl</code> implements a serializable
 * <code>PropertyInfo</code> based on another property info.
 */
public class PropertyInfoImpl extends ItemInfoImpl implements PropertyInfo {

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
     * Creates a new serializable property info for the given
     * <code>PropertyInfo</code>.
     *
     * @param propertyInfo
     */
    public static PropertyInfo createSerializablePropertyInfo(
            PropertyInfo propertyInfo, IdFactory idFactory) {
        if (propertyInfo instanceof Serializable) {
            return propertyInfo;
        } else {
            NodeId parentId = propertyInfo.getId().getParentId();
            parentId = idFactory.createNodeId(
                    parentId.getUniqueID(), parentId.getPath());
            PropertyId propId = idFactory.createPropertyId(
                    parentId, propertyInfo.getId().getName());
            return new PropertyInfoImpl(propertyInfo.getPath(),
                    propId, propertyInfo.getType(),
                    propertyInfo.isMultiValued(), propertyInfo.getValues());
        }
    }

    /**
     * Creates a new property info for the given parameters.
     *
     * @param parentId      the parent id.
     * @param name          the name of this property.
     * @param path          the path to this property.
     * @param id            the id of this property.
     * @param type          the type of this property.
     * @param isMultiValued whether this property is multi-valued.
     * @param values        the values.
     * @deprecated Use {@link #PropertyInfoImpl(Path, PropertyId, int, boolean, QValue[])}
     * instead. The parentId is not used any more.
     */
    public PropertyInfoImpl(NodeId parentId, Name name, Path path,
                            PropertyId id, int type, boolean isMultiValued,
                            QValue[] values) {
        this(path, id, type, isMultiValued, values);
    }

    /**
     * Creates a new property info for the given parameters.
     *
     * @param path          the path to this property.
     * @param id            the id of this property.
     * @param type          the type of this property.
     * @param isMultiValued whether this property is multi-valued.
     * @param values        the values.
     */
    public PropertyInfoImpl(Path path, PropertyId id, int type,
                            boolean isMultiValued, QValue[] values) {
        super(path, false);
        this.propertyId = id;
        this.type = type;
        this.isMultiValued = isMultiValued;
        this.values = values;
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
        QValue[] vals = new QValue[values.length];
        System.arraycopy(values, 0, vals, 0, values.length);
        return vals;
    }
}
