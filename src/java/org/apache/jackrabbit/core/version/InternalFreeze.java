/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.QName;

/**
 * Abstract class that represents either a frozen child or a frozen versionable
 * node.
 */
public abstract class InternalFreeze {

    /** the parent 'freeze' */
    private final InternalFreeze parent;

    /**
     * Creates a new 'Freeze'
     * @param parent
     */
    protected InternalFreeze(InternalFreeze parent) {
        this.parent = parent;
    }

    /**
     * Returns the name of the frozen node
     *
     * @return
     */
    public abstract QName getName();

    /**
     * returns the version manager
     * @return
     */
    public PersistentVersionManager getVersionManager() {
        return parent==null ? null : parent.getVersionManager();
    }

    public InternalFreeze getParent() {
        return parent;
    }
}
