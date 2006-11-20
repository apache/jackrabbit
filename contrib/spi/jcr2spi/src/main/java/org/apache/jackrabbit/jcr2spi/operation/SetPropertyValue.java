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
import org.apache.jackrabbit.value.QValue;

import javax.jcr.version.VersionException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>SetPropertyValue</code>...
 */
public class SetPropertyValue extends AbstractOperation {

    private final PropertyState propertyState;
    private final QValue[] values;
    private final int propertyType;

    private SetPropertyValue(PropertyState propertyState, int propertyType, QValue[] values) {
        this.propertyState = propertyState;
        this.propertyType = propertyType;
        this.values = values;

        addAffectedItemState(propertyState);
    }

    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        visitor.visit(this);
    }

    /**
     * Throws UnsupportedOperationException
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        throw new UnsupportedOperationException("persisted() not implemented for transient modification.");
    }

    //----------------------------------------< Access Operation Parameters >---
    public PropertyState getPropertyState() {
        return propertyState;
    }

    public int getPropertyType() {
        return propertyType;
    }

    public QValue[] getValues() {
        return values;
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(PropertyState propState, QValue[] qValues,
                                   int valueType) {
        // compact array (purge null entries)
        List list = new ArrayList();
        for (int i = 0; i < qValues.length; i++) {
            if (qValues[i] != null) {
                list.add(qValues[i]);
            }
        }
        QValue[] cleanValues = (QValue[]) list.toArray(new QValue[list.size()]);
        SetPropertyValue sv = new SetPropertyValue(propState, valueType, cleanValues);
        return sv;
    }
}