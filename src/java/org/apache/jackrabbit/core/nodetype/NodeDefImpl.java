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

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.QName;

import java.util.Arrays;

/**
 * This class implements the ChildNodeDef interface. It basically holds the
 * respective attributes.
 */
public class NodeDefImpl extends ItemDefImpl implements NodeDef {

    /**
     * the name of the default primary type
     */
    private QName defaultPrimaryType = null;

    /**
     * the names of the required primary types
     */
    private QName[] requiredPrimaryTypes = new QName[]{Constants.NT_BASE};

    /**
     * the 'allowSameNameSibs' flag.
     */
    private boolean allowsSameNameSiblings = false;

    /**
     * the node def id. Set after 'compile'
     */
    private NodeDefId id;

    /**
     * {@inheritDoc}
     */
    public void setDeclaringNodeType(QName declaringNodeType) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. ChildNodeDef already compiled.");
        }
        super.setDeclaringNodeType(declaringNodeType);
    }

    /**
     * {@inheritDoc}
     */
    public void setName(QName name) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. ChildNodeDef already compiled.");
        }
        super.setName(name);
    }

    /**
     * {@inheritDoc}
     */
    public void setAutoCreated(boolean autoCreated) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. ChildNodeDef already compiled.");
        }
        super.setAutoCreated(autoCreated);
    }

    /**
     * {@inheritDoc}
     */
    public void setOnParentVersion(int onParentVersion) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. ChildNodeDef already compiled.");
        }
        super.setOnParentVersion(onParentVersion);
    }

    /**
     * {@inheritDoc}
     */
    public void setProtected(boolean writeProtected) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. ChildNodeDef already compiled.");
        }
        super.setProtected(writeProtected);
    }

    /**
     * {@inheritDoc}
     */
    public void setMandatory(boolean mandatory) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. ChildNodeDef already compiled.");
        }
        super.setMandatory(mandatory);
    }

    /**
     * Sets the name of default primary type
     *
     * @param defaultNodeType
     */
    public void setDefaultPrimaryType(QName defaultNodeType) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. ChildNodeDef already compiled.");
        }
        this.defaultPrimaryType = defaultNodeType;
    }

    /**
     * Sets the names of the required primary types
     *
     * @param requiredPrimaryTypes
     */
    public void setRequiredPrimaryTypes(QName[] requiredPrimaryTypes) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. ChildNodeDef already compiled.");
        }
        if (requiredPrimaryTypes == null) {
            throw new IllegalArgumentException("requiredPrimaryTypes can not be null");
        }
        this.requiredPrimaryTypes = requiredPrimaryTypes;
    }

    /**
     * sets the 'allowsSameNameSiblings' flag
     *
     * @param allowsSameNameSiblings
     */
    public void setAllowsSameNameSiblings(boolean allowsSameNameSiblings) {
        if (id != null) {
            throw new IllegalStateException("Unable to set attribute. ChildNodeDef already compiled.");
        }
        this.allowsSameNameSiblings = allowsSameNameSiblings;
    }

    /**
     * {@inheritDoc}
     */
    public QName getDefaultPrimaryType() {
        return defaultPrimaryType;
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getRequiredPrimaryTypes() {
        return requiredPrimaryTypes;
    }

    /**
     * {@inheritDoc}
     */
    public boolean allowsSameNameSiblings() {
        return allowsSameNameSiblings;
    }

    /**
     * {@inheritDoc}
     */
    public NodeDefId getId() {
        if (id == null) {
            id = new NodeDefId(this);
        }
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public boolean definesNode() {
        return true;
    }

    /**
     * checks if this child node def is equal to the given one.
     * @param obj the other object to compare to.
     * @return <code>true</code> if equal; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeDefImpl) {
            NodeDefImpl other = (NodeDefImpl) obj;
            return super.equals(obj)
                    && Arrays.equals(requiredPrimaryTypes, other.requiredPrimaryTypes)
                    && (defaultPrimaryType == null ? other.defaultPrimaryType == null : defaultPrimaryType.equals(other.defaultPrimaryType))
                    && allowsSameNameSiblings == other.allowsSameNameSiblings;
        }
        return false;
    }
}
