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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

/**
 * <code>UserConstants</code>...
 */
interface UserConstants {

    NameFactory NF = NameFactoryImpl.getInstance();

    /**
     * root-path to security related content e.g. principals
     */
    String SECURITY_ROOT_PATH = "/rep:security";
    String AUTHORIZABLES_PATH = SECURITY_ROOT_PATH + "/rep:authorizables";
    String USERS_PATH = AUTHORIZABLES_PATH + "/rep:users";
    String GROUPS_PATH = AUTHORIZABLES_PATH + "/rep:groups";

    /**
     * Configuration key and default value for the the name of the
     * 'UserAdmin' group-principal.
     */
    String USER_ADMIN_GROUP_NAME = "UserAdmin";
    /**
     * Configuration key and default value for the the name of the
     * 'GroupAdmin' group-principal
     */
    String GROUP_ADMIN_GROUP_NAME = "GroupAdmin";

    Name P_PRINCIPAL_NAME = NF.create(Name.NS_REP_URI, "principalName");
    /**
     * @deprecated As of 2.0 the id-hash is stored with the jcr:uuid making the
     * rep:userId property redundant. It has been removed from the node type
     * definition.
     */
    Name P_USERID = NF.create(Name.NS_REP_URI, "userId");
    Name P_PASSWORD = NF.create(Name.NS_REP_URI, "password");
    Name P_DISABLED = NF.create(Name.NS_REP_URI, "disabled");

    /**
     * @deprecated As of 2.0 group membership is stored with the group node.
     * @see #P_MEMBERS
     */
    Name P_GROUPS = NF.create(Name.NS_REP_URI, "groups");

    Name P_MEMBERS = NF.create(Name.NS_REP_URI, "members");
    Name N_MEMBERS = NF.create(Name.NS_REP_URI, "members");

    /**
     * Name of the user property containing the principal names of those allowed
     * to impersonate.
     */
    Name P_IMPERSONATORS = NF.create(Name.NS_REP_URI, "impersonators");

    Name NT_REP_AUTHORIZABLE = NF.create(Name.NS_REP_URI, "Authorizable");
    Name NT_REP_AUTHORIZABLE_FOLDER = NF.create(Name.NS_REP_URI, "AuthorizableFolder");
    Name NT_REP_USER = NF.create(Name.NS_REP_URI, "User");
    Name NT_REP_GROUP = NF.create(Name.NS_REP_URI, "Group");
    Name NT_REP_MEMBERS = NF.create(Name.NS_REP_URI, "Members");
    Name MIX_REP_IMPERSONATABLE = NF.create(Name.NS_REP_URI, "Impersonatable");
}
