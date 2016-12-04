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
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * <code>AccessControlConstants</code>...
 */
public interface AccessControlConstants {

    //---------------------------------------------------------< node names >---
    /**
     * Default name for a node of type rep:Policy.
     */
    Name N_POLICY = NameConstants.REP_POLICY;

    /**
     * Name name for a node of type rep:Policy storing repository level privileges.
     */
    Name N_REPO_POLICY = NameConstants.REP_REPO_POLICY;

    /**
     * PrincipalBased-ACL:
     * Name of the root-node of all access-control-nodes that store the
     * privileges for individual principals. This node is created upon
     * initializing this provider.
     */
    Name N_ACCESSCONTROL = NameConstants.REP_ACCESSCONTROL;

    //-----------------------------------------------------< property names >---
    /**
     * rep:privileges property name
     */
    Name P_PRIVILEGES =  NameConstants.REP_PRIVILEGES;
    /**
     * rep:principalName property name
     */
    Name P_PRINCIPAL_NAME = NameConstants.REP_PRINCIPAL_NAME;

    /**
     * rep:glob property name used to restrict the number of child nodes
     * or properties that are affected by an ACL inherited from a parent node.
     */
    Name P_GLOB = NameConstants.REP_GLOB;

    //----------------------------------------------------< node type names >---
    /**
     * rep:AccessControllable nodetype
     */
    Name NT_REP_ACCESS_CONTROLLABLE = NameConstants.REP_ACCESS_CONTROLLABLE;
    /**
     * rep:RepoAccessControllable nodetype
     */
    Name NT_REP_REPO_ACCESS_CONTROLLABLE = NameConstants.REP_REPO_ACCESS_CONTROLLABLE;
    /**
     * rep:ACL nodetype
     */
    Name NT_REP_ACL = NameConstants.REP_ACL;
    /**
     * rep:ACE nodetype
     */
    Name NT_REP_ACE = NameConstants.REP_ACE;
    /**
     * rep:GrantACE nodetype
     */
    Name NT_REP_GRANT_ACE = NameConstants.REP_GRANT_ACE;
    /**
     * rep:DenyACE nodetype
     */
    Name NT_REP_DENY_ACE = NameConstants.REP_DENY_ACE;

    //----------------------------------< node types for principal based ac >---
    /**
     * rep:AccessControl nodetype
     */
    Name NT_REP_ACCESS_CONTROL = NameConstants.REP_ACCESS_CONTROL;

    /**
     * rep:PrincipalAccessControl nodetype
     */
    Name NT_REP_PRINCIPAL_ACCESS_CONTROL = NameConstants.REP_PRINCIPAL_ACCESS_CONTROL;

}