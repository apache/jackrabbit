/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.extension.configuration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationKey;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.extension.ExtensionDescriptor;

/**
 * The <code>ItemConfiguration</code> extends the
 * <code>HierarchicalConfiguration</code> class providing support to load the
 * configuration from a repository. It represents the repository subtree from
 * which the configuration is loaded as a configuration tree of configuration
 * nodes and attributes.
 * <p>
 * The configuration is rooted at a user supplied repository node which must be
 * defined such, that properties and child nodes of any type and name may be
 * added. The best way to achieve this is to define the node as of type
 * <code>nt:unstructured</code>.
 * <p>
 * <b>Note on names</b>
 * <p>
 * This implementation uses the repository item names as (basis of) the names of
 * the hierarchy configuration nodes. As such there exists a restriction on
 * those names: The <code>HierarchicalConfiguration</code> extended by this
 * class uses dots (<code>.</code>) as hierarchy level separators. Therefore
 * any configuration node's name with a dot in it will likely lead to unsuable
 * configuration.
 * <p>
 * <i>Therefore it is strongly recommended to not use dots in repository element
 * names to be used by this configuration class.</i>
 * <p id="dataTypeConversion">
 * <b>Data Type Conversion</b>
 * <p>
 * This implementation tries its best to preserve the configuration data type
 * when loading or saving the configuration data. Because the mapping between
 * Java data types supported by the configuration objects and the data types
 * supported by the repository, a mapping has to be applied, which may lead to a
 * certain but acceptable loss of accuracy.
 * <p>
 * When loading values from the repository, the following type conversion
 * applies: <table>
 * <tr>
 * <th>JCR Type
 * <th>Java Type</tr>
 * <tr>
 * <td>Boolean
 * <td>Boolean</tr>
 * <tr>
 * <td>Date
 * <td>Calendar</tr>
 * <tr>
 * <td>Double
 * <td>Double</tr>
 * <tr>
 * <td>Long
 * <td>Long</tr>
 * <tr>
 * <td>Binary, Name, Path, Reference, String, Undefined
 * <td>String</tr>
 * </table>
 * <p>
 * When saveing configuaration data to the repository, the following type
 * conversion applies: <table>
 * <tr>
 * <th>Java Type
 * <th>JCR Type</tr>
 * <tr>
 * <td>String
 * <td>String</tr>
 * <tr>
 * <td>Boolean
 * <td>Boolean</tr>
 * <tr>
 * <td>Calendar
 * <td>Date</tr>
 * <tr>
 * <td>Double or Float
 * <td>Double</tr>
 * <tr>
 * <td>Number except Double and Float
 * <td>Long</tr>
 * <tr>
 * <td>Other types, incl. <code>null</code>
 * <td>String</tr>
 * </table>
 *
 * @author Felix Meschberger
 */
