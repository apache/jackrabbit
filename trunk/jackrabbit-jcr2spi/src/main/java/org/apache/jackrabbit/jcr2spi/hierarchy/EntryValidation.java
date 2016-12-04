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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.jcr2spi.state.Status;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import java.util.Iterator;

/**
 * <code>EntryValidation</code>...
 */
final class EntryValidation {

    private static Logger log = LoggerFactory.getLogger(EntryValidation.class);

    /**
     * Returns <code>true</code> if the collection of child node
     * <code>entries</code> contains at least one valid <code>NodeEntry</code>.
     *
     * @param nodeEntries Iterator of NodeEntries to check.
     * @return <code>true</code> if one of the entries is valid; otherwise
     *         <code>false</code>.
     */
    static boolean containsValidNodeEntry(Iterator<NodeEntry> nodeEntries) {
        boolean hasValid = false;
        while (nodeEntries.hasNext() && !hasValid) {
            NodeEntry cne = nodeEntries.next();
            hasValid = isValidNodeEntry(cne);
        }
        return hasValid;
    }

    /**
     * Returns <code>true</code> if the given childnode entry is not
     * <code>null</code> and resolves to a NodeState, that is valid or if the
     * childnode entry has not been resolved up to now (assuming the corresponding
     * nodestate is still valid).
     *
     * @param cne NodeEntry to check.
     * @return <code>true</code> if the given entry is valid.
     */
    static boolean isValidNodeEntry(NodeEntry cne) {
        // shortcut.
        if (cne == null) {
            return false;
        }
        boolean isValid = false;
        if (cne.isAvailable()) {
            try {
                isValid = cne.getNodeState().isValid();
            } catch (ItemNotFoundException e) {
                // may occur if the cached state is marked 'INVALIDATED' and
                // does not exist any more on the persistent layer -> invalid.
            } catch (RepositoryException e) {
                // should not occur, if the cne is available.
            }
        } else {
            // assume entry is valid
            // TODO: check if this assumption is correct
            isValid = true;
        }

        return isValid;
    }

    /**
     * Returns <code>true</code> if the given childnode entry is not
     * <code>null</code> and resolves to a NodeState, that is neither NEW
     * nor REMOVED.
     *
     * @param cne NodeEntry to check.
     * @return <code>true</code> if the given entry is valid.
     */
    static boolean isValidWorkspaceNodeEntry(NodeEntry cne) {
        // shortcut.
        if (cne == null) {
            return false;
        }
        int status = cne.getStatus();
        return status != Status.NEW && status != Status.REMOVED;
    }

    /**
     * Returns <code>true</code> if the given childproperty entry is not
     * <code>null</code> and resolves to a PropertyState, that is valid or if the
     * childproperty entry has not been resolved up to now (assuming the corresponding
     * PropertyState is still valid).
     *
     * @param cpe PropertyEntry to check.
     * @return <code>true</code> if the given entry is valid.
     */
    static boolean isValidPropertyEntry(PropertyEntry cpe) {
        if (cpe == null) {
            return false;
        }
        boolean isValid = false;
        if (cpe.isAvailable()) {
            try {
                isValid = cpe.getPropertyState().isValid();
            } catch (ItemNotFoundException e) {
                // may occur if the cached state is marked 'INVALIDATED' and
                // does not exist any more on the persistent layer -> invalid.
            } catch (RepositoryException e) {
                // probably removed in the meantime. should not occur.
            }
        } else {
            // assume entry is valid // TODO check if this assumption is correct.
            isValid = true;
        }
        return isValid;
    }
}