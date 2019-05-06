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
package org.apache.jackrabbit.core;

import static org.apache.jackrabbit.core.ItemValidator.CHECK_CHECKED_OUT;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_CONSTRAINTS;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_HOLD;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_LOCK;
import static org.apache.jackrabbit.core.ItemValidator.CHECK_RETENTION;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionWriteOperation;

/**
 * Session operation for removing a given item, optionally with constraint
 * checks enabled.
 */
class ItemRemoveOperation implements SessionWriteOperation<Object> {

    /**
     * The item to be removed.
     */
    private final ItemImpl item;

    /**
     * Flag to enabled constraint checks
     */
    private final boolean checks;

    public ItemRemoveOperation(ItemImpl item, boolean checks) {
        this.item = item;
        this.checks = checks;
    }

    public Object perform(SessionContext context)
            throws RepositoryException {
        // check if this is the root node
        if (item.getDepth() == 0) {
            throw new RepositoryException("Cannot remove the root node");
        }

        NodeImpl parentNode = (NodeImpl) item.getParent();
        if (checks) {
            ItemValidator validator = context.getItemValidator();
            validator.checkRemove(
                    item,
                    CHECK_CONSTRAINTS | CHECK_HOLD | CHECK_RETENTION,
                    Permission.NONE);

            // Make sure the parent node is checked-out and
            // neither protected nor locked.
            validator.checkModify(
                    parentNode,
                    CHECK_LOCK | CHECK_CHECKED_OUT | CHECK_CONSTRAINTS,
                    Permission.NONE);
        }

        // delegate the removal of the child item to the parent node
        if (item.isNode()) {
            parentNode.removeChildNode((NodeId) item.getId());
        } else {
            parentNode.removeChildProperty(item.getPrimaryPath().getName());
        }

        return this;
    }


    //--------------------------------------------------------------< Object >

    /**
     * Returns a string representation of this operation.
     */
    public String toString() {
        return "item.remove()";
    }

}