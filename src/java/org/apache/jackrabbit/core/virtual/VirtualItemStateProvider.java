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
package org.apache.jackrabbit.core.virtual;

import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.state.ItemStateProvider;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.ItemId;

/**
 * This Interface defines a virtual item state provider.
 */
public interface VirtualItemStateProvider extends ItemStateProvider {
    /**
     * Returns a predefined node definition id.
     *
     * @param nodename
     * @return
     */
    public NodeDefId getNodeDefId(QName nodename);

    /**
     * Returns a predefined property definition id
     *
     * @param propname
     * @return
     */
    public PropDefId getPropDefId(QName propname);

    public boolean isVirtualRoot(ItemId id);

    public NodeId getVirtualRootId();
}
