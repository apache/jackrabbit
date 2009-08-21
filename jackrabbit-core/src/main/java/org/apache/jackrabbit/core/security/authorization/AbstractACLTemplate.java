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
package org.apache.jackrabbit.core.security.authorization;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;

/**
 * <code>AbstractACLTemplate</code>...
 */
public abstract class AbstractACLTemplate implements JackrabbitAccessControlList,
        AccessControlConstants {

    /**
     * Path of the node this ACL template has been created for.
     */
    protected final String path;

    /**
     * The value factory
     */
    protected final ValueFactory valueFactory;

    protected AbstractACLTemplate(String path, ValueFactory valueFactory) {
        this.path = path;
        this.valueFactory = valueFactory;
    }

    /**
     * Validates the given parameters to create a new ACE and throws an
     * <code>AccessControlException</code> if any of them is invalid. Otherwise
     * this method returns silently.
     *
     * @param principal The principal to create the ACE for.
     * @param privileges The privileges to be granted/denied by the ACE.
     * @param isAllow Defines if the priveleges are allowed or denied.
     * @param restrictions The additional restrictions.
     * @throws AccessControlException If any of the given params is invalid.
     */
    protected abstract void checkValidEntry(Principal principal,
                                            Privilege[] privileges,
                                            boolean isAllow,
                                            Map<String, Value> restrictions) throws AccessControlException;

    //--------------------------------------< JackrabbitAccessControlPolicy >---
    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy#getPath()
     */
    public String getPath() {
        return path;
    }

    //----------------------------------------< JackrabbitAccessControlList >---
    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlList#addEntry(Principal, Privilege[], boolean)
     */
    public boolean addEntry(Principal principal, Privilege[] privileges, boolean isAllow)
            throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, isAllow, Collections.<String, Value>emptyMap());
    }


    //--------------------------------------------------< AccessControlList >---
    /**
     * @see javax.jcr.security.AccessControlList#addAccessControlEntry(java.security.Principal , javax.jcr.security.Privilege[])
     */
    public boolean addAccessControlEntry(Principal principal, Privilege[] privileges)
            throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, true, Collections.<String, Value>emptyMap());
    }
}