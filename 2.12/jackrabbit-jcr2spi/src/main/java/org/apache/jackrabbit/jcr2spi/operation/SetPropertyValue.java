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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;

/**
 * <code>SetPropertyValue</code>...
 */
public class SetPropertyValue extends TransientOperation {

    private static final int SET_PROPERTY_OPTIONS =
            ItemStateValidator.CHECK_LOCK
            | ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_CONSTRAINTS;
    
    private final PropertyId propertyId;
    private final PropertyState propertyState;
    private final QValue[] values;
    private final int valueType;

    private final QValue[] oldValues;

    private SetPropertyValue(PropertyState propertyState, int valueType, QValue[] values)
            throws RepositoryException {
        this(propertyState, valueType, values, SET_PROPERTY_OPTIONS);

    }

    private SetPropertyValue(PropertyState propertyState, int valueType,
                             QValue[] values, int options) throws RepositoryException {
        super(options);
        this.propertyState = propertyState;

        propertyId = (PropertyId) propertyState.getId();
        this.valueType = valueType;
        this.values = values;

        // remember original values
        oldValues = propertyState.getValues();

        addAffectedItemState(propertyState);
    }

    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * @see Operation#persisted()
     */
    public void persisted() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        try {
            propertyState.getHierarchyEntry().complete(this);
        } finally {
            // dispose the original values
            for (QValue v : oldValues) {
                v.discard();
            }
        }
    }

    /**
     * @see Operation#undo()
     */
    @Override
    public void undo() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_UNDO;
        propertyState.getHierarchyEntry().complete(this);

        // NOTE: new values don't need to be disposed as the transient change
        //       has been reverted with implicit value disposal.
    }

    //----------------------------------------< Access Operation Parameters >---
    public PropertyId getPropertyId() {
        return propertyId;
    }

    public PropertyState getPropertyState() {
        return propertyState;
    }

    public boolean isMultiValued() {
        return propertyState.isMultiValued();
    }

    public int getValueType() {
        return valueType;
    }

    public QValue[] getValues() {
        return values;
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(PropertyState propState, QValue[] qValues,
                                   int valueType) throws RepositoryException {
        // compact array (purge null entries)
        List<QValue> list = new ArrayList<QValue>();
        for (QValue qValue : qValues) {
            if (qValue != null) {
                list.add(qValue);
            }
        }
        QValue[] cleanValues = list.toArray(new QValue[list.size()]);
        SetPropertyValue sv = new SetPropertyValue(propState, valueType, cleanValues);
        return sv;
    }
}