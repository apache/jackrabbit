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

import javax.jcr.nodetype.ItemDef;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;

/**
 * An <code>ItemDefImpl</code> ...
 */
abstract class ItemDefImpl implements ItemDef {

    private static Logger log = Logger.getLogger(ItemDefImpl.class);

    protected static final String ANY_NAME = "*";

    protected final NodeTypeManagerImpl ntMgr;
    // namespace resolver used to translate qualified names to JCR names
    protected final NamespaceResolver nsResolver;

    private final ChildItemDef itemDef;

    /**
     * Package private constructor
     *
     * @param itemDef    item definition
     * @param ntMgr      node type manager
     * @param nsResolver namespace resolver
     */
    ItemDefImpl(ChildItemDef itemDef, NodeTypeManagerImpl ntMgr, NamespaceResolver nsResolver) {
        this.itemDef = itemDef;
        this.ntMgr = ntMgr;
        this.nsResolver = nsResolver;
    }

    public QName getQName() {
        return itemDef.getName();
    }

    //--------------------------------------------------------------< ItemDef >
    /**
     * @see ItemDef#getDeclaringNodeType
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
     * @see ItemDef#getName
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
     * @see ItemDef#getOnParentVersion()
     */
    public int getOnParentVersion() {
        return itemDef.getOnParentVersion();
    }

    /**
     * @see ItemDef#isAutoCreate
     */
    public boolean isAutoCreate() {
        return itemDef.isAutoCreate();
    }

    /**
     * @see ItemDef#isMandatory
     */
    public boolean isMandatory() {
        return itemDef.isMandatory();
    }

    /**
     * @see ItemDef#isProtected
     */
    public boolean isProtected() {
        return itemDef.isProtected();
    }
}

