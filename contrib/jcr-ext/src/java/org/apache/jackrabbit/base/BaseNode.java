/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.base;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.name.Path;

/**
 * Node base class.
 */
public class BaseNode extends BaseItem implements Node {

    /** Protected constructor. This class is only useful when extended. */
    protected BaseNode() {
    }

    /**
     * Implemented by calling <code>super.getPath()</code>
     * (see {@link BaseItem#getPath() BaseItem.getPath()} and appending
     * <code>"[" + getIndex() + "]"</code> if
     * <code>getDefinition().allowsSameNameSiblings()</code> returns
     * <code>true</code>.
     * {@inheritDoc}
     */
    public String getPath() throws RepositoryException {
        if (getDefinition().allowsSameNameSiblings()) {
            return super.getPath() + "[" + getIndex() + "]";
        } else {
            return super.getPath();
        }
    }

    /**
     * Implemented by calling <code>visitor.visit(this)</code>.
     * {@inheritDoc}
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }

    /** Always returns <code>true</code>. {@inheritDoc} */
    public boolean isNode() {
        return true;
    }

    /** Not implemented. {@inheritDoc} */
    public Node addNode(String relPath) throws ItemExistsException,
            PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by calling
     * <code>addNode(relPath).setProperty(prefix + ":primaryType", primaryNodeTypeName)</code>
     * and returning the created node. The prefix is acquired by calling
     * <code>getSession().getNamespacePrefix("http://www.jcp.org/jcr/1.0")</code>.
     * {@inheritDoc}
     */
    public Node addNode(String relPath, String primaryNodeTypeName)
            throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException,
            ConstraintViolationException, RepositoryException {
        Node node = addNode(relPath);
        String prefix =
            getSession().getNamespacePrefix("http://www.jcp.org/jcr/1.0");
        node.setProperty(prefix + ":primaryType", primaryNodeTypeName);
        return node;
    }

    /** Not implemented. {@inheritDoc} */
    public void orderBefore(String srcChildRelPath, String destChildRelPath)
            throws UnsupportedRepositoryOperationException, VersionException,
            ConstraintViolationException, ItemNotFoundException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by calling <code>getProperty(name).setValue(value)</code>
     * and returning the retrieved property. Adding new properties is not
     * implemented.
     * {@inheritDoc}
     */
    public Property setProperty(String name, Value value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        try {
            Property property = getProperty(name);
            property.setValue(value);
            return property;
        } catch (PathNotFoundException e) {
            throw new UnsupportedRepositoryOperationException();
        }
    }

    /**
     * Converts a value to the given type. A new value instance is
     * created using the current value factory
     * (<code>getSession().getValueFactory()</code>) unless the given
     * value already is of the given type or the given type is undefined.
     * <p>
     * This internal utility method is used by the property setters
     * in this class.
     *
     * @param value original value
     * @param type  value type
     * @return converted value
     * @throws ValueFormatException if the value can not be converted
     * @throws RepositoryException  if another error occurs
     */
    private Value convert(Value value, int type)
            throws ValueFormatException, RepositoryException {
        if (type == PropertyType.UNDEFINED || value.getType() == type) {
            return value;
        } else {
            ValueFactory factory = getSession().getValueFactory();
            switch (type) {
            case PropertyType.BINARY:
                return factory.createValue(value.getStream());
            case PropertyType.BOOLEAN:
                return factory.createValue(value.getBoolean());
            case PropertyType.DATE:
                return factory.createValue(value.getDate());
            case PropertyType.DOUBLE:
                return factory.createValue(value.getDouble());
            case PropertyType.LONG:
                return factory.createValue(value.getLong());
            case PropertyType.STRING:
                return factory.createValue(value.getString());
            default:
                return factory.createValue(value.getString(), type);
            }
        }
    }

    /**
     * Implemented by calling
     * <code>setProperty(name, value)</code> after converting the given
     * value to the given type.
     * {@inheritDoc}
     */
    public Property setProperty(String name, Value value, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        return setProperty(name, convert(value, type));
    }

