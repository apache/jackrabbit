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
package org.apache.jackrabbit.core.security.authorization.combined;

import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.util.Text;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Item;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <code>ACLImpl</code>...
 */
class ACLImpl {

    private final Set acPaths;
    private final Map principalToEntryArray;

    private int privileges = -1;

    ACLImpl(Map principalToEntryArray, Set acPaths) {
        this.acPaths = acPaths;
        this.principalToEntryArray = principalToEntryArray;
    }

    Set getAcPaths() {
        return acPaths;
    }

    /**
     * @param target Existing target item for which the permissions will be
     * evaluated.
     * @param protectsACL
     * @return
     * @throws RepositoryException
     */
    int getPermissions(Item target, boolean protectsACL) throws RepositoryException {
        int allows = 0;
        int denies = 0;
        for (Iterator it = principalToEntryArray.keySet().iterator();
             it.hasNext() && allows != Permission.ALL;) {
            Principal princ = (Principal) it.next();
            PolicyEntryImpl[] aces = (PolicyEntryImpl[]) principalToEntryArray.get(princ);
            // loop over all entries and evaluate allows/denies for those
            // matching the given jcrPath
            for (int i = 0; i < aces.length; i++) {
                PolicyEntryImpl entr = aces[i];
                // TODO: check again if correct
                if (entr.matches(target)) {
                    int privs = entr.getPrivilegeBits();
                    int permissions = Permission.calculatePermissions(privs, privs, protectsACL);
                    if (entr.isAllow()) {
                        allows |= Permission.diff(permissions, denies);
                    } else {
                        denies |= Permission.diff(permissions, allows);
                    }
                }
            }
        }
        return allows;
    }

    /**
     *
     * @param parent Existing parent of the target to be evaluated.
     * @param targetName name of a non-existing child item to calculate the
     * permissions for.
     * @param protectsACL
     * @return
     * @throws RepositoryException
     */
    int getPermissions(Node parent, String targetName, boolean protectsACL) throws RepositoryException {
        int allows = 0;
        int denies = 0;
        String jcrPath = parent.getPath() + "/" + targetName;
        for (Iterator it = principalToEntryArray.keySet().iterator();
             it.hasNext() && allows != Permission.ALL;) {
            Principal princ = (Principal) it.next();
            PolicyEntryImpl[] aces = (PolicyEntryImpl[]) principalToEntryArray.get(princ);
            // loop over all entries and evaluate allows/denies for those
            // matching the given jcrPath
            // TODO: check if correct
            for (int i = 0; i < aces.length; i++) {
                PolicyEntryImpl entr = aces[i];
                if (entr.matches(jcrPath)) {
                    int privs = entr.getPrivilegeBits();
                    int permissions = Permission.calculatePermissions(privs, privs, protectsACL);
                    if (entr.isAllow()) {
                        allows |= Permission.diff(permissions, denies);
                    } else {
                        denies |= Permission.diff(permissions, allows);
                    }
                }
            }
        }
        return allows;
    }

    int getPrivileges(String nodePath) throws RepositoryException {
        if (privileges == -1) {
            int allows = 0;
            int denies = 0;
            for (Iterator it = principalToEntryArray.keySet().iterator();
                 it.hasNext() && allows != Permission.ALL;) {
                Principal princ = (Principal) it.next();
                PolicyEntryImpl[] aces = (PolicyEntryImpl[]) principalToEntryArray.get(princ);
                // loop over all entries and evaluate allows/denies for those
                // matching the given jcrPath
                for (int i = 0; i < aces.length; i++) {
                    PolicyEntryImpl entr = aces[i];
                    // TODO: check again which ACEs must be respected.
                    // TODO: maybe ancestor-defs only if glob = *?
                    String np = entr.getNodePath();
                    if (np.equals(nodePath) || Text.isDescendant(np, nodePath)) {
                        if (entr.isAllow()) {
                            allows |= PrivilegeRegistry.diff(entr.getPrivilegeBits(), denies);
                        } else {
                            denies |= PrivilegeRegistry.diff(entr.getPrivilegeBits(), allows);
                        }
                    }
                }
            }
            privileges = allows;
        }
        return privileges;
    }
}