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
package org.apache.jackrabbit.jcr2spi.operation;

import org.apache.jackrabbit.jcr2spi.state.PropertyState;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.value.QValue;

import javax.jcr.version.VersionException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>SetPropertyValue</code>...
 */
public class SetPropertyValue extends AbstractOperation {

    private final PropertyId propertyId;
    private final QValue[] values;
    private final int propertyType;
    private final boolean isMultiValued;

    private SetPropertyValue(PropertyId propertyId, int propertyType, QValue[] values, boolean isMultiValued) {
        this.propertyId = propertyId;
        this.propertyType = propertyType;
        this.values = values;
        this.isMultiValued = isMultiValued;
        addAffectedItemId(propertyId);
    }

    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     */
    public void accept(OperationVisitor visitor) throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        visitor.visit(this);
    }

    //----------------------------------------< Access Operation Parameters >---
    public PropertyId getPropertyId() {
        return propertyId;
    }

    public int getPropertyType() {
        return propertyType;
    }

    public QValue[] getValues() {
        return values;
    }

    public boolean isMultiValued() {
        return isMultiValued;
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(PropertyState propState, QValue[] iva,
                                   int valueType) {
        SetPropertyValue sv = new SetPropertyValue(propState.getPropertyId(), valueType, iva, propState.isMultiValued());
        return sv;
    }
}