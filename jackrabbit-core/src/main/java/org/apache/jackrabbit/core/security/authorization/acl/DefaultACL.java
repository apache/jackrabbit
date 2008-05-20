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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.Iterator;

/**
 * <code>DefaultACL</code>
 */
final class DefaultACL extends ACLImpl {

    DefaultACL(NodeId id) {
        super(id, null, false);
    }

    Iterator getEntries() {
        return Collections.EMPTY_SET.iterator();
    }

    int getPrivileges() {
        return PrivilegeRegistry.NO_PRIVILEGE;
    }

    int getPermissions() {
        return Permission.NONE;
    }

    //------------------------------------------------< AccessControlPolicy >---
    public String getName() throws RepositoryException {
        return "Default ACL";
    }

    public String getDescription() throws RepositoryException {
        return "Default policy not defining any entries and no privileges.";
    }
}