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

import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.core.security.authorization.AccessControlEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlPolicy;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <code>CombinedEditor</code>...
 */
class CombinedEditor implements AccessControlEditor {

    private static Logger log = LoggerFactory.getLogger(CombinedEditor.class);

    private final AccessControlEditor[] editors;

    CombinedEditor(AccessControlEditor[] editors) {
        this.editors = editors;
    }

    //------------------------------------------------< AccessControlEditor >---
    /**
     * @see AccessControlEditor#getPolicies(String)
     */
    public AccessControlPolicy[] getPolicies(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        List<AccessControlPolicy> templates = new ArrayList<AccessControlPolicy>();
        for (AccessControlEditor editor : editors) {
            AccessControlPolicy[] ts = editor.getPolicies(nodePath);
            if (ts != null && ts.length > 0) {
                templates.addAll(Arrays.asList(ts));
            }
        }
        return templates.toArray(new AccessControlPolicy[templates.size()]);
    }

    /**
     * @see AccessControlEditor#getPolicies(Principal)
     */
    public JackrabbitAccessControlPolicy[] getPolicies(Principal principal) throws AccessControlException, RepositoryException {
        List<JackrabbitAccessControlPolicy> templates = new ArrayList<JackrabbitAccessControlPolicy>();
        for (AccessControlEditor editor : editors) {
            JackrabbitAccessControlPolicy[] ts = editor.getPolicies(principal);
            if (ts != null && ts.length > 0) {
                templates.addAll(Arrays.asList(ts));
            }
        }
        return templates.toArray(new JackrabbitAccessControlPolicy[templates.size()]);
    }

    /**
     * @see AccessControlEditor#editAccessControlPolicies(String)
     */
    public AccessControlPolicy[] editAccessControlPolicies(String nodePath) throws AccessControlException, PathNotFoundException, RepositoryException {
        List<AccessControlPolicy> templates = new ArrayList<AccessControlPolicy>();
        for (AccessControlEditor editor : editors) {
            try {
                templates.addAll(Arrays.asList(editor.editAccessControlPolicies(nodePath)));
            } catch (AccessControlException e) {
                log.debug(e.getMessage());
                // ignore.
            }
        }
        return templates.toArray(new AccessControlPolicy[templates.size()]);
    }

    /**
     * @see AccessControlEditor#editAccessControlPolicies(Principal)
     */
    public JackrabbitAccessControlPolicy[] editAccessControlPolicies(Principal principal) throws RepositoryException {
        List<JackrabbitAccessControlPolicy> templates = new ArrayList<JackrabbitAccessControlPolicy>();
        for (AccessControlEditor editor : editors) {
            try {
                templates.addAll(Arrays.asList(editor.editAccessControlPolicies(principal)));
            } catch (AccessControlException e) {
                log.debug(e.getMessage());
                // ignore.
            }
        }
        return templates.toArray(new JackrabbitAccessControlPolicy[templates.size()]);
    }

    /**
     * @see AccessControlEditor#setPolicy(String,AccessControlPolicy)
     */
    public void setPolicy(String nodePath, AccessControlPolicy template) throws AccessControlException, PathNotFoundException, RepositoryException {
        for (AccessControlEditor editor : editors) {
            try {
                // return as soon as the first editor successfully handled the
                // specified template
                editor.setPolicy(nodePath, template);
                log.debug("Set template " + template + " using " + editor);
                return;
            } catch (AccessControlException e) {
                log.debug(e.getMessage());
                // ignore and try next
            }
        }

        // none accepted -> throw
        throw new AccessControlException("None of the editors accepted policy " + template + " at " + nodePath);
    }

    /**
     * @see AccessControlEditor#removePolicy(String,AccessControlPolicy)
     */
    public void removePolicy(String nodePath,
                             AccessControlPolicy policy) throws AccessControlException, PathNotFoundException, RepositoryException {
        for (AccessControlEditor editor : editors) {
            try {
                // return as soon as the first editor successfully handled the
                // specified template
                editor.removePolicy(nodePath, policy);
                log.debug("Removed template " + policy + " using " + editor);
                return;
            } catch (AccessControlException e) {
                log.debug(e.getMessage());
                // ignore and try next
            }
        }
        // neither of the editors was able to remove a policy at nodePath
        throw new AccessControlException("Unable to remove template " + policy);
    }
}