    /**
     * Implemented by calling <code>getProperty(name).setValue(values)</code>
     * and returning the retrieved property. Adding new properties is not
     * implemented.
     * {@inheritDoc}
     */
    public Property setProperty(String name, Value[] values)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        try {
            Property property = getProperty(name);
            property.setValue(values);
            return property;
        } catch (PathNotFoundException e) {
            throw new UnsupportedRepositoryOperationException();
        }
    }

    /**
     * Implemented by calling <code>setProperty(name, convertedValues)</code>
     * with an array of values that were converted from the given values to
     * the given type.
     * {@inheritDoc}
     */
    public Property setProperty(String name, Value[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        Value[] convertedValues = new Value[values.length];
        for (int i = 0; i < values.length; i++) {
            convertedValues[i] = convert(values[i], type);
        }
        return setProperty(name, convertedValues);
    }

    /**
     * Implemented by calling <code>setProperty(name, stringValues)</code>
     * with an array of values that were created from the given strings by
     * <code>getSession().getValueFactory().createValue(values[i]))</code>.
     * {@inheritDoc}
     */
    public Property setProperty(String name, String[] values)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        Value[] stringValues = new Value[values.length];
        for (int i = 0; i < values.length; i++) {
            stringValues[i] = factory.createValue(values[i]);
        }
        return setProperty(name, stringValues);
    }

    /**
     * Implemented by calling <code>setProperty(name, stringValues, type)</code>
     * with an array of Values that were created from the given strings by
     * <code>getSession().getValueFactory().createValue(values[i]))</code>.
     * {@inheritDoc}
     */
    public Property setProperty(String name, String[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        Value[] stringValues = new Value[values.length];
        for (int i = 0; i < values.length; i++) {
            stringValues[i] = factory.createValue(values[i]);
        }
        return setProperty(name, stringValues, type);
    }

    /**
     * Implemented by calling
     * <code>setProperty(name, getSession().getValueFactory().createValue(value)</code>.
     * {@inheritDoc}
     */
    public Property setProperty(String name, String value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Implemented by calling
     * <code>setProperty(name, getSession().getValueFactory().createValue(value, type)</code>.
     * {@inheritDoc}
     */
    public Property setProperty(String name, String value, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value, type));
    }

    /**
     * Implemented by calling
     * <code>setProperty(name, getSession().getValueFactory().createValue(value)</code>.
     * {@inheritDoc}
     */
    public Property setProperty(String name, InputStream value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Implemented by calling
     * <code>setProperty(name, getSession().getValueFactory().createValue(value)</code>.
     * {@inheritDoc}
     */
    public Property setProperty(String name, boolean value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Implemented by calling
     * <code>setProperty(name, getSession().getValueFactory().createValue(value)</code>.
     * {@inheritDoc}
     */
    public Property setProperty(String name, double value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Implemented by calling
     * <code>setProperty(name, getSession().getValueFactory().createValue(value)</code>.
     * {@inheritDoc}
     */
    public Property setProperty(String name, long value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Implemented by calling
     * <code>setProperty(name, getSession().getValueFactory().createValue(value)</code>.
     * {@inheritDoc}
     */
    public Property setProperty(String name, Calendar value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Implemented by calling
     * <code>setProperty(name, getSession().getValueFactory().createValue(value)</code>.
     * {@inheritDoc}
     */
    public Property setProperty(String name, Node value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        ValueFactory factory = getSession().getValueFactory();
        return setProperty(name, factory.createValue(value));
    }

    /**
     * Implemented by calling <code>Path.resolve(this, relPath)</code> from
     * the {@link Path Path} utility class. If the given path resolves to
     * a property, then a {@link PathNotFoundException PathNotFoundException}
     * is thrown.
     * {@inheritDoc}
     */
    public Node getNode(String relPath) throws PathNotFoundException,
            RepositoryException {
        Item item = Path.resolve(this, relPath);
        if (item.isNode()) {
            return (Node) item;
        } else {
            throw new PathNotFoundException("Node not found: " + relPath);
        }
    }

    /** Not implemented. {@inheritDoc} */
    public NodeIterator getNodes() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by calling <code>Path.resolve(this, relPath)</code> from
     * the {@link Path Path} utility class. If the given path resolves to
     * a node, then a {@link PathNotFoundException PathNotFoundException}
     * is thrown.
     * {@inheritDoc}
     */
    public Property getProperty(String relPath) throws PathNotFoundException,
            RepositoryException {
        Item item = Path.resolve(this, relPath);
        if (item.isNode()) {
            throw new PathNotFoundException("Property not found: " + relPath);
        } else {
            return (Property) item;
        }
    }

    /** Not implemented. {@inheritDoc} */
    public PropertyIterator getProperties() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public PropertyIterator getProperties(String namePattern)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public Item getPrimaryItem() throws ItemNotFoundException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public String getUUID() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Always returns <code>1</code>. {@inheritDoc} */
    public int getIndex() throws RepositoryException {
        return 1;
    }

    /** Not implemented. {@inheritDoc} */
    public PropertyIterator getReferences() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by calling <code>getNode(relPath)</code> and returning
     * <code>true</code> unless a
     * {@link PathNotFoundException PathNotFoundException} is thrown.
     * {@inheritDoc}
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
     * Implemented by calling <code>getProperty(relPath)</code> and returning
     * <code>true</code> unless a
     * {@link PathNotFoundException PathNotFoundException} is thrown.
     * {@inheritDoc}
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
     * Implemented by calling <code>getNodes().hasNext()</code>.
     * {@inheritDoc}
     */
    public boolean hasNodes() throws RepositoryException {
        return getNodes().hasNext();
    }

    /**
     * Implemented by calling <code>getProperties().hasNext()</code>.
     * {@inheritDoc}
     */
    public boolean hasProperties() throws RepositoryException {
        return getProperties().hasNext();
    }

    /** Not implemented. {@inheritDoc} */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by calling <code>type.isNodeType(nodeTypeName)</code>
     * for the primary type and all mixin types of this node. Returns
     * <code>true</code> if any of these calls return <code>true</code>.
     * Returns <code>false</code> otherwise.
     * {@inheritDoc}
     */
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        if (getPrimaryNodeType().isNodeType(nodeTypeName)) {
            return true;
        } else {
            NodeType[] types = getMixinNodeTypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i].isNodeType(nodeTypeName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Not implemented. {@inheritDoc} */
    public void addMixin(String mixinName) throws NoSuchNodeTypeException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public boolean canAddMixin(String mixinName) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public NodeDefinition getDefinition() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public Version checkin() throws VersionException,
            UnsupportedRepositoryOperationException, InvalidItemStateException,
            LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public void checkout() throws UnsupportedRepositoryOperationException,
            LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public void doneMerge(Version version) throws VersionException,
            InvalidItemStateException, UnsupportedRepositoryOperationException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public void cancelMerge(Version version) throws VersionException,
            InvalidItemStateException, UnsupportedRepositoryOperationException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public void update(String srcWorkspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException,
            LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort)
            throws UnsupportedRepositoryOperationException,
            NoSuchWorkspaceException, AccessDeniedException, VersionException,
            LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public String getCorrespondingNodePath(String workspaceName)
            throws ItemNotFoundException, NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public boolean isCheckedOut() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public void restore(String versionName, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public void restore(Version version, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public void restore(Version version, String relPath, boolean removeExisting)
            throws PathNotFoundException, ItemExistsException,
            VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public void restoreByLabel(String versionLabel, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public VersionHistory getVersionHistory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public Version getBaseVersion()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public Lock lock(boolean isDeep, boolean isSessionScoped)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public Lock getLock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public void unlock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public boolean holdsLock() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Not implemented. {@inheritDoc} */
    public boolean isLocked() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

}
