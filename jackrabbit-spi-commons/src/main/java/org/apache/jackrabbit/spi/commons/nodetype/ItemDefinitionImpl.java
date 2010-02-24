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

import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.NamespaceException;

/**
 * This class implements the <code>ItemDefinition</code> interface.
 * All method calls are delegated to the wrapped {@link org.apache.jackrabbit.spi.QItemDefinition},
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
     * The namespace resolver used to translate <code>Name</code>s to JCR name strings.
     */
    protected final NamePathResolver resolver;

        /**
     * The node type manager of this session.
     */
    protected final AbstractNodeTypeManager ntMgr;

    /**
     * The wrapped item definition.
     */
    protected final QItemDefinition itemDef;

    /**
     * Package private constructor to create a definition that is based on
     * a template.
     *
     * @param itemDef    item definition
     * @param resolver
     */
    ItemDefinitionImpl(QItemDefinition itemDef, NamePathResolver resolver) {
        this(itemDef, null, resolver);
    }

    /**
     * Package private constructor to create a definition that is based on
     * an existing node type.
     *
     * @param itemDef
     * @param ntMgr
     * @param resolver
     */
    ItemDefinitionImpl(QItemDefinition itemDef, AbstractNodeTypeManager ntMgr, NamePathResolver resolver) {
        this.itemDef = itemDef;
        this.resolver = resolver;
        this.ntMgr = ntMgr;
    }

    //-----------------------------------------------------< ItemDefinition >---
    /**
     * {@inheritDoc}
     */
    public NodeType getDeclaringNodeType() {
        if (ntMgr == null) {
            // only a template
            return null;
        } else {
            try {
                return ntMgr.getNodeType(itemDef.getDeclaringNodeType());
            } catch (NoSuchNodeTypeException e) {
                // should never get here
                log.error("declaring node type does not exist", e);
                return null;
            }
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
                log.error("encountered unregistered namespace in property name", e);
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

    //-------------------------------------------------------------< Object >---
    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public int hashCode() {
        return itemDef.hashCode();
    }
}
