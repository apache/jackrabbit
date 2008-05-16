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

import org.apache.jackrabbit.api.jsr283.nodetype.NodeTypeDefinition;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * This class implements the <code>NodeTypeDefinition</code> interface.
 * All method calls are delegated to the wrapped {@link NodeTypeDef},
 * performing the translation from <code>Name</code>s to JCR names
 * (and vice versa) where necessary.
 */
public class NodeTypeDefinitionImpl implements NodeTypeDefinition {

    private static Logger log = LoggerFactory.getLogger(NodeTypeDefinitionImpl.class);

    private final NodeTypeDef ntd;
    // resolver used to translate qualified names to JCR names
    private final NamePathResolver resolver;

    public NodeTypeDefinitionImpl(NodeTypeDef ntd, NamePathResolver resolver) {
        this.ntd = ntd;
        this.resolver = resolver;
    }

    //---------------------------------------------------< NodeTypeDefinition >
    /**
     * {@inheritDoc}
     */
    public String getName() {
        try {
            return resolver.getJCRName(ntd.getName());
        } catch (NamespaceException e) {
            // should never get here
            log.error("encountered unregistered namespace in node type name", e);
            return ntd.getName().toString();
        }
    }

    /**
     * Returns the names of the supertypes actually declared in this node type.
     * <p/>
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return an array
     * containing a single string indicating the node type
     * <code>nt:base</code>.
     *
     * @return an array of <code>String</code>s
     * @since JCR 2.0
     */
    public String[] getDeclaredSupertypeNames() {
        Name[] ntNames = ntd.getSupertypes();
        String[] supertypes = new String[ntNames.length];
        for (int i = 0; i < ntNames.length; i++) {
            try {
                supertypes[i] = resolver.getJCRName(ntd.getName());
            } catch (NamespaceException e) {
                // should never get here
                log.error("encountered unregistered namespace in node type name", e);
                supertypes[i] = ntd.getName().toString();
            }
        }
        return supertypes;
    }

    /**
     * Returns <code>true</code> if this is an abstract node type; returns
     * <code>false</code> otherwise.
     * <p/>
     * An abstract node type is one that cannot be assigned as the primary or
     * mixin type of a node but can be used in the definitions of other node
     * types as a superclass.
     * <p/>
     * In implementations that support node type registration, if this
     * <code>NodeTypeDefinition</code> object is actually a newly-created empty
     * <code>NodeTypeTemplate</code>, then this method will return
     * <code>false</code>.
     *
     * @return a <code>boolean</code>
     * @since JCR 2.0
     */
    public boolean isAbstract() {
        return ntd.isAbstract();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMixin() {
        return ntd.isMixin();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasOrderableChildNodes() {
        return ntd.hasOrderableChildNodes();
    }

    /**
     * {@inheritDoc}
     */
    public String getPrimaryItemName() {
        try {
            Name piName = ntd.getPrimaryItemName();
            if (piName != null) {
                return resolver.getJCRName(piName);
            } else {
                return null;
            }
        } catch (NamespaceException e) {
            // should never get here
            log.error("encountered unregistered namespace in name of primary item", e);
            return ntd.getName().toString();
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        NodeDef[] cnda = ntd.getChildNodeDefs();
        NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
        for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = new NodeDefinitionImpl(cnda[i], null, resolver);
        }
        return nodeDefs;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        PropDef[] pda = ntd.getPropertyDefs();
        PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
        for (int i = 0; i < pda.length; i++) {
            propDefs[i] = new PropertyDefinitionImpl(pda[i], null, resolver);
        }
        return propDefs;
    }
}
