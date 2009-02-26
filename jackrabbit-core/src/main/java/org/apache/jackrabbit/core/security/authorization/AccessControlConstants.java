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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

/**
 * <code>AccessControlConstants</code>...
 */
public interface AccessControlConstants {

    NameFactory NF = NameFactoryImpl.getInstance();

    //---------------------------------------------------------< node names >---
    /**
     * Default name for a node of type rep:Policy.
     */
    Name N_POLICY = NF.create(Name.NS_REP_URI, "policy");

    /**
     * PrincipalBased-ACL:
     * Name of the root-node of all access-control-nodes that store the
     * privileges for individual principals. This node is created upon
     * initializing this provider.
     */
    Name N_ACCESSCONTROL = NF.create(Name.NS_REP_URI, "accesscontrol");

    //-----------------------------------------------------< property names >---
    /**
     * rep:privileges property name
     */
    Name P_PRIVILEGES = NF.create(Name.NS_REP_URI, "privileges");
    /**
     * rep:principalName property name
     */
    Name P_PRINCIPAL_NAME = NF.create(Name.NS_REP_URI, "principalName");

    //----------------------------------------------------< node type names >---
    /**
     * rep:AccessControllable nodetype
     */
    Name NT_REP_ACCESS_CONTROLLABLE = NF.create(Name.NS_REP_URI, "AccessControllable");
    /**
     * rep:ACL nodetype
     */
    Name NT_REP_ACL = NF.create(Name.NS_REP_URI, "ACL");
    /**
     * rep:ACE nodetype
     */
    Name NT_REP_ACE = NF.create(Name.NS_REP_URI, "ACE");
    /**
     * rep:GrantACE nodetype
     */
    Name NT_REP_GRANT_ACE = NF.create(Name.NS_REP_URI, "GrantACE");
    /**
     * rep:DenyACE nodetype
     */
    Name NT_REP_DENY_ACE = NF.create(Name.NS_REP_URI, "DenyACE");

    //----------------------------------< node types for principal based ac >---
    /**
     * rep:AccessControl nodetype
     */
    Name NT_REP_ACCESS_CONTROL = NF.create(Name.NS_REP_URI, "AccessControl");

    /**
     * rep:PrincipalAccessControl nodetype
     */
    Name NT_REP_PRINCIPAL_ACCESS_CONTROL = NF.create(Name.NS_REP_URI, "PrincipalAccessControl");
    
}