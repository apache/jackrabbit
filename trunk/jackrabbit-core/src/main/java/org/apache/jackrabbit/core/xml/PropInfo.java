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
package org.apache.jackrabbit.core.xml;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * Information about a property being imported. This class is used
 * by the XML import handlers to pass the parsed property information
 * through the {@link Importer} interface to the actual import process.
 * <p>
 * In addition to carrying the actual property data, instances of this
 * class also know how to apply that data when imported either to a
 * {@link NodeImpl} instance through a session or directly to a
 * {@link NodeState} instance in a workspace.
 */
public class PropInfo {

    /**
     * Name of the property being imported.
     */
    private final Name name;

    /**
     * Type of the property being imported.
     */
    private final int type;

    /**
     * Value(s) of the property being imported.
     */
    private final TextValue[] values;

    /**
     * Hint indicating whether the property is multi- or single-value
     */
    public enum MultipleStatus { UNKNOWN, SINGLE, MULTIPLE }
    private MultipleStatus multipleStatus;

    /**
     * Creates a property information instance.
     *
     * @param name name of the property being imported
     * @param type type of the property being imported
     * @param values value(s) of the property being imported
     */
    public PropInfo(Name name, int type, TextValue[] values) {
        this.name = name;
        this.type = type;
        this.values = values;
        multipleStatus =
                values.length == 1
                        ? MultipleStatus.UNKNOWN : MultipleStatus.MULTIPLE;
    }

    /**
     * Creates a property information instance.
     *
     * @param name name of the property being imported
     * @param type type of the property being imported
     * @param values value(s) of the property being imported
     * @param multipleStatus Hint indicating whether the property is
     *                       multi- or single-value
     */
    public PropInfo(Name name, int type, TextValue[] values,
                    MultipleStatus multipleStatus) {
        this.name = name;
        this.type = type;
        this.values = values;
        this.multipleStatus = multipleStatus;
    }

    /**
     * Disposes all values contained in this property.
     */
    public void dispose() {
        for (TextValue value : values) {
            value.dispose();
        }
    }

    public int getTargetType(QPropertyDefinition def) {
        int target = def.getRequiredType();
        if (target != PropertyType.UNDEFINED) {
            return target;
        } else if (type != PropertyType.UNDEFINED) {
            return type;
        } else {
            return PropertyType.STRING;
        }
    }

    public QPropertyDefinition getApplicablePropertyDef(EffectiveNodeType ent)
            throws ConstraintViolationException {
        if (multipleStatus == MultipleStatus.MULTIPLE) {
            return ent.getApplicablePropertyDef(name, type, true);
        } else if (multipleStatus == MultipleStatus.SINGLE) {
            return ent.getApplicablePropertyDef(name, type, false);
        } else {
            // multipleStatus == MultipleStatus.UNKNOWN
            if (values.length == 1) {
                // one value => could be single- or multi-valued
                return ent.getApplicablePropertyDef(name, type);
            } else {
                // zero or more than one values => must be multi-valued
                return ent.getApplicablePropertyDef(name, type, true);
            }
        }
    }

    public Name getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public TextValue[] getTextValues() {
        return values;        
    }

    public Value[] getValues(int targetType, NamePathResolver resolver) throws RepositoryException {
        Value[] va = new Value[values.length];
        for (int i = 0; i < values.length; i++) {
            va[i] = values[i].getValue(targetType, resolver);
        }
        return va;
    }
}
