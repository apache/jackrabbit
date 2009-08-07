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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * This class implements the <code>PropertyDefinition</code> interface.
 * All method calls are delegated to the wrapped {@link PropDef},
 * performing the translation from <code>Name</code>s to JCR names
 * (and vice versa) where necessary.
 */
public class PropertyDefinitionImpl extends ItemDefinitionImpl
        implements PropertyDefinition {

    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(PropertyDefinitionImpl.class);

    /**
     * Package private constructor
     *
     * @param propDef    property definition
     * @param ntMgr      node type manager
     * @param resolver   name resolver
     */
    PropertyDefinitionImpl(PropDef propDef, NodeTypeManagerImpl ntMgr,
                           NamePathResolver resolver) {
        super(propDef, ntMgr, resolver);
    }

    /**
     * Returns the wrapped property definition.
     *
     * @return the wrapped property definition.
     */
    public PropDef unwrap() {
        return (PropDef) itemDef;
    }

    //---------------------------------------------------< PropertyDefinition >
    /**
     * {@inheritDoc}
     */
    public Value[] getDefaultValues() {
        InternalValue[] defVals = ((PropDef) itemDef).getDefaultValues();
        if (defVals == null) {
            return null;
        }
        Value[] values = new Value[defVals.length];
        for (int i = 0; i < defVals.length; i++) {
            try {
                values[i] = defVals[i].toJCRValue(resolver);
            } catch (RepositoryException re) {
                // should never get here
                String propName = (getName() == null) ? "[null]" : getName();
                log.error("illegal default value specified for property "
                        + propName + " in node type " + getDeclaringNodeType(),
                        re);
                return null;
            }
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public int getRequiredType() {
        return ((PropDef) itemDef).getRequiredType();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getValueConstraints() {
        ValueConstraint[] constraints = ((PropDef) itemDef).getValueConstraints();
        if (constraints == null || constraints.length == 0) {
            return new String[0];
        }
        String[] vca = new String[constraints.length];
        for (int i = 0; i < constraints.length; i++) {
            vca[i] = constraints[i].getDefinition(resolver);
        }
        return vca;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() {
        return ((PropDef) itemDef).isMultiple();
    }
}

