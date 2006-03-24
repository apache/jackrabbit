/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state.orm;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.orm.ojb.ValuesToStringFieldConversion;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.ojb.broker.accesslayer.conversions.ConversionException;

import javax.jcr.PropertyType;
import java.io.Serializable;

/**
 * <p>This class represents a property state in an ORM-compatible format.</p>
 */
public class ORMPropertyState implements Serializable {
    private String values;
    private Integer type;
    private String definitionId;
    private Boolean multiValued;
    private String itemId;
    private String name;
    private String parentUUID;

    public ORMPropertyState() {
    }

    public ORMPropertyState(ItemId id) throws ItemStateException {
        if (id instanceof PropertyId) {
            PropertyId propId = (PropertyId) id;
            this.itemId = propId.toString();
            name = propId.getName().toString();
            parentUUID = propId.getParentId().getUUID().toString();
        } else {
            throw new ItemStateException("PropertyId expected, instead got " + id.getClass());
        }
    }

    public ORMPropertyState(PropertyState state) {
        fromPersistentPropertyState(state);
    }

    public void fromPersistentPropertyState(PropertyState state) throws
        ConversionException {
        this.itemId = state.getId().toString();
        name = state.getName().toString();
        parentUUID = state.getParentId().getUUID().toString();
        values = (String) new ValuesToStringFieldConversion().javaToSql(state.getValues());
        type = new Integer(state.getType());
        if (state.getDefinitionId() != null) {
            definitionId = state.getDefinitionId().toString();
        }
        multiValued = new Boolean(state.isMultiValued());
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValues(String values) {
        this.values = values;
    }

    public void setParentUUID(String parentUUID) {
        this.parentUUID = parentUUID;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public void setDefinitionId(String definitionId) {

        this.definitionId = definitionId;
    }

    public void setMultiValued(Boolean multiValued) {
        this.multiValued = multiValued;
    }

    public void setItemId(String itemId) {

        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public String getValues() {
        return values;
    }

    public String getParentUUID() {
        return parentUUID;
    }

    public Integer getType() {
        return type;
    }

    public String getDefinitionId() {

        return definitionId;
    }

    public Boolean getMultiValued() {
        return multiValued;
    }

    public String getItemId() {

        return itemId;
    }

    public void toPersistentPropertyState(PropertyState state) {
        if (getDefinitionId() != null) {
            state.setDefinitionId(PropDefId.valueOf(getDefinitionId()));
        }
        if (getType() != null) {
            state.setType(getType().intValue());
        }
        if (getType().intValue() != PropertyType.BINARY) {
            ValuesToStringFieldConversion vts = new
                ValuesToStringFieldConversion(getType().intValue());
            InternalValue[] values = (InternalValue[]) vts.sqlToJava(getValues());
            if (values.length > 1) {
                state.setMultiValued(true);
            } else {
                state.setMultiValued(multiValued.booleanValue());
            }
            state.setValues(values);
        } else {
            state.setMultiValued(multiValued.booleanValue());
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ORMPropertyState)) {
            return false;
        }
        ORMPropertyState right = (ORMPropertyState) obj;
        if (itemId.equals(right.getItemId())) {
            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return getItemId().hashCode();
    }
}
