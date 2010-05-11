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

import javax.jcr.ItemExistsException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.core.BatchedItemOperations;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Logger instance.
     */
    private static Logger log = LoggerFactory.getLogger(PropInfo.class);

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
     * Creates a proprety information instance.
     *
     * @param name name of the property being imported
     * @param type type of the property being imported
     * @param values value(s) of the property being imported
     */
    public PropInfo(Name name, int type, TextValue[] values) {
        this.name = name;
        this.type = type;
        this.values = values;
    }

    /**
     * Disposes all values contained in this property.
     */
    public void dispose() {
        for (int i = 0; i < values.length; i++) {
            values[i].dispose();
        }
    }

    private int getTargetType(PropDef def) {
        int target = def.getRequiredType();
        if (target != PropertyType.UNDEFINED) {
            return target;
        } else if (type != PropertyType.UNDEFINED) {
            return type;
        } else {
            return PropertyType.STRING;
        }
    }

    private PropDef getApplicablePropertyDef(EffectiveNodeType ent)
            throws ConstraintViolationException {
        if (values.length == 1) {
            // could be single- or multi-valued (n == 1)
            return ent.getApplicablePropertyDef(name, type);
        } else {
            // can only be multi-valued (n == 0 || n > 1)
            return ent.getApplicablePropertyDef(name, type, true);
        }
    }

    public void apply(
            NodeImpl node, NamePathResolver resolver,
            ReferenceChangeTracker refTracker) throws RepositoryException {
        // find applicable definition
        PropDef def = getApplicablePropertyDef(node.getEffectiveNodeType());
        if (def.isProtected()) {
            // skip protected property
            log.debug("skipping protected property " + name);
            return;
        }

        // convert serialized values to Value objects
        Value[] va = new Value[values.length];
        int targetType = getTargetType(def);
        for (int i = 0; i < values.length; i++) {
            va[i] = values[i].getValue(targetType, resolver);
        }

        // multi- or single-valued property?
        if (va.length == 1 && !def.isMultiple()) {
            Exception e = null;
            try {
                // set single-value
                node.setProperty(name, va[0]);
            } catch (ValueFormatException vfe) {
                e = vfe;
            } catch (ConstraintViolationException cve) {
                e = cve;
            }
            if (e != null) {
                // setting single-value failed, try setting value array
                // as a last resort (in case there are ambiguous property
                // definitions)
                node.setProperty(name, va, type);
            }
        } else {
            // can only be multi-valued (n == 0 || n > 1)
            node.setProperty(name, va, type);
        }
        if (type == PropertyType.REFERENCE) {
            // store reference for later resolution
            refTracker.processedReference(node.getProperty(name));
        }
    }

    public void apply(
            NodeState node, BatchedItemOperations itemOps,
            NodeTypeRegistry ntReg, ReferenceChangeTracker refTracker)
            throws RepositoryException {
        PropertyState prop = null;
        PropDef def = null;

        if (node.hasPropertyName(name)) {
            // a property with that name already exists...
            PropertyId idExisting = new PropertyId(node.getNodeId(), name);
            prop = (PropertyState) itemOps.getItemState(idExisting);
            def = itemOps.findApplicablePropertyDefinition(
                    prop.getName(), prop.getType(), prop.isMultiValued(), node);
            if (def.isProtected()) {
                // skip protected property
                log.debug("skipping protected property "
                        + itemOps.safeGetJCRPath(idExisting));
                return;
            }
            if (!def.isAutoCreated()
                    || (prop.getType() != type && type != PropertyType.UNDEFINED)
                    || def.isMultiple() != prop.isMultiValued()) {
                throw new ItemExistsException(itemOps.safeGetJCRPath(prop.getPropertyId()));
            }
        } else {
            // there's no property with that name,
            // find applicable definition
            def = getApplicablePropertyDef(itemOps.getEffectiveNodeType(node));
            if (def.isProtected()) {
                // skip protected property
                log.debug("skipping protected property " + name);
                return;
            }

            // create new property
            prop = itemOps.createPropertyState(node, name, type, def);
        }

        // check multi-valued characteristic
        if (values.length != 1 && !def.isMultiple()) {
            throw new ConstraintViolationException(itemOps.safeGetJCRPath(prop.getPropertyId())
                    + " is not multi-valued");
        }

        // convert serialized values to InternalValue objects
        int targetType = getTargetType(def);
        InternalValue[] iva = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            iva[i] = values[i].getInternalValue(targetType);
        }

        // set values
        prop.setValues(iva);

        // make sure property is valid according to its definition
        itemOps.validate(prop);

        if (prop.getType() == PropertyType.REFERENCE) {
            // store reference for later resolution
            refTracker.processedReference(prop);
        }

        // store property
        itemOps.store(prop);
    }

}
