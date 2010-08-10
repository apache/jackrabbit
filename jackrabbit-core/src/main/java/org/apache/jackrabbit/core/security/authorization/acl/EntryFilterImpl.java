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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceException;
import javax.jcr.security.AccessControlEntry;
import java.security.acl.Group;
import java.util.Collection;
import java.util.List;

/**
 * <code>PrincipalEntryFilter</code>...
 */
class EntryFilterImpl implements EntryFilter {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(EntryFilterImpl.class);

    private final Collection<String> principalNames;
    private final Path path;
    private final PathResolver resolver;

    private String itemPath;

    EntryFilterImpl(Collection<String> principalNames, Path path, PathResolver resolver) {
        this.principalNames = principalNames;
        this.path = path;
        this.resolver = resolver;
    }

    /**
     * Separately collect the entries defined for the user and group
     * principals.
     *
     * @param entries
     * @param resultLists
     * @see EntryFilter#filterEntries(java.util.List, java.util.List[])
     */
    public void filterEntries(List<AccessControlEntry> entries, List<AccessControlEntry>... resultLists) {
        if (resultLists.length == 2) {
            List<AccessControlEntry> userAces = resultLists[0];
            List<AccessControlEntry> groupAces = resultLists[1];

            int uInsertIndex = userAces.size();
            int gInsertIndex = groupAces.size();

            // first collect aces present on the given aclNode.
            for (AccessControlEntry ace : entries) {
                // only process ace if 'principalName' is contained in the given set
                if (matches(ace)) {
                    // add it to the proper list (e.g. separated by principals)
                    /**
                     * NOTE: access control entries must be collected in reverse
                     * order in order to assert proper evaluation.
                     */
                    if (ace.getPrincipal() instanceof Group) {
                        groupAces.add(gInsertIndex, ace);
                    } else {
                        userAces.add(uInsertIndex, ace);
                    }
                }
            }
        } else {
            log.warn("Filtering aborted. Expected 2 result lists.");
        }
    }

    private boolean matches(AccessControlEntry ace) {
        if (principalNames == null || principalNames.contains(ace.getPrincipal().getName())) {
            if (((ACLTemplate.Entry) ace).getRestrictions().isEmpty()) {
                // short cut: there is no glob-restriction -> the entry matches
                // because it is either defined on the node or inherited.
                return true;
            } else {
                // there is a glob-restriction: check if the target path matches
                // this entry.
                try {
                    return ((ACLTemplate.Entry) ace).matches(getPath());
                } catch (NamespaceException e) {
                    log.error("Cannot determine ACE match.", e);
                }
            }
        }

        // doesn't match this filter -> ignore
        return false;
    }

    String getPath() throws NamespaceException {
        if (itemPath == null) {
            itemPath = resolver.getJCRPath(path);
        }
        return itemPath;
    }
}