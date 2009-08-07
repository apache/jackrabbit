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
package org.apache.jackrabbit.api.jsr283.security;

/**
 * A privilege represents the capability of performing a particular set
 * of operations on items in the JCR repository. Each privilege is identified
 * by a JCR name. JCR defines a set of standard privileges in the <code>jcr</code>
 * namespace. Implementations may add additional privileges in namespaces other
 * than <code>jcr</code>.
 * <p/>
 * A privilege may be an aggregate privilege. Aggregate privileges are sets of
 * other privileges. Granting, denying, or testing an aggregate privilege is
 * equivalent to individually granting, denying, or testing each privilege it
 * contains. The privileges contained by an aggregate privilege may themselves
 * be aggregate privileges if the resulting privilege graph is acyclic.
 * <p/>
 * A privilege may be an abstract privilege. Abstract privileges cannot
 * themselves be granted or denied, but can be composed into aggregate privileges
 * which are granted or denied.
 * <p/>
 * A privilege can be both aggregate and abstract.
 *
 * @since JCR 2.0
 */
public interface Privilege {

    /**
     * A constant representing <code>jcr:read</code> (in extended form), the privilege to retrieve
     * a node and get its properties and their values.
     */
    public static final String JCR_READ = "{http://www.jcp.org/jcr/1.0}read";

    /**
     * A constant representing <code>jcr:modifyProperties</code> (in extended form), the privilege
     * to create, modify and remove the properties of a node.
     */
    public static final String JCR_MODIFY_PROPERTIES = "{http://www.jcp.org/jcr/1.0}modifyProperties";

    /**
     * A constant representing <code>jcr:addChildNodes</code> (in extended form), the privilege
     * to create child nodes of a node.
     */
    public static final String JCR_ADD_CHILD_NODES = "{http://www.jcp.org/jcr/1.0}addChildNodes";

    /**
     * A constant representing <code>jcr:removeNode</code> (in extended form), the privilege
     * to remove a node.
     * <p>
     * In order to actually remove a node requires <code>jcr:removeNode</code> on that node
     * and <code>jcr:removeChildNodes</code> on the parent node.
     * <p>
     * The distinction is provided in order to reflect implementations that internally model "remove"
     * as a "delete" instead of a "unlink". A repository that uses the "delete" model
     * can have <code>jcr:removeChildNodes</code> in every access control policy, so that removal
     * is effectively controlled by <code>jcr:removeNode</code>.
     */
    public static final String JCR_REMOVE_NODE = "{http://www.jcp.org/jcr/1.0}removeNode";

    /**
     * A constant representing <code>jcr:removeChildNodes</code> (in extended form), the privilege
     * to remove child nodes of a node. In order to actually remove a node requires <code>jcr:removeNode</code> on that node
     * and <code>jcr:removeChildNodes</code> on the parent node.
     * <p>
     * The distinction is provided in order to reflect implementations that internally model "remove"
     * as a "unlink" instead of a "delete". A repository that uses the "unlink" model
     * can have <code>jcr:removeNode</code> in every access control policy, so that removal
     * is effectively controlled by <code>jcr:removeChildNodes</code>.
     */
    public static final String JCR_REMOVE_CHILD_NODES = "{http://www.jcp.org/jcr/1.0}removeChildNodes";

    /**
     * A constant representing <code>jcr:write</code> (in extended form), an aggregate privilege that contains:
     * <ul>
     *  <li><code>jcr:modifyProperties</code></li>
     *  <li><code>jcr:addChildNodes</code></li>
     *  <li><code>jcr:removeNode</code></li>
     *  <li><code>jcr:removeChildNodes</code></li>
     * </ul>
     */
    public static final String JCR_WRITE = "{http://www.jcp.org/jcr/1.0}write";

    /**
     * A constant representing <code>jcr:readAccessControl</code> (in extended form), the privilege
     * to get the access control policy of a node.
     */
    public static final String JCR_READ_ACCESS_CONTROL = "{http://www.jcp.org/jcr/1.0}readAccessControl";

    /**
     * A constant representing <code>jcr:modifyAccessControl</code> (in extended form), the privilege
     * to modify the access control policies of a node.
     */
    public static final String JCR_MODIFY_ACCESS_CONTROL = "{http://www.jcp.org/jcr/1.0}modifyAccessControl";

    /**
     * A constant representing <code>jcr:retentionManagement</code>.
     */
    public static final String JCR_RETENTION_MANAGEMENT = "{http://www.jcp.org/jcr/1.0}retentionManagement";

    /**
     * A constant representing <code>jcr:lifecycleManagement</code>.
     */
    public static final String JCR_LIFECYCLE_MANAGEMENT = "{http://www.jcp.org/jcr/1.0}lifecycleManagement";

    /**
     * A constant representing <code>jcr:versionManagement</code>.
     */
    public static final String JCR_VERSION_MANAGEMENT = "{http://www.jcp.org/jcr/1.0}versionManagement";

    /**
     * A constant representing <code>jcr:lockManagement</code>.
     */
    public static final String JCR_LOCK_MANAGEMENT = "{http://www.jcp.org/jcr/1.0}lockManagement";

    /**
     * A constant representing <code>jcr:nodeTypeManagement</code>.
     */
    public static final String JCR_NODE_TYPE_MANAGEMENT = "{http://www.jcp.org/jcr/1.0}nodeTypeManagement";

    /**
     * A constant representing <code>jcr:all</code> (in extended form), an aggregate privilege that contains
     * all predefined privileges:
     * <ul>
     *   <li><code>jcr:read</code></li>
     *   <li><code>jcr:write</code></li>
     *   <li><code>jcr:readAccessControl</code></li>
     *   <li><code>jcr:modifyAccessControl</code></li>
     *   <li><code>jcr:lockManagement</code></li>
     *   <li><code>jcr:versionManagement</code></li>
     *   <li><code>jcr:nodeTypeManagement</code></li>
     *   <li><code>jcr:retentionManagement</code></li>
     *   <li><code>jcr:lifecycleManagement</code></li>
     * </ul>
     * It should, in addition, include all implementation-defined privileges.
     */
    public static final String JCR_ALL = "{http://www.jcp.org/jcr/1.0}all";

    /**
     * Returns the name of this privilege.
     *
     * @return the name of this privilege.
     */
    public String getName();

    /**
     * Returns whether this privilege is an abstract privilege.
     * @return <code>true</code> if this privilege is an abstract privilege;
     *         <code>false</code> otherwise.
     */
    public boolean isAbstract();

    /**
     * Returns whether this privilege is an aggregate privilege.
     * @return <code>true</code> if this privilege is an aggregate privilege;
     *         <code>false</code> otherwise.
     */
    public boolean isAggregate();

    /**
     * If this privilege is an aggregate privilege, returns the privileges directly
     * contained by the aggregate privilege. Otherwise returns an empty array.
     *
     * @return an array of <code>Privilege</code>s
     */
    public Privilege[] getDeclaredAggregatePrivileges();

    /**
     * If this privilege is an aggregate privilege, returns the privileges it
     * contains, the privileges contained by any aggregate privileges among
     * those, and so on (the transitive closure of privileges contained by this
     * privilege). Otherwise returns an empty array.
     *
     * @return an array of <code>Privilege</code>s
     */
    public Privilege[] getAggregatePrivileges();
}
