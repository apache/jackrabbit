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

import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.value.QValue;

import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import javax.jcr.ValueFormatException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>AddProperty</code>...
 */
public class AddProperty extends AbstractOperation {

    private final NodeId parentId;
    private final QName propertyName;
    private final int propertyType;
    private final QValue[] values;

    private final QPropertyDefinition definition;

    private AddProperty(PropertyId newPropertyId, int propertyType, QValue[] values, QPropertyDefinition definition) {
        this.parentId = newPropertyId.getParentId();
        this.propertyName = newPropertyId.getQName();
        this.propertyType = propertyType;
        this.values = values;
        this.definition = definition;
        addAffectedItemId(parentId);
        addAffectedItemId(newPropertyId);
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
    public NodeId getParentId() {
        return parentId;
    }

    public QName getPropertyName() {
        return propertyName;
    }

    public int getPropertyType() {
        return propertyType;
    }

    public QValue[] getValues() {
        return values;
    }

    public boolean isMultiValued() {
        return definition.isMultiple();
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(PropertyId newPropertyId, int propertyType,
                                   QPropertyDefinition def, QValue[] values) {
        AddProperty ap = new AddProperty(newPropertyId, propertyType, values, def);
        return ap;
    }
}