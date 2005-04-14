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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.QName;
import org.apache.log4j.Logger;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;

/**
 * An <code>ItemDefinitionImpl</code> ...
 */
abstract class ItemDefinitionImpl implements ItemDefinition {

    private static Logger log = Logger.getLogger(ItemDefinitionImpl.class);

    protected static final String ANY_NAME = "*";

    protected final NodeTypeManagerImpl ntMgr;
    // namespace resolver used to translate qualified names to JCR names
    protected final NamespaceResolver nsResolver;

    private final ItemDef itemDef;

    /**
     * Package private constructor
     *
     * @param itemDef    item definition
     * @param ntMgr      node type manager
     * @param nsResolver namespace resolver
     */
    ItemDefinitionImpl(ItemDef itemDef, NodeTypeManagerImpl ntMgr,
                       NamespaceResolver nsResolver) {
        this.itemDef = itemDef;
        this.ntMgr = ntMgr;
        this.nsResolver = nsResolver;
    }

    public QName getQName() {
        return itemDef.getName();
    }

    //--------------------------------------------------------------< ItemDef >
    /**
     * {@inheritDoc}
     */
    public NodeType getDeclaringNodeType() {
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
                return itemDef.getName().toJCRName(nsResolver);
            } catch (NoPrefixDeclaredException npde) {
                // should never get here
                log.error("encountered unregistered namespace in property name", npde);
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
}

