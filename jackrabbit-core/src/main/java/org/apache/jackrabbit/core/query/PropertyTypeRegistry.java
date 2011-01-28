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
package org.apache.jackrabbit.core.query;

import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistryListener;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The <code>PropertyTypeRegistry</code> keeps track of registered node type
 * definitions and its property types. It provides a fast type lookup for a
 * given property name.
 */
public class PropertyTypeRegistry implements NodeTypeRegistryListener {

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(PropertyTypeRegistry.class);

    /**
     * Empty <code>TypeMapping</code> array as return value if no type is
     * found
     */
    private static final TypeMapping[] EMPTY = new TypeMapping[0];

    /** The NodeTypeRegistry */
    private final NodeTypeRegistry registry;

    /** Property Name to TypeMapping[] mapping */
    private final Map<Name, TypeMapping[]> typeMapping = new HashMap<Name, TypeMapping[]>();

    /**
     * Creates a new <code>PropertyTypeRegistry</code> instance. This instance
     * is *not* registered as listener to the NodeTypeRegistry in the constructor!
     * @param reg the <code>NodeTypeRegistry</code> where to read the property
     * type information.
     */
    public PropertyTypeRegistry(NodeTypeRegistry reg) {
        this.registry = reg;
        fillCache();
    }

    /**
     * Returns an array of type mappings for a given property name
     * <code>propName</code>. If <code>propName</code> is not defined as a property
     * in any registered node type an empty array is returned.
     * @param propName the name of the property.
     * @return an array of <code>TypeMapping</code> instances.
     */
    public TypeMapping[] getPropertyTypes(Name propName) {
        synchronized (typeMapping) {
            TypeMapping[] types = typeMapping.get(propName);
            if (types != null) {
                return types;
            } else {
                return EMPTY;
            }
        }
    }

    public void nodeTypeRegistered(Name ntName) {
        try {
            QNodeTypeDefinition def = registry.getNodeTypeDef(ntName);
            QPropertyDefinition[] propDefs = def.getPropertyDefs();
            synchronized (typeMapping) {
                for (QPropertyDefinition propDef : propDefs) {
                    int type = propDef.getRequiredType();
                    if (!propDef.definesResidual() && type != PropertyType.UNDEFINED) {
                        Name name = propDef.getName();
                        // only remember defined property types
                        TypeMapping[] types = typeMapping.get(name);
                        if (types == null) {
                            types = new TypeMapping[1];
                        } else {
                            TypeMapping[] tmp = new TypeMapping[types.length + 1];
                            System.arraycopy(types, 0, tmp, 0, types.length);
                            types = tmp;
                        }
                        types[types.length - 1] = new TypeMapping(ntName, type, propDef.isMultiple());
                        typeMapping.put(name, types);
                    }
                }
            }
        } catch (NoSuchNodeTypeException e) {
            log.error("Unable to get newly registered node type definition for name: " + ntName);
        }
    }

    public void nodeTypeReRegistered(Name ntName) {
        nodeTypesUnregistered(Collections.singleton(ntName));
        nodeTypeRegistered(ntName);
    }

    public void nodeTypesUnregistered(Collection<Name> names) {
        // remove all TypeMapping instances referring to this ntName
        synchronized (typeMapping) {
            Map<Name, TypeMapping[]> modified = new HashMap<Name, TypeMapping[]>();
            for (Iterator<Name> it = typeMapping.keySet().iterator(); it.hasNext();) {
                Name propName = (Name) it.next();
                TypeMapping[] mapping = typeMapping.get(propName);
                List<TypeMapping> remove = null;
                for (TypeMapping tm : mapping) {
                    if (names.contains(tm.ntName)) {
                        if (remove == null) {
                            // not yet created
                            remove = new ArrayList<TypeMapping>(mapping.length);
                        }
                        remove.add(tm);
                    }
                }
                if (remove != null) {
                    it.remove();
                    if (mapping.length == remove.size()) {
                        // all removed -> done
                    } else {
                        // only some removed
                        List<TypeMapping> remaining = new ArrayList<TypeMapping>(Arrays.asList(mapping));
                        remaining.removeAll(remove);
                        modified.put(propName, remaining.toArray(new TypeMapping[remaining.size()]));
                    }
                }
            }
            // finally re-add the modified mappings
            typeMapping.putAll(modified);
        }
    }

    /**
     * Initially fills the cache of this registry with property type definitions
     * from the {@link org.apache.jackrabbit.core.nodetype.NodeTypeRegistry}.
     */
    private void fillCache() {
        for (Name ntName : registry.getRegisteredNodeTypes()) {
            nodeTypeRegistered(ntName);
        }
    }

    public static class TypeMapping {

        /** The property type as an integer */
        public final int type;

        /** The Name of the node type where this type mapping originated */
        final Name ntName;

        /** True if the property type is multi-valued */
        public final boolean isMultiValued;

        private TypeMapping(Name ntName, int type, boolean isMultiValued) {
            this.type = type;
            this.ntName = ntName;
            this.isMultiValued = isMultiValued;
        }
    }
}
