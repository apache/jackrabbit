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

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.QValue;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * <code>PropertyInfoImpl</code>...
 */
public class PropertyInfoImpl extends ItemInfoImpl implements PropertyInfo {

    private final boolean multiValued;

    private PropertyId id;
    private int propertyType;
    private QValue[] values = QValue.EMPTY_ARRAY;

    public PropertyInfoImpl(PropertyId id, Path path, int propertyType,
                            QValue value) throws RepositoryException {
        super(path, false);
        this.id = id;
        this.propertyType = propertyType;
        multiValued = false;
        values = new QValue[]{value};
    }

    public PropertyInfoImpl(PropertyId id, Path path, int propertyType, QValue[] values) throws RepositoryException {
        super(path, false);
        this.id = id;
        this.propertyType = propertyType;
        this.values = values;
        multiValued = true;
    }

    //-------------------------------------------------------< PropertyInfo >---
    public PropertyId getId() {
        return id;
    }

    public int getType() {
        if (propertyType == PropertyType.UNDEFINED) {
            // in case of empty-value-array of a multivalued property the type
            // must always be set.
            propertyType = getValues()[0].getType();
        }
        return propertyType;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public QValue[] getValues() {
        return values;
    }

    //--------------------------------------------------------------------------
    void setId(PropertyId id) {
        this.id = id;
    }

    void checkCompleted() throws RepositoryException {
        if (id == null) {
            throw new RepositoryException("Incomplete PropertyInfo: id missing.");
        }
        if (propertyType == PropertyType.UNDEFINED) {
            throw new RepositoryException("Incomplete PropertyInfo: missing type of property.");
        }
    }
}
