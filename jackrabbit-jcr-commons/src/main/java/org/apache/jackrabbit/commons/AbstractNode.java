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
package org.apache.jackrabbit.commons;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * Abstract base class for implementing the JCR {@link Node} interface.
 * <p>
 * {@link Item} methods <em>without</em> a default implementation:
 * <ul>
 *   <li>{@link Item#accept(javax.jcr.ItemVisitor)}</li>
 *   <li>{@link Item#getName()}</li>
 *   <li>{@link Item#getParent()}</li>
 *   <li>{@link Item#getSession()}</li>
 *   <li>{@link Item#isModified()}</li>
 *   <li>{@link Item#isNew()}</li>
 *   <li>{@link Item#isSame(Item)}</li>
 *   <li>{@link Item#refresh(boolean)}</li>
 *   <li>{@link Item#remove()}</li>
 *   <li>{@link Item#save()}</li>
 * </ul>
 * <p>
 * {@link Node} methods <em>without</em> a default implementation:
 * <ul>
 *   <li>{@link Node#addMixin(String)}</li>
 *   <li>{@link Node#addNode(String)}</li>
 *   <li>{@link Node#addNode(String, String)}</li>
 *   <li>{@link Node#canAddMixin(String)}</li>
 *   <li>{@link Node#cancelMerge(Version)}</li>
 *   <li>{@link Node#checkin()}</li>
 *   <li>{@link Node#checkout()}</li>
 *   <li>{@link Node#doneMerge(Version)}</li>
 *   <li>{@link Node#getBaseVersion()}</li>
 *   <li>{@link Node#getCorrespondingNodePath(String)}</li>
 *   <li>{@link Node#getDefinition()}</li>
 *   <li>{@link Node#getIndex()}</li>
 *   <li>{@link Node#getLock()}</li>
 *   <li>{@link Node#getNode(String)}</li>
 *   <li>{@link Node#getNodes()}</li>
 *   <li>{@link Node#getNodes(String)}</li>
 *   <li>{@link Node#getPrimaryItem()}</li>
 *   <li>{@link Node#getProperties()}</li>
 *   <li>{@link Node#getProperties(String)}</li>
 *   <li>{@link Node#getReferences()}</li>
 *   <li>{@link Node#lock(boolean, boolean)}</li>
 *   <li>{@link Node#merge(String, boolean)}</li>
 *   <li>{@link Node#orderBefore(String, String)}</li>
 *   <li>{@link Node#removeMixin(String)}</li>
 *   <li>{@link Node#restore(Version, String, boolean)}</li>
 *   <li>{@link Node#setProperty(String, Value)}</li>
 *   <li>{@link Node#setProperty(String, Value[])}</li>
 *   <li>{@link Node#unlock()}</li>
 *   <li>{@link Node#update(String)}</li>
 * </ul>
 */
public abstract class AbstractNode extends AbstractItem implements Node {

    //----------------------------------------------------------------< Item >

    /**
     * Accepts the given item visitor.
     * <p>
     * The default implementation calls {@link ItemVisitor#visit(Node)} on
     * the given visitor with this node as the argument.
     *
     * @param visitor item visitor
     * @throws RepositoryException if an error occurs
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }

    /**
     * Returns the path of this node.
     * <p>
     * The default implementation recursively calls this method on the
     * parent node and appends the name and optionally the index of this
     * node to construct the full path. Returns "/" if the parent node is
     * not available (i.e. this is the root node).
     *
     * @return node path
     * @throws RepositoryException if an error occurs
     */
    public String getPath() throws RepositoryException {
        try {
            StringBuffer buffer = new StringBuffer(getParent().getPath());
            if (buffer.length() > 1) {
                buffer.append('/');
            }
            buffer.append(getName());
            int index = getIndex();
            if (index != 1) {
                buffer.append('[');
                buffer.append(index);
                buffer.append(']');
            }
            return buffer.toString();
        } catch (ItemNotFoundException e) {
            return "/";
        }
    }

    /**
     * Returns <code>true</code>.
     *
     * @return <code>true</code>
     */
    public boolean isNode() {
        return true;
    }

    //----------------------------------------------------------------< Node >

    /**
     * Returns the declared mixin node types of this node.
     * <p>
     * The default implementation uses the values of the
     * <code>jcr:mixinTypes</code> property to look up the mixin node types
     * from the {@link NodeTypeManager} of the current workspace.
     *
     * @return mixin node types
     * @throws RepositoryException if an error occurs
     */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        try {
            NodeTypeManager manager =
                getSession().getWorkspace().getNodeTypeManager();
            Property property = getProperty(getName("jcr:mixinTypes"));
            Value[] values = property.getValues();
            NodeType[] types = new NodeType[values.length];
            for (int i = 0; i < values.length; i++) {
                types[i] = manager.getNodeType(values[i].getString());
            }
            return types;
        } catch (PathNotFoundException e) {
            // jcr:mixinTypes does not exist, i.e. no mixin types on this node
            return new NodeType[0];
        }
    }

    /**
     * Returns the primary node type of this node.
     * <p>
     * The default implementation uses the value of the
     * <code>jcr:primaryType</code> property to look up the primary
     * node type from the {@link NodeTypeManager} of the current workspace.
     *
     * @return primary node type
     * @throws RepositoryException if an error occurs
     */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        NodeTypeManager manager =
            getSession().getWorkspace().getNodeTypeManager();
        Property property = getProperty(getName("jcr:primaryType"));
        return manager.getNodeType(property.getString());
    }

    /**
     * Returns the property at the given relative path from this node.
     * <p>
     * The default implementation looks up the parent node of the given
     * relative path and iterates through the properties of that node to
     * find and return the identified property.
     *
     * @param relPath relative path of the property
     * @return property
     * @throws PathNotFoundException if the property is not found
     * @throws RepositoryException if an error occurs
     */
    public Property getProperty(String relPath)
            throws PathNotFoundException, RepositoryException {
        // Corner case, remove any "/." self references at the end of the path
        while (relPath.endsWith("/.")) {
            relPath = relPath.substring(0, relPath.length() - 2);
        }

        // Find the parent node of the identified property
        Node node = this;
        int slash = relPath.lastIndexOf('/');
        if (slash == 0) {
            node = getSession().getRootNode();
            relPath = relPath.substring(1);
        } else if (slash > 0) {
            node = getNode(relPath.substring(0, slash));
            relPath = relPath.substring(slash + 1);
        }

        // Look for the named property. Must iterate and re-check for the name
        // since the client could have used an invalid path like "./a|b".
        PropertyIterator properties = node.getProperties(relPath);
        while (properties.hasNext()) {
            Property property = (Property) properties.next();
            if (relPath.equals(property.getName())) {
                return property;
            }
        }

        throw new PathNotFoundException("Property not found: " + relPath);
    }

    /**
     * Returns the UUID of this node.
     * <p>
     * The default implementation checks if this node is referenceable (i.e. of
     * type <code>mix:referenceable</code>) and returns the contents of the
     * <code>jcr:uuid</code> property if it is.
     *
     * @return node UUID
     * @throws UnsupportedRepositoryOperationException
     *         if this node is not referenceable
     * @throws RepositoryException if an error occurs
     */
    public String getUUID()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        if (isNodeType(getName("mix:referenceable"))) {
            return getProperty(getName("jcr:uuid")).getString();
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "This node is not referenceable: " + getPath());
        }
    }

    /**
     * Returns the version history of this node.
     * <p>
     * The default implementation returns the containing version history of
     * the base version of this node.
     *
     * @return version history
     * @throws RepositoryException if an error occurs
     */
    public VersionHistory getVersionHistory() throws RepositoryException {
        return getBaseVersion().getContainingHistory();
    }

    /**
     * Checks whether a node at the given relative path exists.
     * <p>
     * The default implementation looks up the node using
     * {@link Node#getNode(String)} and returns <code>true</code> if
     * a {@link PathNotFoundException} is not thrown.
     *
     * @param relPath relative path
     * @return <code>true</code> if a node exists at the given path,
     *         <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
     */
    public boolean hasNode(String relPath) throws RepositoryException {
        try {
            getNode(relPath);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if this node has one or more properties.
     * <p>
     * The default implementation calls {@link Node#getNodes()} and returns
     * <code>true</code> iff returned iterator has at least one element.
     *
     * @return <code>true</code> if this node has child nodes,
     *         <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
     */
    public boolean hasNodes() throws RepositoryException {
        return getNodes().hasNext();
    }

    /**
     * Checks if this node has one or more properties.
     * <p>
     * The default implementation calls {@link Node#getProperties()} and
     * returns <code>true</code> iff returned iterator has at least one element.
     * <p>
     * Note that in normal circumstances (i.e. no weird access controls) this
     * method will always return <code>true</code> since all nodes always have
     * at least the <code>jcr:primaryType</code> property.
     *
     * @return <code>true</code> if this node has properties,
     *         <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
     */
    public boolean hasProperties() throws RepositoryException {
        return getProperties().hasNext();
    }

    /**
     * Checks whether a property at the given relative path exists.
     * <p>
     * The default implementation looks up the property using
     * {@link Node#getProperty(String)} and returns <code>true</code> if
     * a {@link PathNotFoundException} is not thrown.
     *
     * @param relPath relative path
     * @return <code>true</code> if a property exists at the given path,
     *         <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
        try {
            getProperty(relPath);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if this node holds a lock.
     * <p>
     * The default implementation calls {@link Node#getLock()} and returns
     * <code>true</code> iff the holding node of the lock is the same as this
     * node.
     *
     * @return <code>true</code> if this node holds a lock,
     *         <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
     */
    public boolean holdsLock() throws RepositoryException {
        try {
            return isSame(getLock().getNode());
        } catch (LockException e) {
            return false;
        }
    }

    /**
     * Checks whether this node is checked out.
     * <p>
     * The default implementation checks the <code>jcr:isCheckedOut</code>
     * property if this node is versionable, and recursively calls this method
     * on the parent node if this node is not versionable. A non-versionable
     * root node always returns <code>true</code> from this method.
     *
     * @return <code>true</code> if this node is checked out,
     *         <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
     */
    public boolean isCheckedOut() throws RepositoryException {
        if (isNodeType(getName("jcr:versionable"))) {
            // This node is versionable, check the jcr:isCheckedOut property
            return getProperty(getName("jcr:isCheckedOut")).getBoolean();
        } else {
            try {
                // This node is not versionable, is the parent checked out?
                return getParent().isCheckedOut();
            } catch (ItemNotFoundException e) {
                // This node is the root node, always checked out
                return true;
            }
        }
    }

    /**
     * Checks if this node is locked.
     * <p>
     * The default implementation calls {@link Node#getLock()} and returns
     * <code>true</code> iff a {@link LockException} is not thrown.
     *
     * @return <code>true</code> if this node is locked,
     *         <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
     */
    public boolean isLocked() throws RepositoryException {
        try {
            getLock();
            return true;
        } catch (LockException e) {
            return false;
        }
    }

    /**
     * Checks whether this node is of the given type.
     * <p>
     * The default implementation iterates through the primary and mixin
     * types and all the supertypes of this node, returning <code>true</code>
     * if a type with the given name is encountered. Returns <code>false</code>
     * if none of the types matches.
     *
     * @param name type name
     * @return <code>true</code> if this node is of the given type,
     *         <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
     */
    public boolean isNodeType(String name) throws RepositoryException {
        NodeType type = getPrimaryNodeType();
        if (name.equals(type.getName())) {
            return true;
        }
        NodeType[] supertypes = type.getSupertypes();
        for (int i = 0; i < supertypes.length; i++) {
            if (name.equals(supertypes[i].getName())) {
                return true;
            }
        }

        NodeType[] mixins = getMixinNodeTypes();
        for (int i = 0; i < mixins.length; i++) {
            if (name.equals(mixins[i].getName())) {
                return true;
            }
            supertypes = mixins[i].getSupertypes();
            for (int j = 0; j < supertypes.length; j++) {
                if (name.equals(supertypes[j].getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Restores this node to the version with the given name.
     * <p>
     * The default implement retrieves the named {@link Version} from the
     * associated {@link VersionHistory} and forwards the call to the
     * {@link Node#restore(Version, boolean)} method.
     *
     * @param versionName version name
     * @param removeExisting passed through
     * @throws RepositoryException if an error occurs
     */
    public void restore(String versionName, boolean removeExisting)
            throws RepositoryException {
        restore(getVersionHistory().getVersion(versionName), removeExisting);
    }

    /**
     * Restores this node to the given version.
     * <p>
     * The default implementation forwards the call to the
     * {@link Node#restore(Version, String, boolean)} method using the
     * relative path ".".
     *
     * @param version passed through
     * @param removeExisting passed through
     * @throws RepositoryException if an error occurs
     */
    public void restore(Version version, boolean removeExisting)
            throws RepositoryException {
        restore(version, ".", removeExisting);
    }

    /**
     * Restores this node to the version with the given label.
     * <p>
     * The default implement retrieves the labeled {@link Version} from the
     * associated {@link VersionHistory} and forwards the call to the
     * {@link Node#restore(Version, boolean)} method.
     *
     * @param versionLabel version label
     * @param removeExisting passed through
     * @throws RepositoryException if an error occurs
     */
    public void restoreByLabel(String versionLabel, boolean removeExisting)
            throws RepositoryException {
        restore(getVersionHistory().getVersionByLabel(versionLabel),
                removeExisting);
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to create a {@link Value} instances from
     * the given string values and forwards the call to the
     * {@link Node#setProperty(String, Value[])} method.
     *
     * @param name property name
     * @param strings string values
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, String[] strings)
            throws RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        Value[] values = new Value[strings.length];
        for (int i = 0; i < strings.length; i++) {
            values[i] = factory.createValue(strings[i]);
        }
        return setProperty(name, values);
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to create a {@link Value} instance from
     * the given string value and forwards the call to the
     * {@link Node#setProperty(String, Value)} method.
     *
     * @param name property name
     * @param value string value
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, String value)
            throws RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to create a {@link Value} instance from
     * the given binary value and forwards the call to the
     * {@link Node#setProperty(String, Value)} method.
     *
     * @param name property name
     * @param value binary value
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, InputStream value)
            throws RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to create a {@link Value} instance from
     * the given boolean value and forwards the call to the
     * {@link Node#setProperty(String, Value)} method.
     *
     * @param name property name
     * @param value boolean value
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, boolean value)
            throws RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to create a {@link Value} instance from
     * the given double value and forwards the call to the
     * {@link Node#setProperty(String, Value)} method.
     *
     * @param name property name
     * @param value double value
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, double value)
            throws RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to create a {@link Value} instance from
     * the given long value and forwards the call to the
     * {@link Node#setProperty(String, Value)} method.
     *
     * @param name property name
     * @param value long value
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, long value)
            throws RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to create a {@link Value} instance from
     * the given date value and forwards the call to the
     * {@link Node#setProperty(String, Value)} method.
     *
     * @param name property name
     * @param value date value
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, Calendar value)
            throws RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to create a {@link Value} instance from
     * the given reference value and forwards the call to the
     * {@link Node#setProperty(String, Value)} method.
     *
     * @param name property name
     * @param value reference value
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, Node value)
            throws RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to convert the given value to the given
     * type and forwards the call to the
     * {@link Node#setProperty(String, Value)} method.
     *
     * @param name property name
     * @param value property value
     * @param type property type
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, Value value, int type)
            throws RepositoryException {
        if (value.getType() != type) {
            ValueFactory factory = getSession().getValueFactory();
            value = factory.createValue(value.getString(), type);
        }
        return setProperty(name, value);
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to convert the given values to the given
     * type and forwards the call to the
     * {@link Node#setProperty(String, Value[])} method.
     *
     * @param name property name
     * @param values property values
     * @param type property type
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, Value[] values, int type)
            throws RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        Value[] converted = new Value[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i].getType() != type) {
                converted[i] = factory.createValue(values[i].getString(), type);
            } else {
                converted[i] = values[i];
            }
        }
        return setProperty(name, converted);
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to create {@link Value} instances of the
     * given type from the given string values and forwards the call to the
     * {@link Node#setProperty(String, Value[])} method.
     *
     * @param name property name
     * @param strings string values
     * @param type property type
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, String[] strings, int type)
            throws RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        Value[] values = new Value[strings.length];
        for (int i = 0; i < strings.length; i++) {
            values[i] =  factory.createValue(strings[i], type);
        }
        return setProperty(name, values);
    }

    /**
     * Sets the value of the named property.
     * <p>
     * The default implementation uses the {@link ValueFactory} of the
     * current {@link Session} to create a {@link Value} instance of the
     * given type from the given string value and forwards the call to the
     * {@link Node#setProperty(String, Value)} method.
     *
     * @param name property name
     * @param value string value
     * @param type property type
     * @return modified property
     * @throws RepositoryException if an error occurs
     */
    public Property setProperty(String name, String value, int type)
            throws RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value, type));
    }

    //-------------------------------------------------------------< private >

    /**
     * Returns the prefixed JCR name for the namespace URI and local name
     * using the current namespace mappings.
     *
     * @param uri namespace URI
     * @param name namespace-local name
     * @return prefixed JCR name
     * @throws RepositoryException if an error occurs
     */
    private String getName(String name) throws RepositoryException {
        return new NamespaceHelper(getSession()).getJcrName(name);
    }

}
