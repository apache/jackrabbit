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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.Value;

/**
 * Remote version of the JCR {@link javax.jcr.nodetype.NodeType NodeType}
 * interface. Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerNodeType ServerNodeType} and
 * {@link org.apache.jackrabbit.rmi.client.ClientNodeType ClientNodeType}
 * adapters to provide transparent RMI access to remote node types.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding NodeType method. The remote object will simply forward
 * the method call to the underlying NodeType instance. Return values
 * and possible exceptions are copied over the network. Complex return
 * values (like NodeTypes and PropertyDefs) are returned as remote
 * references to the corresponding remote interfaces. RMI errors are
 * signaled with RemoteExceptions.
 *
 * @see javax.jcr.nodetype.NodeType
 * @see org.apache.jackrabbit.rmi.client.ClientNodeType
 * @see org.apache.jackrabbit.rmi.server.ServerNodeType
 */
public interface RemoteNodeType extends Remote {

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#getName() NodeType.getName()} method.
     *
     * @return node type name
     * @throws RemoteException on RMI errors
     */
    String getName() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#isMixin() NodeType.isMixin()} method.
     *
     * @return <code>true</code> if this is a mixin type,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isMixin() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#isAbstract() NodeType.isAbstract()} method.
     *
     * @return <code>true</code> if this is an abstract type,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isAbstract() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#hasOrderableChildNodes() NodeType.hasOrderableChildNodes()}
     * method.
     *
     * @return <code>true</code> if nodes of this type has orderable
     *         child nodes, <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean hasOrderableChildNodes() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#getSupertypes() NodeType.getSupertypes()}
     * method.
     *
     * @return supertypes
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType[] getSupertypes() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#getDeclaredSupertypes() NodeType.getDeclaredSupertypes()}
     * method.
     *
     * @return declared supertypes
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType[] getDeclaredSupertypes() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#isNodeType(String) NodeType.isNodeType(String)}
     * method.
     *
     * @param type node type name
     * @return <code>true</code> if this node type is or extends the
     *         given node type, <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isNodeType(String type) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#getPropertyDefinitions() NodeType.getPropertyDefinitions()}
     * method.
     *
     * @return property definitions
     * @throws RemoteException on RMI errors
     */
    RemotePropertyDefinition[] getPropertyDefs() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#getDeclaredPropertyDefinitions() NodeType.getDeclaredPropertyDefinitions()}
     * method.
     *
     * @return declared property definitions
     * @throws RemoteException on RMI errors
     */
    RemotePropertyDefinition[] getDeclaredPropertyDefs() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#getChildNodeDefinitions() NodeType.getChildNodeDefinitions()}
     * method.
     *
     * @return child node definitions
     * @throws RemoteException on RMI errors
     */
    RemoteNodeDefinition[] getChildNodeDefs() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#getDeclaredChildNodeDefinitions() NodeType.getDeclaredChildNodeDefinitions()}
     * method.
     *
     * @return declared child node definitions
     * @throws RemoteException on RMI errors
     */
    RemoteNodeDefinition[] getDeclaredChildNodeDefs() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#canSetProperty(String,Value) NodeType.canSetProperty(String,Value)}
     * method.
     *
     * @param name property name
     * @param value property value
     * @return <code>true</code> if the property can be set,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean canSetProperty(String name, Value value) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#canSetProperty(String,Value[]) NodeType.canSetProperty(String,Value[])}
     * method.
     *
     * @param name property name
     * @param values property values
     * @return <code>true</code> if the property can be set,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean canSetProperty(String name, Value[] values) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#canAddChildNode(String) NodeType.canAddChildNode(String)}
     * method.
     *
     * @param name child node name
     * @return <code>true</code> if the child node can be added,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean canAddChildNode(String name) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#canAddChildNode(String,String) NodeType.canAddChildNode(String,String)}
     * method.
     *
     * @param name child node name
     * @param type child node type
     * @return <code>true</code> if the child node can be added,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean canAddChildNode(String name, String type) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#canRemoveItem(String) NodeType.canRemoveItem(String)}
     * method.
     *
     * @param name item name
     * @return <code>true</code> if the item can be removed,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean canRemoveItem(String name) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#getPrimaryItemName() NodeType.getPrimaryItemName()}
     * method.
     *
     * @return primary item name
     * @throws RemoteException on RMI errors
     */
    String getPrimaryItemName() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#canRemoveNode(String) NodeType.canRemoveNode()}
     * method.
     *
     * @return boolean
     * @throws RemoteException on RMI errors
     */
	boolean canRemoveNode(String nodeName) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#canRemoveProperty(String) NodeType.canRemoveProperty()}
     * method.
     *
     * @return boolean
     * @throws RemoteException on RMI errors
     */
	boolean canRemoveProperty(String propertyName) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#getDeclaredSupertypeNames() NodeType.getDeclaredSupertypeNames()}
     * method.
     *
     * @return a String[]
     * @throws RemoteException on RMI errors
     */
	String[] getDeclaredSupertypeNames() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#isQueryable() NodeType.isQueryable()}
     * method.
     *
     * @return boolean
     * @throws RemoteException on RMI errors
     */
	boolean isQueryable() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#getDeclaredSubtypes() NodeType.getDeclaredSubtypes()}
     * method.
     *
     * @return RemoteIterator
     * @throws RemoteException on RMI errors
     */
	RemoteIterator getDeclaredSubtypes() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.nodetype.NodeType#getSubtypes() NodeType.getSubtypes()}
     * method.
     *
     * @return RemoteIterator
     * @throws RemoteException on RMI errors
     */
	RemoteIterator getSubtypes() throws RemoteException;

}
