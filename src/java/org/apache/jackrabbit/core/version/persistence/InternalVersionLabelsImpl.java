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
package org.apache.jackrabbit.core.version.persistence;

import org.apache.jackrabbit.core.version.InternalVersionItem;
import org.apache.jackrabbit.core.version.InternalVersionLabels;
import org.apache.jackrabbit.core.version.PersistentVersionManager;

/**
 * This Class represents a version labels
 */
public class InternalVersionLabelsImpl extends InternalVersionItemImpl implements InternalVersionLabels {

    private final InternalVersionItem parent;

    private final PersistentNode node;

    protected InternalVersionLabelsImpl(PersistentVersionManager vMgr,
                                        PersistentNode node,
                                        InternalVersionItem parent) {
        super(vMgr);
        this.node = node;
        this.parent = parent;
    }

    public InternalVersionItem getParent() {
        return parent;
    }

    public String getId() {
        return node.getUUID();
    }
}
