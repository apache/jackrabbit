/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.lite.nodetype;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDef;

import org.apache.jackrabbit.name.Name;

/**
 * TODO
 */
public class LitePropertyDef extends LiteItemDef implements PropertyDef {

    private final int requiredType;

    private final boolean multiple;

    private final List constraints;

    private final List defaults;

    public LitePropertyDef(
            Session session, Name name, NodeType declaringNodeType,
            int onParentVersion, boolean autoCreate,
            boolean mandatory, boolean isProtected,
            int requiredType, boolean multiple) {
        super(session, name, declaringNodeType,
                onParentVersion, autoCreate, mandatory, isProtected);
        this.requiredType = requiredType;
        this.multiple = multiple;
        this.constraints = new LinkedList();
        this.defaults = new LinkedList();
    }

    /** {@inheritDoc} */
    public int getRequiredType() {
        return requiredType;
    }

    /** {@inheritDoc} */
    public boolean isMultiple() {
        return multiple;
    }

    /** {@inheritDoc} */
    public String[] getValueConstraints() {
        return (String[]) constraints.toArray(new String[0]);
    }

    public void addValueConstraint(String constraint) {
        constraints.add(constraint);
    }

    /** {@inheritDoc} */
    public Value[] getDefaultValues() {
        return (Value[]) defaults.toArray(new Value[0]);
    }

    public void addDefaultValue(Value value) {
        defaults.add(value);
    }
}
