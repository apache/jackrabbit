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
package org.apache.jackrabbit.lite.nodetype;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ItemDef;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.name.Name;

/**
 * TODO
 */
public class LiteItemDef implements ItemDef {

    private final Session session;

    private final Name name;

    private final NodeType declaringNodeType;

    private int onParentVersion;

    private boolean autoCreate;

    private boolean mandatory;

    private boolean isProtected;

    protected LiteItemDef(
            Session session, Name name, NodeType declaringNodeType,
            int onParentVersion, boolean autoCreate,
            boolean mandatory, boolean isProtected) {
        this.session = session;
        this.name = name;
        this.declaringNodeType = declaringNodeType;
        this.onParentVersion = onParentVersion;
        this.autoCreate = autoCreate;
        this.mandatory = mandatory;
        this.isProtected = isProtected;
    }

    public NodeType getDeclaringNodeType() {
        return declaringNodeType;
    }

    public String getName() {
        try {
            return name.toJCRName(session);
        } catch (RepositoryException e) {
            throw new IllegalStateException(
                    "Unexpected namespace problem: " + e.getMessage());
        }
    }

    public int getOnParentVersion() {
        return onParentVersion;
    }

    public boolean isAutoCreate() {
        return autoCreate;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public boolean isProtected() {
        return isProtected;
    }

}