public class ItemConfiguration extends HierarchicalConfiguration implements
        RepositoryConfiguration {

    /** default log */
    private static final Log log = LogFactory.getLog(ExtensionDescriptor.class);

    /**
     * The name of the property providing the configuration value of a
     * configuration node.
     */
    private static final String NODE_CONTENT_PROPERTY = "__DEFAULT__";

    /**
     * The <code>Node</code> to which this configuration is attached. The
     * configuration data itself is loaded and saved from/to the
     * <code>configuration</code> child node of this node.
     *
     * @see #load(javax.jcr.Node)
     * @see #save(javax.jcr.Node)
     */
    private javax.jcr.Node jcrNode;

    /**
     * The backlog of absolute paths of items which backed removed configuration
     * data. This set is worked through to remove the items when the
     * configuration is saved.
     *
     * @see #save(javax.jcr.Node)
     * @see ItemNode#removeReference()
     */
    private Set deleteBackLog;

    /**
     * Creates an empty configuration not hooked to any node.
     */
    public ItemConfiguration() {
        super();
    }

    /**
     * Creates a configuration attached to the given <code>node</code> and
     * load the configuration data from the <code>configuration</code> child
     * node.
     * <p>
     * If <code>node</code> is <code>null</code>, this constructor has the same
     * effect as the default constructor ({@link #ItemConfiguration()} in that
     * this configuration is not attached to a <code>Node</code> and
     * configuration is not loaded.
     *
     * @param node The <code>Node</code> containing the configuration data.
     *
     * @throws ConfigurationException If an error occurrs loading the
     *      configuration data.
     */
    public ItemConfiguration(javax.jcr.Node node) throws ConfigurationException {
        super();

        setNode(node);
        load();
    }

    /**
     * Returns the <code>Node</code> to which this configuration is attached.
     * If this configuration is not attached to a node, this method returns
     * <code>null</code>.
     */
    public javax.jcr.Node getNode() {
        return jcrNode;
    }

    /**
     * Attaches this configuration to the given node to provide
     * ({@link #load(javax.jcr.Node)}) or take ({@link #save(javax.jcr.Node)})
     * configuration data. To detach this configuration from the repository,
     * set <code>node</code> to <code>null</code>.
     *
     * @param node The <code>Node</code> to which this configuration is
     *            attached or <code>null</code> to detach the configuration.
     */
    public void setNode(javax.jcr.Node node) {
        // if the new node is different from the old node, remove the current
        // configuration's references
        if (isDifferent(node)) {
            removeReferences(getRoot());
        }

        // set the new node
        this.jcrNode = node;
    }

    /**
     * Creates an instance of the <code>ItemNode</code> class with an empty
     * reference.
     * <p>
     * As noted in the class comment, the name should not contain a dot,
     * otherwise the <code>HierarchicalConfiguration</code> class will have
     * problems resolving the configuration.
     *
     * @param name The name of the new configuratio node.
     */
    protected Node createNode(String name) {
        return new ItemNode(name, null);
    }

    /**
     * Loads the configuration data from the <code>Node</code> to which this
     * configuration is attached. If this configuration is not attached to
     * a <code>Node</code>, this method has no effect.
     * <p>
     * If configuration data is already present in this configuration, the data
     * is extended by the data loaded from the <code>Node</code>. To prevent
     * such additions, clear this configuration before loading new data.
     *
     * @throws ConfigurationException If an error occurrs loading the
     *      configuration data.
     *
     * @see #load(javax.jcr.Node)
     */
    public void load() throws ConfigurationException {
        if (jcrNode != null) {
            load(jcrNode);
        }
    }

    /**
     * Loads the configuration data from the given <code>node</code>. If
     * <code>node</code> is <code>null</code>, a <code>NullPointerException</code>
     * is thrown.
     * <p>
     * If configuration data is already present in this configuration, the data
     * is extended by the data loaded from the <code>Node</code>. To prevent
     * such additions, clear this configuration before loading new data.
     *
     * @param node The <code>Node</code> containing the configuration to be
     *      loaded into this configuration. This must no be <code>null</code>.
     *
     * @throws NullPointerException if <code>node</code> is <code>null</code>.
     * @throws ConfigurationException If an error occurrs loading the
     *      configuration data.
     */
    public void load(javax.jcr.Node node) throws ConfigurationException {
        try {
            boolean sameNode = !isDifferent(node);

            // construct the hierarchy and record references if loading
            // from the node this configuration is attached to
            constructHierarchy(getRoot(), node, sameNode);
        } catch (RepositoryException re) {
            throw new ConfigurationException(re);
        }
    }

    /**
     * Saves the configuration data to the <code>Node</code> to which this
     * configuration is attached. If this configuration is not attached to
     * a <code>Node</code>, this method has no effect.
     *
     * @throws ConfigurationException If an error occurrs saving the
     *      configuration data.
     *
     * @see #save(javax.jcr.Node)
     */
    public void save() throws ConfigurationException {
        if (jcrNode != null) {
            save(jcrNode);
        }
    }

    /**
     * Saves the configuration data to the given <code>node</code>. If
     * <code>node</code> is <code>null</code>, a <code>NullPointerException</code>
     * is thrown.
     *
     * @param node The <code>Node</code> to store the configuration to. This
     *      must no be <code>null</code>.
     *
     * @throws NullPointerException if <code>node</code> is <code>null</code>.
     * @throws ConfigurationException If an error occurrs saving the
     *      configuration data.
     */
    public void save(javax.jcr.Node node) throws ConfigurationException {
        boolean lockable = false;
        try {
            // remove the node references from the current configuration
            // nodes if the destination is different from the node to which
            // the configuration is attached
            if (isDifferent(node)) {
                removeReferences(getRoot());
            }

            lockable = node.isNodeType("mix:lockable");
            if (lockable) {
                if (node.isLocked()) {
                    // trick: reset lockable to not unlock in finally{}
                    lockable = false;
                    throw new ConfigurationException("Configuration node is locked");
                }

                // deep session lock
                node.lock(true, true);
            }

            // check whether the node is versionable
            boolean versionable = node.isNodeType("mix:versionable");

            // make sure the node is checked out for modification
            if (versionable && !node.isCheckedOut()) {
                node.checkout();
            }

            // remove all items which have to be removed because the
            // configuration which were backed by them has been removed
            if (deleteBackLog != null) {
                Session session = node.getSession();
                for (Iterator di=deleteBackLog.iterator(); di.hasNext(); ) {
                    String itemPath = (String) di.next();
                    try {
                        session.getItem(itemPath).remove();
                    } catch (PathNotFoundException pnfe) {
                        // might have already been removed, ignore
                        log.debug("Item " + itemPath + " cannot be accessed for removal",
                            pnfe);
                    }
                }
            }

            // store now
            ItemBuilderVisitor builder = new ItemBuilderVisitor(node);
            builder.processDocument(getRoot());

            // save modifications
            node.save();

            // checkin after saving
            if (versionable) {
                node.checkin();
            }

        } catch (RepositoryException re) {
            throw new ConfigurationException("Cannot save configuration", re);
        } finally {
            // if the node is still modified, this is an error and we
            // rollback
            try {
                if (node.isModified()) {
                    node.refresh(false);
                } else {
                    // reset deleteBackLog, because all items have been removed
                    // and need not be removed the next time.
                    // (If an error occurred saving the configuration, the back
                    // log must remain, such that the deleted items may be
                    // removed the next time, save() is called).
                    deleteBackLog = null;
                }
            } catch (RepositoryException re) {
                log.error("Problem refreshing persistent config state", re);
            }

            // unlock the node again
            try {
                if (lockable && node.isLocked()) {
                    node.unlock();
                }
            } catch (RepositoryException re) {
                log.warn("Cannot unlock configuration node", re);
            }
        }
    }

    /**
     * Returns <code>true</code> if <code>newNode</code> is not the same
     * repository <code>Node</code> as the <code>Node</code> to which this
     * configuration is currently associated.
     * <p>
     * Removing the references makes sure that the complete configuration data
     * is written to the repository the next time {@link #save()} is called.
     *
     * @param newNode The repository <code>Node</code> to which the current
     *      base <code>Node</code> is compared.
     *
     * @return <code>true</code> if <code>newNode</code> is different to the
     *      <code>Node</code> to which the configuration is currently attached.
     */
    private boolean isDifferent(javax.jcr.Node newNode) {
        // return false if the objects are the same
        if (jcrNode == newNode) {
            return false;
        }

        // return true if no node yet and new is not null
        if (jcrNode == null) {
            return newNode != null;
        }

        // return true if the new node is null and the old is set
        if (newNode == null) {
            return jcrNode != null;
        }

        // otherwise try to compare the new to the old node
        try {
            return !jcrNode.isSame(newNode);
        } catch (RepositoryException re) {
            // cannot check whether the nodes are different, assume yes
            log.warn("Cannot check whether the current and new nodes " +
                "are different, assuming they are", re);
        }

        // fallback to different in case of problems
        return true;
    }

    /**
     * Vists all configuration nodes starting from the given <code>node</code>
     * and resets all node's reference fields to <code>null</code>. This forces
     * complete configuration storage on the next call to the {@link #save()} or
     * {@link #save(javax.jcr.Node)} methods.
     *
     * @param node The <code>Node</code> at which to start removing references
     */
    private static void removeReferences(Node node) {
        // remove repository item references from the nodes
        node.visit(new NodeVisitor() {
            public void visitBeforeChildren(Node node, ConfigurationKey key) {
                node.setReference(null);
            };
        }, null);
    }

    /**
     * Creates the internal configuration hierarchy of {@link ItemNode}s from
     * the items in the repository.
     *
     * @param node The configuration node to which the new configuration is
     *      attached.
     * @param element The JCR <code>Node</code> from which the configuration
     *      is read.
     * @param elemRefs <code>true</code> if the configuration nodes created
     *      while reading the repository items get the reference fields set to
     *      the corresponding repository item.
     *
     * @throws RepositoryException If an error occurrs reading from the
     *      repository.
     */
    private void constructHierarchy(Node node, javax.jcr.Node element,
            boolean elemRefs) throws RepositoryException {

        // create attribute child nodes for the element's properties
        processAttributes(node, element, elemRefs);

        // read the element's child nodes as child nodes into the configuration
        NodeIterator list = element.getNodes();
        while (list.hasNext()) {
            javax.jcr.Node jcrNode = list.nextNode();

            // ignore protected nodes
            if (jcrNode.getDefinition().isProtected()) {
                continue;
            }

            Node childNode = new ItemNode(jcrNode.getName(),
                elemRefs ? jcrNode.getPath() : null);
            constructHierarchy(childNode, jcrNode, elemRefs);
            node.addChild(childNode);
        }
    }

    /**
     * Helper method for constructing node objects for the attributes of the
     * given XML element.
     *
     * @param node the actual node
     * @param element the actual XML element
     * @param elemRefs a flag whether references to the XML elements should be
     *            set
     * @param node The configuration node to which the new configuration is
     *      attached.
     * @param element The JCR <code>Node</code> whose properties are to be
     *      read and attached.
     * @param elemRefs <code>true</code> if the configuration nodes created
     *      while reading the properties get the reference fields set to the
     *      corresponding property.
     *
     * @throws RepositoryException If an error occurrs reading from the
     *      repository.
     */
    private void processAttributes(Node node, javax.jcr.Node element,
            boolean elemRefs) throws RepositoryException {

        PropertyIterator attributes = element.getProperties();
        while (attributes.hasNext()) {
            Property prop = attributes.nextProperty();

            // ignore protected properties
            if (prop.getDefinition().isProtected()) {
                continue;
            }

            Value[] values;
            if (prop.getDefinition().isMultiple()) {
                values = prop.getValues();
            } else {
                values = new Value[] { prop.getValue() };
            }

            if (NODE_CONTENT_PROPERTY.equals(prop.getName())) {
                // this is the value of the node itself
                // only consider the first value
                if (values.length > 0) {
                    node.setValue(importValue(values[0]));
                }
            } else {
                String name = ConfigurationKey.constructAttributeKey(prop.getName());
                String ref = elemRefs ? prop.getPath() : null;
                for (int i = 0; i < values.length; i++) {
                    Node child = new ItemNode(name, ref);
                    child.setValue(importValue(values[i]));
                    node.addChild(child);
                }
            }
        }
    }

    /**
     * The <code>ItemNode</code> class extends the standard <code>Node</code>
     * class by support for removing underlying repository items in case of
     * removal of a configuration node.
     *
     * @author Felix Meschberger
     */
    private class ItemNode extends Node {

        /*
         * This class is not static to have a reference to the owning instance
         * such that the deleteBackLog set may be accessed which is used to
         * record items to be removed due to ItemNode removals
         */

        /** fake serialVersionUID */
        private static final long serialVersionUID = 1L;

        /**
         * Creates an instance of this node type presetting the reference.
         *
         * @param name The name of the new configuration node.
         * @param reference The (optional) reference to initially set on the
         *      new configuration node. This may be <code>null</code>.
         */
        protected ItemNode(String name, String reference) {
            super(name);
            setReference(reference);
        }

        /**
         * Removes the associated repository item if this node is removed
         * from the configuration.
         */
        protected void removeReference() {
            if (getReference() != null) {

                if (ConfigurationKey.isAttributeKey(getName())) {
                    List list = getParent().getChildren(getName());
                    if (list != null && list.size() > 0) {
                        for (Iterator ci=list.iterator(); ci.hasNext(); ) {
                            // clear references of sibblings
                            ((Node) ci.next()).setReference(null);
                        }
                    }
                }

                if (deleteBackLog == null) {
                    deleteBackLog = new HashSet();
                }
                deleteBackLog.add(getReference());
            }
        }
    }

    /**
     * The <code>ItemBuilderVisitor</code> class stores the configuration
     * rooted at a given <code>Node</code> to the repository <code>Node</code>
     * defined at construction time.
     * <p>
     * This visitor just adds nodes and properties to the repository and does
     * not care whether the operations actually overwrite data or not. It is
     * recommended that the JCR <code>Node</code> from which the visitor is
     * created be cleared before processing the configuration through the
     * {@link #processDocument(Node)} method.
     *
     * @author Felix Meschberger
     */
    private static class ItemBuilderVisitor extends BuilderVisitor {

        /** Stores the document to be constructed. */
        private javax.jcr.Node jcrNode;

        /**
         * Creates a new instance of <code>ItemBuilderVisitor</code> storing the
         * configuration at and below the given <code>jcrNode</code>.
         *
         * @param jcrNode The JCR <code>Node</code> to take the configuration.
         */
        public ItemBuilderVisitor(javax.jcr.Node jcrNode) {
            this.jcrNode = jcrNode;
        }

        /**
         * Processes the node hierarchy and adds new items to the repository
         *
         * @param rootNode The configuration <code>Node</code> to start at in
         *      the configuration hierarchy.
         */
        public void processDocument(Node rootNode) throws RepositoryException {
            rootNode.setReference(jcrNode.getPath());
            rootNode.visit(this, null);
        }

        /**
         * Inserts a new node. This implementation ensures that the correct XML
         * element is created and inserted between the given siblings.
         *
         * @param newNode the node to insert
         * @param parent the parent node
         * @param sibling1 the first sibling
         * @param sibling2 the second sibling
         * @return the new node
         */
        protected Object insert(Node newNode, Node parent, Node sibling1,
            Node sibling2) {

            try {
                // get the parent's owning node
                javax.jcr.Node parentNode;
                if (parent.getName() == null) {
                    parentNode = jcrNode;
                } else {
                    String ref = (String) parent.getReference();
                    parentNode = (javax.jcr.Node) jcrNode.getSession().getItem(ref);
                }

                // if the configuration node is an attribute, set the respective
                // property and return.
                if (ConfigurationKey.isAttributeKey(newNode.getName())) {
                    updateAttribute(parent, parentNode, newNode.getName());
                    return null;
                }

                // create the repository node for the configuration node
                javax.jcr.Node elem = parentNode.addNode(newNode.getName());

                // if the configuration node has a value, set the __DEFAULT__
                // property to this value
                if (newNode.getValue() != null) {
                    Value value =
                        exportValue(elem.getSession().getValueFactory(),
                            newNode.getValue());
                    elem.setProperty(NODE_CONTENT_PROPERTY, value);
                }

                // order before sibling2 if defined, ignore sibling1
                if (parentNode.getPrimaryNodeType().hasOrderableChildNodes()) {
                    if (sibling2 != null) {
                        parentNode.orderBefore(newNode.getName(),
                            sibling2.getName());
                    }
                }

                return elem.getPath();
            } catch (RepositoryException re) {
                log.warn("Cannot update repository for configuration node " +
                    newNode.getName(), re);
            }

            // fallback to returning nothing
            return null;
        }

        /**
         * Helper method for updating the value of the specified node's
         * attribute with the given name.
         *
         * @param node the affected node
         * @param elem the element that is associated with this node
         * @param name the name of the affected attribute
         */
        private void updateAttribute(Node node, javax.jcr.Node elem,
            String name) throws RepositoryException {
            if (node != null && elem != null) {
                String propName = ConfigurationKey.attributeName(name);

                // copy the values of all like named attributes to another list
                List attrs = node.getChildren(name);
                List values = new ArrayList();
                for (Iterator ai = attrs.iterator(); ai.hasNext();) {
                    Node attr = (Node) ai.next();
                    if (attr.getValue() != null) {
                        values.add(attr.getValue());
                    }
                }

                // remove property before trying to set
                if (elem.hasProperty(propName)) {
                    elem.getProperty(propName).remove();
                }

                Property attrProp;
                ValueFactory vf = elem.getSession().getValueFactory();
                if (values.size() == 0) {
                    // no attribute values
                    attrProp = null;
                } else if (values.size() == 1) {
                    // single valued property
                    attrProp =
                        elem.setProperty(propName, exportValue(vf, values.get(0)));
                } else {
                    Value[] valArray = new Value[values.size()];
                    for (int i = 0; i < valArray.length; i++) {
                        valArray[i] = exportValue(vf, values.get(i));
                    }
                    attrProp = elem.setProperty(propName, valArray);
                }

                // set the references on the attribute nodes
                String ref = attrProp != null ? attrProp.getPath() : null;
                for (Iterator ai = attrs.iterator(); ai.hasNext();) {
                    Node attr = (Node) ai.next();
                    attr.setReference(ref);
                }
            }
        }
    }

    //---------- Data type helpers for loading and storing --------------------

    /**
     * Converts the JCR <code>Value</code> object to a configuration value of
     * the corresponding runtime Java type. See the <a
     * href="#dataTypeConversion">class comment</a> for information on the type
     * conversion applied.
     *
     * @param jcrValue The JCR <code>Value</code> to convert into a
     *            configuration value object.
     * @return The configuration value object.
     * @throws NullPointerException if <code>jcrValue</code> is
     *             <code>null</code>.
     */
    private static Object importValue(Value jcrValue)
            throws RepositoryException {

        switch (jcrValue.getType()) {
            case PropertyType.BOOLEAN:
                return new Boolean(jcrValue.getBoolean());
            case PropertyType.DATE:
                return jcrValue.getDate();
            case PropertyType.DOUBLE:
                return new Double(jcrValue.getDouble());
            case PropertyType.LONG:
                return new Long(jcrValue.getLong());
            default:
                // Binary, Name, Path, Reference, String, Undefined
                return jcrValue.getString();
        }
    }

    /**
     * Converts the value object to a JCR <code>Value</code> instance
     * according to the runtime type of the <code>value</code>. See the <a
     * href="#dataTypeConversion">class comment</a> for information on the type
     * conversion applied.
     *
     * @param vf The <code>ValueFactory</code> used to create JCR
     *            <code>Value</code> objects.
     * @param value The configuration value to convert (export) to a JCR
     *            <code>Value</code> object.
     * @return The JCR <code>Value</code> object representing the
     *         configuration value.
     */
    private static Value exportValue(ValueFactory vf, Object value) {
        if (value instanceof String) {
            return vf.createValue((String) value);
        } else if (value instanceof Boolean) {
            return vf.createValue(((Boolean) value).booleanValue());
        } else if (value instanceof Calendar) {
            return vf.createValue((Calendar) value);
        } else if (value instanceof Double || value instanceof Float) {
            // handle float and double values as double
            return vf.createValue(((Number) value).doubleValue());
        } else if (value instanceof Number) {
            // handle other numbers (float and double above) as long
            return vf.createValue(((Number) value).longValue());
        } else {
            return vf.createValue(String.valueOf(value));
        }
    }
}
