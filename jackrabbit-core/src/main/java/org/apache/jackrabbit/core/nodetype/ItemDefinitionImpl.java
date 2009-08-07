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

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.NamespaceException;

/**
 * This class implements the <code>ItemDefinition</code> interface.
 * All method calls are delegated to the wrapped {@link ItemDef},
 * performing the translation from <code>Name</code>s to JCR names
 * (and vice versa) where necessary.
 */
abstract class ItemDefinitionImpl implements ItemDefinition {

    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(ItemDefinitionImpl.class);

    /**
     * Literal for 'any name'.
     */
    protected static final String ANY_NAME = "*";

    /**
     * The node type manager of this session.
     */
    protected final NodeTypeManagerImpl ntMgr;

    /**
     * The name/path resolver used to translate qualified names to JCR names.
     */
    protected final NamePathResolver resolver;

    /**
     * The wrapped item definition.
     */
    protected final ItemDef itemDef;

    /**
     * Package private constructor
     *
     * @param itemDef    item definition
     * @param ntMgr      node type manager
     * @param resolver   name resolver
     */
    ItemDefinitionImpl(ItemDef itemDef, NodeTypeManagerImpl ntMgr,
                       NamePathResolver resolver) {
        this.itemDef = itemDef;
        this.ntMgr = ntMgr;
        this.resolver = resolver;
    }

    /**
     * Checks whether this is a residual item definition.
     *
     * @return <code>true</code> if this is a residual item definition
     */
    public boolean definesResidual() {
        return itemDef.definesResidual();
    }

    /**
     * Gets the <code>Name</code> of the child item. It is an error to
     * call this method if this is a residual item definition.
     *
     * @return the <code>Name</code> of the child item.
     * @see #getName()
     */
    public Name getQName() {
        return itemDef.getName();
    }

    //-------------------------------------------------------< ItemDefinition >
    /**
     * {@inheritDoc}
     */
    public NodeType getDeclaringNodeType() {
        if (ntMgr == null) {
            return null;
        }
        try {
            return ntMgr.getNodeType(itemDef.getDeclaringNodeType());
        } catch (NoSuchNodeTypeException e) {
            // should never get here
            log.error("declaring node type does not exist", e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        if (itemDef.definesResidual()) {
            return ANY_NAME;
        } else {
            try {
                return resolver.getJCRName(itemDef.getName());
            } catch (NamespaceException e) {
                // should never get here
                log.error("encountered unregistered namespace in item name",
                        e);
                // not correct, but an acceptable fallback
                return itemDef.getName().toString();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getOnParentVersion() {
        return itemDef.getOnParentVersion();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAutoCreated() {
        return itemDef.isAutoCreated();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMandatory() {
        return itemDef.isMandatory();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isProtected() {
        return itemDef.isProtected();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemDefinitionImpl)) {
            return false;
        }
        return itemDef.equals(((ItemDefinitionImpl) o).itemDef);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return itemDef.hashCode();
    }
}

