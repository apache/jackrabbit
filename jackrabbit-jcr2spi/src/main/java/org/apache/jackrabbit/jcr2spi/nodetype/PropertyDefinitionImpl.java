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
package org.apache.jackrabbit.jcr2spi.nodetype;

import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.spi.commons.nodetype.ValueConstraint;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * This class implements the <code>PropertyDefinition</code> interface.
 * All method calls are delegated to the wrapped {@link QPropertyDefinition},
 * performing the translation from <code>Name</code>s to JCR names
 * (and vice versa) where necessary.
 */
public class PropertyDefinitionImpl extends ItemDefinitionImpl implements PropertyDefinition {

    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(PropertyDefinitionImpl.class);

    private final ValueFactory valueFactory;

    /**
     * Package private constructor
     *
     * @param propDef    property definition
     * @param ntMgr      node type manager
     * @param resolver
     */
    PropertyDefinitionImpl(QPropertyDefinition propDef, NodeTypeManagerImpl ntMgr,
                           NamePathResolver resolver, ValueFactory valueFactory) {
        super(propDef, ntMgr, resolver);
        this.valueFactory = valueFactory;
    }

    //---------------------------------------------------< PropertyDefinition >
    /**
     * {@inheritDoc}
     */
    public Value[] getDefaultValues() {
        QPropertyDefinition pDef = ((QPropertyDefinition) itemDef);
        QValue[] defVals = pDef.getDefaultValues();
        if (defVals == null) {
            return null;
        }

        Value[] values = new Value[defVals.length];
        for (int i = 0; i < defVals.length; i++) {
            try {
                values[i] = ValueFormat.getJCRValue(defVals[i], resolver, valueFactory);
            } catch (RepositoryException e) {
                // should never get here
                String propName = (getName() == null) ? "[null]" : getName();
                log.error("illegal default value specified for property " + propName + " in node type " + getDeclaringNodeType(), e);
                return null;
            }
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public int getRequiredType() {
        return ((QPropertyDefinition) itemDef).getRequiredType();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getValueConstraints() {
        QPropertyDefinition pd = (QPropertyDefinition)itemDef;
        String[] constraints = pd.getValueConstraints();
        if (constraints == null || constraints.length == 0) {
            return new String[0];
        }
        try {
            String[] vca = new String[constraints.length];
            for (int i = 0; i < constraints.length; i++) {
                ValueConstraint constr = ValueConstraint.create(pd.getRequiredType(), constraints[i]);
                vca[i] = constr.getDefinition(resolver);
            }
            return vca;
        } catch (InvalidConstraintException e) {
            log.error("Invalid value constraint: " + e.getMessage());
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() {
        return ((QPropertyDefinition) itemDef).isMultiple();
    }
}


