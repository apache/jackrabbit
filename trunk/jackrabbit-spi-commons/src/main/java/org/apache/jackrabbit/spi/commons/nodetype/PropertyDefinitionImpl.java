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
package org.apache.jackrabbit.spi.commons.nodetype;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the <code>PropertyDefinition</code> interface.
 * All method calls are delegated to the wrapped {@link QPropertyDefinition},
 * performing the translation from <code>Name</code>s to JCR names
 * (and vice versa) where necessary.
 */
public class PropertyDefinitionImpl extends ItemDefinitionImpl implements PropertyDefinition {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(PropertyDefinitionImpl.class);

    private final ValueFactory valueFactory;

    /**
     * Package private constructor
     *
     * @param propDef    property definition
     * @param resolver the name-path resolver
     * @param valueFactory a value factory
     */
    public PropertyDefinitionImpl(QPropertyDefinition propDef, NamePathResolver resolver,
                                  ValueFactory valueFactory) {
        this(propDef, null, resolver, valueFactory);
    }

    /**
     *
     * @param propDef underlying propdef
     * @param ntMgr nodetype manager
     * @param resolver name-path resolver
     * @param valueFactory value factory (for default values)
     */
    public PropertyDefinitionImpl(QPropertyDefinition propDef,
                                  AbstractNodeTypeManager ntMgr,
                                  NamePathResolver resolver,
                                  ValueFactory valueFactory) {
        super(propDef, ntMgr, resolver);
        this.valueFactory = valueFactory;
    }

    /**
     * Returns the wrapped property definition.
     *
     * @return the wrapped property definition.
     */
    public QPropertyDefinition unwrap() {
        return (QPropertyDefinition) itemDef;
    }

    //-------------------------------------------------< PropertyDefinition >---

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
        QPropertyDefinition pd = (QPropertyDefinition) itemDef;
        QValueConstraint[] constraints = pd.getValueConstraints();
        if (constraints == null || constraints.length == 0) {
            return new String[0];
        }
        String[] vca = new String[constraints.length];
        for (int i = 0; i < constraints.length; i++) {
            try {
                ValueConstraint vc = ValueConstraint.create(pd.getRequiredType(), constraints[i].getString());
                vca[i] = vc.getDefinition(resolver);
            } catch (InvalidConstraintException e) {
                log.warn("Internal error during conversion of constraint.", e);
                vca[i] = constraints[i].getString();
            }
        }
        return vca;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() {
        return ((QPropertyDefinition) itemDef).isMultiple();
    }

    /**
     * @see javax.jcr.nodetype.PropertyDefinition#getAvailableQueryOperators()
     */
    public String[] getAvailableQueryOperators() {
        return ((QPropertyDefinition) itemDef).getAvailableQueryOperators();
    }

    /**
     * @see javax.jcr.nodetype.PropertyDefinition#isFullTextSearchable()
     */
    public boolean isFullTextSearchable() {
        return ((QPropertyDefinition) itemDef).isFullTextSearchable();
    }

    /**
     * @see javax.jcr.nodetype.PropertyDefinition#isQueryOrderable()
     */
    public boolean isQueryOrderable() {
        return ((QPropertyDefinition) itemDef).isQueryOrderable();
    }
}