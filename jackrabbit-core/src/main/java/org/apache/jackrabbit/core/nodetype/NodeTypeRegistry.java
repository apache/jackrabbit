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
package org.apache.jackrabbit.core.nodetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.cluster.NodeTypeEventChannel;
import org.apache.jackrabbit.core.cluster.NodeTypeEventListener;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeDefDiff;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

/**
 * A <code>NodeTypeRegistry</code> ...
 */
public class NodeTypeRegistry implements NodeTypeEventListener {

    private static Logger log = LoggerFactory.getLogger(NodeTypeRegistry.class);

    private static final String BUILTIN_NODETYPES_RESOURCE_PATH =
            "org/apache/jackrabbit/core/nodetype/builtin_nodetypes.cnd";

    private static final String CUSTOM_NODETYPES_RESOURCE_NAME =
            "/nodetypes/custom_nodetypes.xml";

    /**
     * Feature flag for the unfortunate behavior in Jackrabbit 2.1 and 2.2
     * where the exception from {@link #checkForReferencesInContent(Name)}
     * was never thrown because of a mistaken commit for
     * <a href="https://issues.apache.org/jira/browse/JCR-2587">JCR-2587</a>.
     * Setting this flag to <code>true</code> (the default value comes from
     * the "disableCheckForReferencesInContentException" system property)
     * will disable the exception thrown by default by the method.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-3223">JCR-3223</a>
     */
    public static volatile boolean disableCheckForReferencesInContentException =
            Boolean.getBoolean("disableCheckForReferencesInContentException");

    /**
     * resource holding custom node type definitions which are represented as
     * nodes in the repository; it is needed in order to make the registrations
     * persistent.
     */
    private final FileSystemResource customNodeTypesResource;

    // persistent node type definitions of built-in & custom node types
    private final NodeTypeDefStore builtInNTDefs;
    private final NodeTypeDefStore customNTDefs;

    // cache of pre-built aggregations of node types
    private EffectiveNodeTypeCache entCache;

    // map of node type names and node type definitions
    private final Map<Name, QNodeTypeDefinition> registeredNTDefs;

    // definition of the root node
    private final QNodeDefinition rootNodeDef;

    /**
     * namespace registry for resolving prefixes and namespace URI's;
     * used for (de)serializing node type definitions
     */
    private final NamespaceRegistry nsReg;

    /**
     * Listeners (weak references)
     */
    private final Map<NodeTypeRegistryListener, NodeTypeRegistryListener> listeners =
            Collections.synchronizedMap(new ReferenceMap<>(ReferenceStrength.WEAK, ReferenceStrength.WEAK));

    /**
     * Node type event channel.
     */
    private NodeTypeEventChannel eventChannel;

    //----------------------------------------< public NodeTypeRegistry 'api' >
    /**
     * Returns the names of all registered node types. That includes primary
     * and mixin node types.
     *
     * @return the names of all registered node types.
     */
    public Name[] getRegisteredNodeTypes() {
        return registeredNTDefs.keySet().toArray(new Name[registeredNTDefs.size()]);
    }

    /**
     * Validates the <code>NodeTypeDef</code> and returns
     * an  <code>EffectiveNodeType</code> object representing the newly
     * registered node type.
     * <p>
     * The validation includes the following checks:
     * <ul>
     * <li>Supertypes must exist and be registered</li>
     * <li>Inheritance graph must not be circular</li>
     * <li>Aggregation of supertypes must not result in name conflicts,
     * ambiguities, etc.</li>
     * <li>Definitions of auto-created properties must specify a name</li>
     * <li>Default values in property definitions must satisfy value constraints
     * specified in the same property definition</li>
     * <li>Definitions of auto-created child-nodes must specify a name</li>
     * <li>Default node type in child-node definitions must exist and be
     * registered</li>
     * <li>The aggregation of the default node types in child-node definitions
     * must not result in name conflicts, ambiguities, etc.</li>
     * <li>Definitions of auto-created child-nodes must not specify default
     * node types which would lead to infinite child node creation
     * (e.g. node type 'A' defines auto-created child node with default
     * node type 'A' ...)</li>
     * <li>Node types specified as constraints in child-node definitions
     * must exist and be registered</li>
     * <li>The aggregation of the node types specified as constraints in
     * child-node definitions must not result in name conflicts, ambiguities,
     * etc.</li>
     * <li>Default node types in child-node definitions must satisfy
     * node type constraints specified in the same child-node definition</li>
     * </ul>
     *
     * @param ntd the definition of the new node type
     * @return an <code>EffectiveNodeType</code> instance
     * @throws InvalidNodeTypeDefException if the given node type definition is invalid.
     * @throws RepositoryException if a repository error occurs.
     */
    public EffectiveNodeType registerNodeType(QNodeTypeDefinition ntd)
            throws InvalidNodeTypeDefException, RepositoryException {

        EffectiveNodeType ent;

        synchronized (this) {

            // validate and register new node type definition
            ent = internalRegister(ntd);

            // persist new node type definition
            customNTDefs.add(ntd);
            persistCustomNodeTypeDefs(customNTDefs);

            // notify listeners
            notifyRegistered(ntd.getName());

        }

        if (eventChannel != null) {
            Set<QNodeTypeDefinition> ntDefs = new HashSet<QNodeTypeDefinition>();
            ntDefs.add(ntd);
            eventChannel.registered(ntDefs);
        }

        return ent;
    }

    /**
     * Same as <code>{@link #registerNodeType(QNodeTypeDefinition)}</code> except
     * that a collection of <code>NodeTypeDef</code>s is registered instead of
     * just one.
     * <p>
     * This method can be used to register a set of node types that have
     * dependencies on each other.
     *
     * @param ntDefs a collection of <code>QNodeTypeDefinition</code> objects
     * @throws InvalidNodeTypeDefException if the given node type definition is invalid.
     * @throws RepositoryException if a repository error occurs.
     */
    public void registerNodeTypes(Collection<QNodeTypeDefinition> ntDefs)
            throws InvalidNodeTypeDefException, RepositoryException {

        registerNodeTypes(ntDefs, false);
    }

    /**
     * Internal implementation of {@link #registerNodeTypes(Collection)}
     *
     * @param ntDefs a collection of <code>QNodeTypeDefinition<code> objects
     * @param external whether this invocation should be considered external
     * @throws InvalidNodeTypeDefException if the given node type definition is invalid.
     * @throws RepositoryException if a repository error occurs.
     */
    private void registerNodeTypes(Collection<QNodeTypeDefinition> ntDefs,
                                                boolean external)
            throws InvalidNodeTypeDefException, RepositoryException {

        synchronized (this) {

            // validate and register new node type definitions
            internalRegister(ntDefs, external);
            // persist new node type definitions
            for (QNodeTypeDefinition ntDef: ntDefs) {
                customNTDefs.add(ntDef);
            }
            persistCustomNodeTypeDefs(customNTDefs);

            // notify listeners
            for (QNodeTypeDefinition ntDef : ntDefs) {
                notifyRegistered(ntDef.getName());
            }

        }

        // inform cluster if this is not an external invocation
        if (!external && eventChannel != null) {
            eventChannel.registered(ntDefs);
        }

    }

    /**
     * Same as <code>{@link #unregisterNodeType(Name)}</code> except
     * that a set of node types is unregistered instead of just one.
     * <p>
     * This method can be used to unregister a set of node types that depend on
     * each other.
     *
     * @param ntNames a collection of <code>Name</code> objects denoting the
     *                node types to be unregistered
     * @throws NoSuchNodeTypeException if any of the specified names does not
     *                                 denote a registered node type.
     * @throws RepositoryException if another error occurs
     * @see #unregisterNodeType(Name)
     */
    public void unregisterNodeTypes(Set<Name> ntNames)
            throws NoSuchNodeTypeException, RepositoryException {
        unregisterNodeTypes(ntNames, false);
    }

    /**
     * Internal implementation of {@link #unregisterNodeTypes(Set)}
     *
     * @param ntNames a collection of <code>Name</code> objects denoting the
     *                node types to be unregistered
     * @param external whether this invocation should be considered external
     * @throws NoSuchNodeTypeException if any of the specified names does not
     *                                 denote a registered node type.
     * @throws RepositoryException if another error occurs
     */
    private void unregisterNodeTypes(
            Collection<Name> ntNames, boolean external)
            throws NoSuchNodeTypeException, RepositoryException {

        synchronized (this) {

            // do some preliminary checks
            for (Name ntName: ntNames) {
                if (!registeredNTDefs.containsKey(ntName)) {
                    throw new NoSuchNodeTypeException(ntName.toString());
                }
                if (builtInNTDefs.contains(ntName)) {
                    throw new RepositoryException(ntName.toString()
                            + ": can't unregister built-in node type.");
                }
                // check for node types other than those to be unregistered
                // that depend on the given node types
                Set<Name> dependents = getDependentNodeTypes(ntName);
                dependents.removeAll(ntNames);
                if (dependents.size() > 0) {
                    StringBuilder msg = new StringBuilder();
                    msg.append(ntName).append(" can not be removed because the following node types depend on it: ");
                    for (Name dependent : dependents) {
                        msg.append(dependent);
                        msg.append(" ");
                    }
                    throw new RepositoryException(msg.toString());
                }
            }

            // make sure node types are not currently in use
            for (Name ntName : ntNames) {
                checkForReferencesInContent(ntName);
            }

            // all preconditions are met, node types can now safely be unregistered
            internalUnregister(ntNames);

            // persist removal of node type definitions & notify listeners
            for (Name ntName : ntNames) {
                customNTDefs.remove(ntName);
            }
            notifyUnregistered(ntNames);

            persistCustomNodeTypeDefs(customNTDefs);

        }

        // inform cluster if this is not an external invocation
        if (!external && eventChannel != null) {
            eventChannel.unregistered(ntNames);
        }

    }

    /**
     * Unregisters the specified node type. In order for a node type to be
     * successfully unregistered it must meet the following conditions:
     * <ol>
     * <li>the node type must obviously be registered.</li>
     * <li>a built-in node type can not be unregistered.</li>
     * <li>the node type must not have dependents, i.e. other node types that
     * are referencing it.</li>
     * <li>the node type must not be currently used by any workspace.</li>
     * </ol>
     *
     * @param ntName name of the node type to be unregistered
     * @throws NoSuchNodeTypeException if <code>ntName</code> does not
     *                                 denote a registered node type.
     * @throws RepositoryException if another error occurs.
     * @see #unregisterNodeTypes(Collection, boolean)
     */
    public void unregisterNodeType(Name ntName)
            throws NoSuchNodeTypeException, RepositoryException {
        HashSet<Name> ntNames = new HashSet<Name>();
        ntNames.add(ntName);
        unregisterNodeTypes(ntNames);
    }

    /**
     * Reregister a node type.
     * @param ntd node type definition
     * @return the new effective node type
     * @throws NoSuchNodeTypeException if <code>ntd</code> refers to an
     *                                 unknown node type
     * @throws InvalidNodeTypeDefException if the node type definition
     *                                     is invalid
     * @throws RepositoryException if another error occurs
     */
    public EffectiveNodeType reregisterNodeType(QNodeTypeDefinition ntd)
            throws NoSuchNodeTypeException, InvalidNodeTypeDefException,
            RepositoryException {

        return reregisterNodeType(ntd, false);
    }

    /**
     * Internal implementation of {@link #reregisterNodeType(QNodeTypeDefinition)}.
     *
     * @param ntd node type definition
     * @param external whether this invocation should be considered external
     * @return the new effective node type
     * @throws NoSuchNodeTypeException if <code>ntd</code> refers to an
     *                                 unknown node type
     * @throws InvalidNodeTypeDefException if the node type definition
     *                                     is invalid
     * @throws RepositoryException if another error occurs
     */
    private EffectiveNodeType reregisterNodeType(QNodeTypeDefinition ntd,
                                                              boolean external)
            throws NoSuchNodeTypeException, InvalidNodeTypeDefException,
            RepositoryException {

        EffectiveNodeType entNew;

        synchronized (this) {

            Name name = ntd.getName();
            if (!registeredNTDefs.containsKey(name)) {
                throw new NoSuchNodeTypeException(name.toString());
            }
            if (builtInNTDefs.contains(name)) {
                throw new RepositoryException(name.toString()
                        + ": can't reregister built-in node type.");
            }

            /**
             * validate new node type definition
             */
            ntd = checkNtBaseSubtyping(ntd, registeredNTDefs);
            validateNodeTypeDef(ntd, entCache, registeredNTDefs, nsReg, false);

            /**
             * build diff of current and new definition and determine type of change
             */
            QNodeTypeDefinition ntdOld = registeredNTDefs.get(name);
            NodeTypeDefDiff diff = NodeTypeDefDiff.create(ntdOld, ntd);
            if (!diff.isModified()) {
                // the definition has not been modified, there's nothing to do here...
                return getEffectiveNodeType(name);
            }

            // make sure existing content would not conflict
            // with new node type definition
            checkForConflictingContent(ntd, diff);

            /**
             * re-register node type definition and update caches &
             * notify listeners on re-registration
             */
            internalUnregister(name);
            // remove old node type definition from store
            customNTDefs.remove(name);

            entNew = internalRegister(ntd);

            // add new node type definition to store
            customNTDefs.add(ntd);
            // persist node type definitions
            persistCustomNodeTypeDefs(customNTDefs);

            // notify listeners
            notifyReRegistered(name);

        }

        // inform cluster if this is not an external invocation
        if (!external && eventChannel != null) {
            eventChannel.reregistered(ntd);
        }

        return entNew;

    }

    /**
     * @param ntName name
     * @return effective node type
     * @throws NoSuchNodeTypeException if node type does not exist
     */
    public EffectiveNodeType getEffectiveNodeType(Name ntName)
            throws NoSuchNodeTypeException {
        return getEffectiveNodeType(ntName, entCache, registeredNTDefs);
    }

    /**
     * Returns the effective node type of a node with the given primary
     * and mixin types.
     *
     * @param primary primary type of the node
     * @param mixins mixin types of the node (set of {@link Name names});
     * @return effective node type
     * @throws NodeTypeConflictException if the given types are conflicting
     * @throws NoSuchNodeTypeException if one of the given types is not found
     */
    public EffectiveNodeType getEffectiveNodeType(Name primary, Set<Name> mixins)
            throws NodeTypeConflictException, NoSuchNodeTypeException {
        if (mixins.isEmpty()) {
            return getEffectiveNodeType(primary);
        } else {
            Name[] names = new Name[mixins.size() + 1];
            mixins.toArray(names);
            names[names.length - 1] = primary;
            return getEffectiveNodeType(names, entCache, registeredNTDefs);
        }
    }

    /**
     * Returns the effective node type representation of the given node types.
     *
     * @param mixins mixin types of the node (set of {@link Name names});
     * @return effective node type
     * @throws NodeTypeConflictException if the given types are conflicting
     * @throws NoSuchNodeTypeException if one of the given types is not found
     */
    public EffectiveNodeType getEffectiveNodeType(Set<Name> mixins)
            throws NodeTypeConflictException, NoSuchNodeTypeException {
        Name[] names = new Name[mixins.size()];
        mixins.toArray(names);
        return getEffectiveNodeType(names, entCache, registeredNTDefs);
    }

    /**
     * Returns the names of those registered node types that have
     * dependencies on the given node type.
     *
     * @param nodeTypeName node type name
     * @return a set of node type <code>Name</code>s
     * @throws NoSuchNodeTypeException if node type does not exist
     */
    public Set<Name> getDependentNodeTypes(Name nodeTypeName)
            throws NoSuchNodeTypeException {
        if (!registeredNTDefs.containsKey(nodeTypeName)) {
            throw new NoSuchNodeTypeException(nodeTypeName.toString());
        }

        /**
         * collect names of those node types that have dependencies on the given
         * node type
         */
        HashSet<Name> names = new HashSet<Name>();
        for (QNodeTypeDefinition ntd : registeredNTDefs.values()) {
            if (ntd.getDependencies().contains(nodeTypeName)) {
                names.add(ntd.getName());
            }
        }
        return names;
    }

    /**
     * Returns the node type definition of the node type with the given name.
     *
     * @param nodeTypeName name of node type whose definition should be returned.
     * @return the node type definition of the node type with the given name.
     * @throws NoSuchNodeTypeException if a node type with the given name
     *                                 does not exist
     */
    public QNodeTypeDefinition getNodeTypeDef(Name nodeTypeName)
            throws NoSuchNodeTypeException {
        QNodeTypeDefinition def = registeredNTDefs.get(nodeTypeName);
        if (def == null) {
            throw new NoSuchNodeTypeException(nodeTypeName.toString());
        }
        return def;
    }

    /**
     * @param nodeTypeName node type name
     * @return <code>true</code> if the specified node type is registered;
     *         <code>false</code> otherwise.
     */
    public boolean isRegistered(Name nodeTypeName) {
        return registeredNTDefs.containsKey(nodeTypeName);
    }

    /**
     * @param nodeTypeName node type name
     * @return <code>true</code> if the specified node type is built-in;
     *         <code>false</code> otherwise.
     */
    public boolean isBuiltIn(Name nodeTypeName) {
        return builtInNTDefs.contains(nodeTypeName);
    }

    /**
     * Add a <code>NodeTypeRegistryListener</code>
     *
     * @param listener the new listener to be informed on (un)registration
     *                 of node types
     */
    public void addListener(NodeTypeRegistryListener listener) {
        if (!listeners.containsKey(listener)) {
            listeners.put(listener, listener);
        }
    }

    /**
     * Remove a <code>NodeTypeRegistryListener</code>
     *
     * @param listener an existing listener
     */
    public void removeListener(NodeTypeRegistryListener listener) {
        listeners.remove(listener);
    }

    //--------------------------------------------------------------< Object >

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NodeTypeRegistry (" + super.toString() + ")\n");
        builder.append("Registered NodeTypes:\n");
        for (QNodeTypeDefinition ntd : registeredNTDefs.values()) {
            builder.append(ntd.getName());
            builder.append("\n");
            builder.append(
                    "\tSupertypes: "
                    + Arrays.toString(ntd.getSupertypes()) + "\n");
            builder.append("\tMixin\t" + ntd.isMixin() + "\n");
            builder.append("\tOrderableChildNodes\t" + ntd.hasOrderableChildNodes() + "\n");
            builder.append("\tPrimaryItemName\t" + (ntd.getPrimaryItemName() == null ? "<null>" : ntd.getPrimaryItemName().toString()) + "\n");
            QPropertyDefinition[] pd = ntd.getPropertyDefs();
            for (QPropertyDefinition aPd : pd) {
                builder.append("\tPropertyDefinition\n");
                builder.append(" (declared in " + aPd.getDeclaringNodeType() + ")\n");
                builder.append("\t\tName\t\t" + (aPd.definesResidual() ? "*" : aPd.getName().toString()) + "\n");
                String type = aPd.getRequiredType() == 0 ? "null" : PropertyType.nameFromValue(aPd.getRequiredType());
                builder.append("\t\tRequiredType\t" + type + "\n");
                QValueConstraint[] vca = aPd.getValueConstraints();
                StringBuilder constraints = new StringBuilder();
                if (vca == null) {
                    constraints.append("<null>");
                } else {
                    for (QValueConstraint aVca : vca) {
                        if (constraints.length() > 0) {
                            constraints.append(", ");
                        }
                        constraints.append(aVca.getString());
                    }
                }
                builder.append("\t\tValueConstraints\t" + constraints + "\n");
                QValue[] defVals = aPd.getDefaultValues();
                StringBuilder defaultValues = new StringBuilder();
                if (defVals == null) {
                    defaultValues.append("<null>");
                } else {
                    for (QValue defVal : defVals) {
                        if (defaultValues.length() > 0) {
                            defaultValues.append(", ");
                        }
                        defaultValues.append(defVal.toString());
                    }
                }
                builder.append("\t\tDefaultValue\t" + defaultValues + "\n");
                builder.append("\t\tAutoCreated\t" + aPd.isAutoCreated() + "\n");
                builder.append("\t\tMandatory\t" + aPd.isMandatory() + "\n");
                builder.append("\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(aPd.getOnParentVersion()) + "\n");
                builder.append("\t\tProtected\t" + aPd.isProtected() + "\n");
                builder.append("\t\tMultiple\t" + aPd.isMultiple() + "\n");
            }
            QNodeDefinition[] nd = ntd.getChildNodeDefs();
            for (QNodeDefinition aNd : nd) {
                builder.append("\tNodeDefinition\\n");
                builder.append(" (declared in " + aNd.getDeclaringNodeType() + ")\\n");
                builder.append("\t\tName\t\t" + (aNd.definesResidual() ? "*" : aNd.getName().toString()) + "\n");
                Name[] reqPrimaryTypes = aNd.getRequiredPrimaryTypes();
                if (reqPrimaryTypes != null && reqPrimaryTypes.length > 0) {
                    for (Name reqPrimaryType : reqPrimaryTypes) {
                        builder.append("\t\tRequiredPrimaryType\t" + reqPrimaryType + "\n");
                    }
                }
                Name defPrimaryType = aNd.getDefaultPrimaryType();
                if (defPrimaryType != null) {
                    builder.append("\n\t\tDefaultPrimaryType\t" + defPrimaryType + "\n");
                }
                builder.append("\n\t\tAutoCreated\t" + aNd.isAutoCreated() + "\n");
                builder.append("\t\tMandatory\t" + aNd.isMandatory() + "\n");
                builder.append("\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(aNd.getOnParentVersion()) + "\n");
                builder.append("\t\tProtected\t" + aNd.isProtected() + "\n");
                builder.append("\t\tAllowsSameNameSiblings\t" + aNd.allowsSameNameSiblings() + "\n");
            }
        }
        builder.append(entCache);
        return builder.toString();
    }

    //------------------------------------------------< NodeTypeEventListener >

    /**
     * {@inheritDoc}
     */
    public void externalRegistered(Collection<QNodeTypeDefinition> ntDefs)
            throws RepositoryException, InvalidNodeTypeDefException {

        registerNodeTypes(ntDefs, true);
    }

    /**
     * {@inheritDoc}
     */
    public void externalReregistered(QNodeTypeDefinition ntDef)
            throws NoSuchNodeTypeException, InvalidNodeTypeDefException,
            RepositoryException {

        reregisterNodeType(ntDef, true);
    }

    /**
     * {@inheritDoc}
     */
    public void externalUnregistered(Collection<Name> ntNames)
            throws RepositoryException, NoSuchNodeTypeException {
        unregisterNodeTypes(ntNames, true);
    }

    //---------------------------------------------------------< overridables >
    /**
     * Constructor
     *
     * @param nsReg name space registry
     * @param fs repository file system
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public NodeTypeRegistry(NamespaceRegistry nsReg, FileSystem fs)
            throws RepositoryException {
        this.nsReg = nsReg;
        customNodeTypesResource =
            new FileSystemResource(fs, CUSTOM_NODETYPES_RESOURCE_NAME);
        try {
            // make sure path to resource exists
            if (!customNodeTypesResource.exists()) {
                customNodeTypesResource.makeParentDirs();
            }
        } catch (FileSystemException fse) {
            String error = "internal error: invalid resource: "
                    + customNodeTypesResource.getPath();
            log.debug(error);
            throw new RepositoryException(error, fse);
        }

        // use the improved node type cache
        // (replace with: entCache = new EffectiveNodeTypeCacheImpl();
        // for the old one)
        entCache = new BitSetENTCacheImpl();
        registeredNTDefs = new ConcurrentReaderHashMap();

        // setup definition of root node
        rootNodeDef = createRootNodeDef();

        // load and register pre-defined (i.e. built-in) node types
        builtInNTDefs = new NodeTypeDefStore();
        try {
            // load built-in node type definitions
            loadBuiltInNodeTypeDefs(builtInNTDefs);

            // register built-in node types
            internalRegister(builtInNTDefs.all(), false, true);
        } catch (InvalidNodeTypeDefException intde) {
            String error =
                    "internal error: invalid built-in node type definition stored in "
                    + BUILTIN_NODETYPES_RESOURCE_PATH;
            log.debug(error);
            throw new RepositoryException(error, intde);
        }

        // load and register custom node types
        customNTDefs = new NodeTypeDefStore();

        // load custom node type definitions
        loadCustomNodeTypeDefs(customNTDefs);

        // validate & register custom node types
        try {
            internalRegister(customNTDefs.all(), false);
        } catch (InvalidNodeTypeDefException intde) {
            String error =
                    "internal error: invalid custom node type definition stored in "
                    + customNodeTypesResource.getPath();
            log.debug(error);
            throw new RepositoryException(error, intde);
        }
    }

    /**
     * Loads the built-in node type definitions into the given <code>store</code>.
     * <p>
     * This method may be overridden by extensions of this class; It must
     * only be called once and only from within the constructor though.
     *
     * @param store The {@link NodeTypeDefStore} into which the node type
     *              definitions are loaded.
     * @throws RepositoryException If an error occurs while loading the
     *                             built-in node type definitions.
     */
    protected void loadBuiltInNodeTypeDefs(NodeTypeDefStore store)
            throws RepositoryException {
        InputStream in = null;
        try {
            in = getClass().getClassLoader().getResourceAsStream(BUILTIN_NODETYPES_RESOURCE_PATH);
            if (in != null) {
                Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);
                store.loadCND(r, BUILTIN_NODETYPES_RESOURCE_PATH);
            }
        } catch (IOException ioe) {
            String error =
                    "internal error: failed to read built-in node type definitions stored in "
                    + BUILTIN_NODETYPES_RESOURCE_PATH;
            log.debug(error);
            throw new RepositoryException(error, ioe);
        } catch (InvalidNodeTypeDefException intde) {
            String error =
                    "internal error: invalid built-in node type definition stored in "
                    + BUILTIN_NODETYPES_RESOURCE_PATH;
            log.debug(error);
            throw new RepositoryException(error, intde);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Loads the custom node type definitions into the given <code>store</code>.
     * <p>
     * This method may be overridden by extensions of this class; It must
     * only be called once and only from within the constructor though.
     *
     * @param store The {@link NodeTypeDefStore} into which the node type
     *              definitions are loaded.
     * @throws RepositoryException If an error occurs while loading the
     *                             custom node type definitions.
     */
    protected void loadCustomNodeTypeDefs(NodeTypeDefStore store)
            throws RepositoryException {

        InputStream in = null;
        try {
            if (customNodeTypesResource.exists()) {
                in = customNodeTypesResource.getInputStream();
            }
        } catch (FileSystemException fse) {
            String error =
                    "internal error: failed to access custom node type definitions stored in "
                    + customNodeTypesResource.getPath();
            log.debug(error);
            throw new RepositoryException(error, fse);
        }

        if (in == null) {
            log.info("no custom node type definitions found");
        } else {
            try {
                store.load(in);
            } catch (IOException ioe) {
                String error =
                        "internal error: failed to read custom node type definitions stored in "
                        + customNodeTypesResource.getPath();
                log.debug(error);
                throw new RepositoryException(error, ioe);
            } catch (InvalidNodeTypeDefException intde) {
                String error =
                        "internal error: invalid custom node type definition stored in "
                        + customNodeTypesResource.getPath();
                log.debug(error);
                throw new RepositoryException(error, intde);
            } finally {
                try {
                    in.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    /**
     * Persists the custom node type definitions contained in the given
     * <code>store</code>.
     *
     * @param store The {@link NodeTypeDefStore} containing the definitions to
     *              be persisted.
     * @throws RepositoryException If an error occurs while persisting the
     *                             custom node type definitions.
     */
    protected void persistCustomNodeTypeDefs(NodeTypeDefStore store)
            throws RepositoryException {
        try {
            OutputStream out = customNodeTypesResource.getOutputStream();
            try {
                store.store(out, nsReg);
            } finally {
                out.close();
            }
        } catch (IOException ioe) {
            String error =
                    "internal error: failed to persist custom node type definitions to "
                    + customNodeTypesResource.getPath();
            log.debug(error);
            throw new RepositoryException(error, ioe);
        } catch (FileSystemException fse) {
            String error =
                    "internal error: failed to persist custom node type definitions to "
                    + customNodeTypesResource.getPath();
            log.debug(error);
            throw new RepositoryException(error, fse);
        }
    }

    /**
     * Checks whether there is existing content that would conflict with the
     * given node type definition.
     * <p>
     * This method is not implemented yet and always throws a
     * <code>RepositoryException</code>.
     * <p>
     * TODO
     * <ol>
     * <li>apply deep locks on root nodes in every workspace or alternatively
     * put repository in 'exclusive' or 'single-user' mode
     * <li>check if the given node type (or any node type that has
     * dependencies on this node type) is currently referenced by nodes
     * in the repository.
     * <li>check if applying the changed definitions to the affected items would
     * violate existing node type constraints
     * <li>apply and persist changes to affected nodes (e.g. update
     * definition id's, etc.)
     * </ol>
     * <p>
     * the above checks/actions are absolutely necessary in order to
     * guarantee integrity of repository content.
     *
     *
     * @param ntd The node type definition replacing the former node type
     *            definition of the same name.
     * @param diff
     * @throws RepositoryException If there is conflicting content or if the
     *                             check failed for some other reason.
     */
    protected void checkForConflictingContent(QNodeTypeDefinition ntd, final NodeTypeDefDiff diff)
            throws RepositoryException {

        if (!diff.isTrivial()) {
            /**
             * collect names of node types that have dependencies on the given
             * node type
             */
            //Set dependentNTs = getDependentNodeTypes(ntd.getName());

            String message =
                    "The following node type change contains non-trivial changes."
                            + "Up until now only trivial changes are supported."
                            + " (see javadoc for "
                            + NodeTypeDefDiff.class.getName()
                            + "):\n" + diff.toString();
            throw new RepositoryException(message);
        }

        /**
         * the change is trivial and has no effect on current content
         * (e.g. that would be the case when non-mandatory properties had
         * been added);
         */
    }

    /**
     * Checks whether there is existing content that directly or indirectly
     * refers to the specified node type.
     * <p>
     * This method is not implemented yet and always throws a
     * <code>RepositoryException</code>.
     * <p>
     * TODO:
     * <ol>
     * <li>apply deep locks on root nodes in every workspace or alternatively
     * put repository in 'single-user' mode
     * <li>check if the given node type is currently referenced by nodes
     * in the repository.
     * <li>remove the node type if it is not currently referenced, otherwise
     * throw exception
     * </ol>
     * <p>
     * the above checks are absolutely necessary in order to guarantee
     * integrity of repository content.
     *
     * @param nodeTypeName The name of the node type to be checked.
     * @throws RepositoryException If the specified node type is currently
     *                             being referenced or if the check failed for
     *                             some other reason.
     */
    protected void checkForReferencesInContent(Name nodeTypeName)
            throws RepositoryException {
        if (!disableCheckForReferencesInContentException) {
            throw new RepositoryException(
                    "The check for the existence of content using the"
                    + " given node type is not yet implemented, so to"
                    + " guarantee repository consistency the request to"
                    + " unregister the type is denied. Contributions to"
                    + " implement this feature would be welcome! To restore"
                    + " the broken behavior of previous Jackrabbit versions"
                    + " where this check was simply skipped, please set the"
                    + " disableCheckForReferencesInContentException system"
                    + " property to true.");
        }
    }

    //-------------------------------------------------------< implementation >
    /**
     * @return the definition of the root node
     */
    public QNodeDefinition getRootNodeDef() {
        return rootNodeDef;
    }

    /**
     * Set an event channel to inform about changes.
     *
     * @param eventChannel event channel
     */
    public void setEventChannel(NodeTypeEventChannel eventChannel) {
        this.eventChannel = eventChannel;
        eventChannel.setListener(this);
    }

    /**
     * @param ntName node type name
     * @param entCache cache of already-built effective node types
     * @param ntdCache cache of node type definitions
     * @return the effective node type
     * @throws NoSuchNodeTypeException if a node type reference (e.g. a supertype)
     *                                 could not be resolved.
     */
    static EffectiveNodeType getEffectiveNodeType(Name ntName,
                                                  EffectiveNodeTypeCache entCache,
                                                  Map<Name, QNodeTypeDefinition> ntdCache)
            throws NoSuchNodeTypeException {
        // 1. check if effective node type has already been built
        EffectiveNodeTypeCache.Key key = entCache.getKey(new Name[]{ntName});
        EffectiveNodeType ent = entCache.get(key);
        if (ent != null) {
            return ent;
        }

        // 2. make sure we've got the definition of the specified node type
        QNodeTypeDefinition ntd = ntdCache.get(ntName);
        if (ntd == null) {
            throw new NoSuchNodeTypeException(ntName.toString());
        }

        // 3. build effective node type
        synchronized (entCache) {
            try {
                ent = EffectiveNodeType.create(ntd, entCache, ntdCache);
                // store new effective node type
                entCache.put(ent);
                return ent;
            } catch (NodeTypeConflictException ntce) {
                // should never get here as all known node types should be valid!
                String msg = "internal error: encountered invalid registered node type " + ntName;
                log.debug(msg);
                throw new NoSuchNodeTypeException(msg, ntce);
            }
        }
    }

    /**
     * Returns an effective node type representation of the given node types.
     *
     * @param ntNames  array of node type names
     * @param entCache cache of already-built effective node types
     * @param ntdCache cache of node type definitions
     * @return the desired effective node type
     * @throws NodeTypeConflictException if the effective node type representation
     *                                   could not be built due to conflicting
     *                                   node type definitions.
     * @throws NoSuchNodeTypeException if a node type reference (e.g. a supertype)
     *                                 could not be resolved.
     */
    static EffectiveNodeType getEffectiveNodeType(Name[] ntNames,
                                                  EffectiveNodeTypeCache entCache,
                                                  Map<Name, QNodeTypeDefinition> ntdCache)
            throws NodeTypeConflictException, NoSuchNodeTypeException {

        EffectiveNodeTypeCache.Key key = entCache.getKey(ntNames);

        // 1. check if aggregate has already been built
        if (entCache.contains(key)) {
            return entCache.get(key);
        }

        // 2. make sure we've got the definitions of the specified node types
        for (Name ntName : ntNames) {
            if (!ntdCache.containsKey(ntName)) {
                throw new NoSuchNodeTypeException(ntName.toString());
            }
        }

        // 3. build aggregate
        EffectiveNodeTypeCache.Key requested = key;
        EffectiveNodeType result = null;
        synchronized (entCache) {
            // build list of 'best' existing sub-aggregates
            while (key.getNames().length > 0) {
                // find the (sub) key that matches the current key the best
                EffectiveNodeTypeCache.Key subKey = entCache.findBest(key);
                if (subKey != null) {
                    EffectiveNodeType ent = entCache.get(subKey);
                    if (result == null) {
                        result = ent;
                    } else {
                        result = result.merge(ent);
                        // store intermediate result
                        entCache.put(result);
                    }
                    // subtract the result from the temporary key
                    key = key.subtract(subKey);
                } else {
                    /**
                     * no matching sub-aggregates found:
                     * build aggregate of remaining node types through iteration
                     */
                    Name[] remainder = key.getNames();
                    for (Name aRemainder : remainder) {
                        QNodeTypeDefinition ntd = ntdCache.get(aRemainder);
                        EffectiveNodeType ent =
                                EffectiveNodeType.create(ntd, entCache, ntdCache);
                        // store new effective node type
                        entCache.put(ent);
                        if (result == null) {
                            result = ent;
                        } else {
                            result = result.merge(ent);
                            // store intermediate result (sub-aggregate)
                            entCache.put(result);
                        }
                    }
                    break;
                }
            }
        }
        // also put the requested key, since the merge could have removed some
        // the redundant nodetypes
        if (!entCache.contains(requested)) {
            entCache.put(requested, result);
        }
        // we're done
        return result;
    }

    static void checkForCircularInheritance(Name[] supertypes,
                                            Stack<Name> inheritanceChain,
                                            Map<Name, QNodeTypeDefinition> ntDefCache)
            throws InvalidNodeTypeDefException, RepositoryException {
        for (Name nt : supertypes) {
            int pos = inheritanceChain.lastIndexOf(nt);
            if (pos >= 0) {
                StringBuilder buf = new StringBuilder();
                for (int j = 0; j < inheritanceChain.size(); j++) {
                    if (j == pos) {
                        buf.append("--> ");
                    }
                    buf.append(inheritanceChain.get(j));
                    buf.append(" extends ");
                }
                buf.append("--> ");
                buf.append(nt);
                throw new InvalidNodeTypeDefException("circular inheritance detected: " + buf.toString());
            }

            try {
                QNodeTypeDefinition ntd = ntDefCache.get(nt);
                Name[] sta = ntd.getSupertypes();
                if (sta.length > 0) {
                    // check recursively
                    inheritanceChain.push(nt);
                    checkForCircularInheritance(sta, inheritanceChain, ntDefCache);
                    inheritanceChain.pop();
                }
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = "unknown supertype: " + nt;
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        }
    }

    static void checkForCircularNodeAutoCreation(EffectiveNodeType childNodeENT,
                                                 Stack<Name> definingParentNTs,
                                                 EffectiveNodeTypeCache anEntCache,
                                                 Map<Name, QNodeTypeDefinition> ntDefCache)
            throws InvalidNodeTypeDefException {
        // check for circularity through default node types of auto-created child nodes
        // (node type 'a' defines auto-created child node with default node type 'a')
        Name[] childNodeNTs = childNodeENT.getAllNodeTypes();
        for (Name nt : childNodeNTs) {
            int pos = definingParentNTs.lastIndexOf(nt);
            if (pos >= 0) {
                StringBuilder buf = new StringBuilder();
                for (int j = 0; j < definingParentNTs.size(); j++) {
                    if (j == pos) {
                        buf.append("--> ");
                    }
                    buf.append("node type ");
                    buf.append(definingParentNTs.get(j));
                    buf.append(" defines auto-created child node with default ");
                }
                buf.append("--> ");
                buf.append("node type ");
                buf.append(nt);
                throw new InvalidNodeTypeDefException("circular node auto-creation detected: "
                        + buf.toString());
            }
        }

        QNodeDefinition[] nodeDefs = childNodeENT.getAutoCreateNodeDefs();
        for (QNodeDefinition nodeDef : nodeDefs) {
            Name dnt = nodeDef.getDefaultPrimaryType();
            Name definingNT = nodeDef.getDeclaringNodeType();
            try {
                if (dnt != null) {
                    // check recursively
                    definingParentNTs.push(definingNT);
                    checkForCircularNodeAutoCreation(getEffectiveNodeType(dnt, anEntCache, ntDefCache),
                            definingParentNTs, anEntCache, ntDefCache);
                    definingParentNTs.pop();
                }
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = definingNT
                        + " defines invalid default node type for child node " + nodeDef.getName();
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        }
    }

    private EffectiveNodeType internalRegister(QNodeTypeDefinition ntd)
            throws InvalidNodeTypeDefException, RepositoryException {
        Name name = ntd.getName();
        if (name != null && registeredNTDefs.containsKey(name)) {
            String msg = name + " already exists";
            log.debug(msg);
            throw new InvalidNodeTypeDefException(msg);
        }

        ntd = checkNtBaseSubtyping(ntd, registeredNTDefs);
        EffectiveNodeType ent =
                validateNodeTypeDef(ntd, entCache, registeredNTDefs, nsReg, false);

        // store new effective node type instance
        entCache.put(ent);

        registeredNTDefs.put(name, ntd);

        return ent;
    }

    /**
     * Validates and registers the specified collection of <code>NodeTypeDef</code>
     * objects. An <code>InvalidNodeTypeDefException</code> is thrown if the
     * validation of any of the contained <code>NodeTypeDef</code> objects fails.
     * <p>
     * Note that in the case an exception is thrown no node type will be
     * eventually registered.
     *
     * @param ntDefs collection of <code>NodeTypeDef</code> objects
     * @throws InvalidNodeTypeDefException if the node type is not valid
     * @throws RepositoryException if an error occurs
     * @see #registerNodeType
     */
    private void internalRegister(Collection<QNodeTypeDefinition> ntDefs, boolean external)
            throws InvalidNodeTypeDefException, RepositoryException {
        internalRegister(ntDefs, external, false);
    }

    /**
     * Same as {@link #internalRegister(java.util.Collection, boolean)} except for the
     * additional <code>lenient</code> parameter which governs whether
     * validation can be lenient (e.g. for built-in node types) or has to be
     * strict (such as in the case of custom node types). This differentiation
     * is unfortunately required as there are e.g. properties defined in built-in
     * node types which are auto-created but don't have a fixed default value
     * that can be exposed in a property definition because it is
     * system-generated (such as jcr:primaryType in nt:base).
     */
    private void internalRegister(Collection<QNodeTypeDefinition> ntDefs, boolean external, boolean lenient)
            throws InvalidNodeTypeDefException, RepositoryException {

        // need a list/collection that can be modified
        List<QNodeTypeDefinition> defs = new ArrayList<QNodeTypeDefinition>(ntDefs);

        // map of node type names and node type definitions
        Map<Name, QNodeTypeDefinition> tmpNTDefCache = new HashMap<Name, QNodeTypeDefinition>(registeredNTDefs);

        // temporarily register the node type definition
        // and do some preliminary checks
        for (QNodeTypeDefinition ntd : defs) {
            Name name = ntd.getName();
            if (!external && name != null && tmpNTDefCache.containsKey(name)) {
                String msg = name + " already exists locally";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            // add definition to temporary cache
            tmpNTDefCache.put(ntd.getName(), ntd);
        }

        // check if all node type defs have proper nt:base subtyping
        for (int i = 0; i < defs.size(); i++) {
            QNodeTypeDefinition ntd = defs.get(i);
            QNodeTypeDefinition mod = checkNtBaseSubtyping(ntd, tmpNTDefCache);
            if (mod != ntd) {
                // check fixed subtyping
                // -> update cache and list of defs
                tmpNTDefCache.put(mod.getName(), mod);
                defs.set(i, mod);
            }
        }

        // create working copies of current ent & ntd caches:
        // cache of pre-built aggregations of node types
        EffectiveNodeTypeCache tmpENTCache = (EffectiveNodeTypeCache) entCache.clone();
        for (QNodeTypeDefinition ntd : defs) {
            EffectiveNodeType ent = validateNodeTypeDef(ntd, tmpENTCache,
                    tmpNTDefCache, nsReg, lenient);

            // store new effective node type instance
            tmpENTCache.put(ent);
        }

        // since no exception was thrown so far the definitions are assumed to
        // be valid
        for (QNodeTypeDefinition ntd : defs) {
            registeredNTDefs.put(ntd.getName(), ntd);
        }

        // finally add newly created effective node types to entCache
        entCache = tmpENTCache;
    }

    private void internalUnregister(Name name) throws NoSuchNodeTypeException {
        QNodeTypeDefinition ntd = registeredNTDefs.get(name);
        if (ntd == null) {
            throw new NoSuchNodeTypeException(name.toString());
        }
        registeredNTDefs.remove(name);
        entCache.invalidate(name);
    }

    private void internalUnregister(Collection<Name> ntNames)
            throws NoSuchNodeTypeException {
        for (Name name : ntNames) {
            internalUnregister(name);
        }
    }

    /**
     * Utility method for verifying that the namespace of a <code>Name</code>
     * is registered; a <code>null</code> argument is silently ignored.
     *
     * @param name name whose namespace is to be checked
     * @param nsReg namespace registry to be used for checking
     * @throws RepositoryException if the namespace of the given name is not
     *                             registered or if an unspecified error occured
     */
    private static void checkNamespace(Name name, NamespaceRegistry nsReg)
            throws RepositoryException {
        if (name != null) {
            // make sure namespace uri denotes a registered namespace
            nsReg.getPrefix(name.getNamespaceURI());
        }
    }

    /**
     * Checks if the given node type def has the correct supertypes in respect
     * to nt:base. all mixin nodetypes must not have a nt:base, the primary
     * ones only if they don't inherit it from another supertype.
     *
     * @param ntd the node type def to check
     * @param ntdCache cache for lookup
     * @return the node type definition that was given to check or a new
     *          instance if it had to be fixed up.
     */
    private static QNodeTypeDefinition checkNtBaseSubtyping(QNodeTypeDefinition ntd, Map<Name, QNodeTypeDefinition> ntdCache) {
        if (NameConstants.NT_BASE.equals(ntd.getName())) {
            return ntd;
        }
        Set<Name> supertypes = new TreeSet<Name>(Arrays.asList(ntd.getSupertypes()));
        if (supertypes.isEmpty()) {
            return ntd;
        }
        boolean modified;
        if (ntd.isMixin()) {
            // if mixin, remove possible nt:base supertype
            modified = supertypes.remove(NameConstants.NT_BASE);
        } else {
            // check if all supertypes (except nt:base) are mixins
            boolean allMixins = true;
            for (Name name: supertypes) {
                if (!name.equals(NameConstants.NT_BASE)) {
                    QNodeTypeDefinition def = ntdCache.get(name);
                    if (def != null && !def.isMixin()) {
                        allMixins = false;
                        break;
                    }
                }
            }
            if (allMixins) {
                // ntd is a primary node type and has only mixins as supertypes,
                // so it needs a nt:base
                modified = supertypes.add(NameConstants.NT_BASE);
            } else {
                // ntd is a primary node type and at least one of the supertypes
                // is too, so ensure that no nt:base is added. note that the
                // trivial case, where there would be no supertype left is handled
                // in the QNodeTypeDefinition directly
                modified = supertypes.remove(NameConstants.NT_BASE);
            }
        }
        if (modified) {
            ntd = new QNodeTypeDefinitionImpl(ntd.getName(),
                    supertypes.toArray(new Name[supertypes.size()]),
                    ntd.getSupportedMixinTypes(), ntd.isMixin(),
                    ntd.isAbstract(), ntd.isQueryable(),
                    ntd.hasOrderableChildNodes(), ntd.getPrimaryItemName(),
                    ntd.getPropertyDefs(), ntd.getChildNodeDefs());
        }
        return ntd;
    }

    /**
     * Validates the specified <code>NodeTypeDef</code> within the context of
     * the two other given collections and returns an <code>EffectiveNodeType</code>.
     *
     * @param ntd node type definition
     * @param entCache effective node type cache
     * @param ntdCache cache of 'known' node type definitions, used to resolve dependencies
     * @param nsReg    namespace registry used for validatingatch names
     * @param lenient flag governing whether validation can be lenient or has to be strict
     * @return an effective node type representation of the specified <code>QNodeTypeDefinition</code>
     * @throws InvalidNodeTypeDefException if the node type is not valid
     * @throws RepositoryException         if another error occurs
     */
    private static EffectiveNodeType validateNodeTypeDef(QNodeTypeDefinition ntd,
                                                         EffectiveNodeTypeCache entCache,
                                                         Map<Name, QNodeTypeDefinition> ntdCache,
                                                         NamespaceRegistry nsReg,
                                                         boolean lenient)
            throws InvalidNodeTypeDefException, RepositoryException {

        /**
         * the effective (i.e. merged and resolved) node type resulting from
         * the specified node type definition;
         * the effective node type will finally be created after the definition
         * has been verified and checked for conflicts etc.; in some cases it
         * will be created already at an earlier stage during the validation
         * of child node definitions
         */
        EffectiveNodeType ent = null;

        Name name = ntd.getName();
        if (name == null) {
            String msg = "no name specified";
            log.debug(msg);
            throw new InvalidNodeTypeDefException(msg);
        }
        checkNamespace(name, nsReg);

        // validate supertypes
        Name[] supertypes = ntd.getSupertypes();
        if (supertypes.length > 0) {
            for (Name supertype : supertypes) {
                checkNamespace(supertype, nsReg);
                /**
                 * simple check for infinite recursion
                 * (won't trap recursion on a deeper inheritance level)
                 */
                if (name.equals(supertype)) {
                    String msg = "[" + name + "] invalid supertype: "
                            + supertype + " (infinite recursion))";
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg);
                }
                if (!ntdCache.containsKey(supertype)) {
                    String msg = "[" + name + "] invalid supertype: "
                            + supertype;
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg);
                }
            }

            /**
             * check for circularity in inheritance chain
             * ('a' extends 'b' extends 'a')
             */
            Stack<Name> inheritanceChain = new Stack<Name>();
            inheritanceChain.push(name);
            checkForCircularInheritance(supertypes, inheritanceChain, ntdCache);
        }

        /**
         * note that infinite recursion through inheritance is automatically
         * being checked by the following call to getEffectiveNodeType(...)
         * as it's impossible to register a node type definition which
         * references a supertype that isn't registered yet...
         */

        /**
         * build effective (i.e. merged and resolved) node type from supertypes
         * and check for conflicts
         */
        if (supertypes.length > 0) {
            try {
                EffectiveNodeType est = getEffectiveNodeType(supertypes, entCache, ntdCache);
                // check whether specified node type definition overrides
                // a supertypes's primaryItem -> illegal (JCR-1947)
                if (ntd.getPrimaryItemName() != null
                        && est.getPrimaryItemName() != null) {
                    String msg = "[" + name + "] primaryItemName is already specified by a supertype and must therefore not be overridden.";
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg);

                }
            } catch (NodeTypeConflictException ntce) {
                String msg = "[" + name + "] failed to validate supertypes";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, ntce);
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = "[" + name + "] failed to validate supertypes";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        }

        checkNamespace(ntd.getPrimaryItemName(), nsReg);

        // validate property definitions
        QPropertyDefinition[] pda = ntd.getPropertyDefs();
        for (QPropertyDefinition pd : pda) {
            /**
             * sanity check:
             * make sure declaring node type matches name of node type definition
             */
            if (!name.equals(pd.getDeclaringNodeType())) {
                String msg = "[" + name + "#" + pd.getName()
                        + "] invalid declaring node type specified";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            checkNamespace(pd.getName(), nsReg);
            // check that auto-created properties specify a name
            if (pd.definesResidual() && pd.isAutoCreated()) {
                String msg = "[" + name + "#" + pd.getName()
                        + "] auto-created properties must specify a name";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            // check that auto-created properties specify a type
            if (pd.getRequiredType() == PropertyType.UNDEFINED
                    && pd.isAutoCreated()) {
                String msg = "[" + name + "#" + pd.getName()
                        + "] auto-created properties must specify a type";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            /**
             * check default values:
             * make sure type of value is consistent with required property type
             */
            QValue[] defVals = pd.getDefaultValues();
            if (defVals != null && defVals.length != 0) {
                int reqType = pd.getRequiredType();
                for (QValue defVal : defVals) {
                    if (reqType == PropertyType.UNDEFINED) {
                        reqType = defVal.getType();
                    } else {
                        if (defVal.getType() != reqType) {
                            String msg = "[" + name + "#" + pd.getName()
                                    + "] type of default value(s) is not consistent with required property type";
                            log.debug(msg);
                            throw new InvalidNodeTypeDefException(msg);
                        }
                    }
                }
            } else {
                // no default values specified
                if (!lenient) {
                    // auto-created properties must have a default value
                    if (pd.isAutoCreated()) {
                        String msg = "[" + name + "#" + pd.getName()
                                + "] auto-created property must have a default value";
                        log.debug(msg);
                        throw new InvalidNodeTypeDefException(msg);
                    }
                }
            }

            // check that default values satisfy value constraints
            QValueConstraint[] constraints = pd.getValueConstraints();
            if (constraints != null && constraints.length > 0) {
                if (defVals != null && defVals.length > 0) {
                    // check value constraints on every value
                    for (QValue defVal : defVals) {
                        // constraints are OR-ed together
                        boolean satisfied = false;
                        ConstraintViolationException cve = null;
                        for (QValueConstraint constraint : constraints) {
                            try {
                                constraint.check(defVal);
                                // at least one constraint is satisfied
                                satisfied = true;
                                break;
                            } catch (ConstraintViolationException e) {
                                cve = e;
                            }
                        }
                        if (!satisfied) {
                            // report last exception we encountered
                            String msg = "[" + name + "#" + pd.getName()
                                    + "] default value does not satisfy value constraint";
                            log.debug(msg);
                            throw new InvalidNodeTypeDefException(msg, cve);
                        }
                    }
                }

                /**
                 * ReferenceConstraint:
                 * the specified node type must be registered, with one notable
                 * exception: the node type just being registered
                 */
                if (pd.getRequiredType() == PropertyType.REFERENCE
                        || pd.getRequiredType() == PropertyType.WEAKREFERENCE) {
                    for (QValueConstraint constraint : constraints) {
                        Name ntName = NameFactoryImpl.getInstance().create(constraint.getString());
                        if (!name.equals(ntName) && !ntdCache.containsKey(ntName)) {
                            String msg = "[" + name + "#" + pd.getName()
                                    + "] invalid "
                                    + (pd.getRequiredType() == PropertyType.REFERENCE ? "REFERENCE" : "WEAKREFERENCE")
                                    + " value constraint '"
                                    + ntName + "' (unknown node type)";
                            log.debug(msg);
                            throw new InvalidNodeTypeDefException(msg);
                        }
                    }
                }
            }
        }

        // validate child-node definitions
        QNodeDefinition[] cnda = ntd.getChildNodeDefs();
        for (QNodeDefinition cnd : cnda) {
            /**
             * sanity check:
             * make sure declaring node type matches name of node type definition
             */
            if (!name.equals(cnd.getDeclaringNodeType())) {
                String msg = "[" + name + "#" + cnd.getName()
                        + "] invalid declaring node type specified";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            checkNamespace(cnd.getName(), nsReg);
            // check that auto-created child-nodes specify a name
            if (cnd.definesResidual() && cnd.isAutoCreated()) {
                String msg = "[" + name + "#" + cnd.getName()
                        + "] auto-created child-nodes must specify a name";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            // check that auto-created child-nodes specify a default primary type
            if (cnd.getDefaultPrimaryType() == null
                    && cnd.isAutoCreated()) {
                String msg = "[" + name + "#" + cnd.getName()
                        + "] auto-created child-nodes must specify a default primary type";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            // check default primary type
            Name dpt = cnd.getDefaultPrimaryType();
            checkNamespace(dpt, nsReg);
            boolean referenceToSelf = false;
            EffectiveNodeType defaultENT = null;
            if (dpt != null) {
                // check if this node type specifies itself as default primary type
                if (name.equals(dpt)) {
                    referenceToSelf = true;
                }
                /**
                 * the default primary type must be registered, with one notable
                 * exception: the node type just being registered
                 */
                if (!name.equals(dpt) && !ntdCache.containsKey(dpt)) {
                    String msg = "[" + name + "#" + cnd.getName()
                            + "] invalid default primary type '" + dpt + "'";
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg);
                }
                /**
                 * build effective (i.e. merged and resolved) node type from
                 * default primary type and check for conflicts
                 */
                try {
                    if (!referenceToSelf) {
                        defaultENT = getEffectiveNodeType(dpt, entCache, ntdCache);
                    } else {
                        /**
                         * the default primary type is identical with the node
                         * type just being registered; we have to instantiate it
                         * 'manually'
                         */
                        ent = EffectiveNodeType.create(ntd, entCache, ntdCache);
                        defaultENT = ent;
                    }
                    if (cnd.isAutoCreated()) {
                        /**
                         * check for circularity through default primary types
                         * of auto-created child nodes (node type 'a' defines
                         * auto-created child node with default primary type 'a')
                         */
                        Stack<Name> definingNTs = new Stack<Name>();
                        definingNTs.push(name);
                        checkForCircularNodeAutoCreation(defaultENT, definingNTs, entCache, ntdCache);
                    }
                } catch (NodeTypeConflictException ntce) {
                    String msg = "[" + name + "#" + cnd.getName()
                            + "] failed to validate default primary type";
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg, ntce);
                } catch (NoSuchNodeTypeException nsnte) {
                    String msg = "[" + name + "#" + cnd.getName()
                            + "] failed to validate default primary type";
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg, nsnte);
                }
            }

            // check required primary types
            Name[] reqTypes = cnd.getRequiredPrimaryTypes();
            if (reqTypes != null && reqTypes.length > 0) {
                for (Name rpt : reqTypes) {
                    // skip nt:base required types
                    if (NameConstants.NT_BASE.equals(rpt)) {
                        continue;
                    }
                    checkNamespace(rpt, nsReg);
                    referenceToSelf = false;
                    /**
                     * check if this node type specifies itself as required
                     * primary type
                     */
                    if (name.equals(rpt)) {
                        referenceToSelf = true;
                    }
                    /**
                     * the required primary type must be registered, with one
                     * notable exception: the node type just being registered
                     */
                    if (!name.equals(rpt) && !ntdCache.containsKey(rpt)) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] invalid required primary type: " + rpt;
                        log.debug(msg);
                        throw new InvalidNodeTypeDefException(msg);
                    }
                    /**
                     * check if default primary type satisfies the required
                     * primary type constraint
                     */
                    if (defaultENT != null && !defaultENT.includesNodeType(rpt)) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] default primary type does not satisfy required primary type constraint "
                                + rpt;
                        log.debug(msg);
                        throw new InvalidNodeTypeDefException(msg);
                    }
                    /**
                     * build effective (i.e. merged and resolved) node type from
                     * required primary type constraint and check for conflicts
                     */
                    try {
                        if (!referenceToSelf) {
                            getEffectiveNodeType(rpt, entCache, ntdCache);
                        } else {
                            /**
                             * the required primary type is identical with the
                             * node type just being registered; we have to
                             * instantiate it 'manually'
                             */
                            if (ent == null) {
                                ent = EffectiveNodeType.create(ntd, entCache, ntdCache);
                            }
                        }
                    } catch (NodeTypeConflictException ntce) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] failed to validate required primary type constraint";
                        log.debug(msg);
                        throw new InvalidNodeTypeDefException(msg, ntce);
                    } catch (NoSuchNodeTypeException nsnte) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] failed to validate required primary type constraint";
                        log.debug(msg);
                        throw new InvalidNodeTypeDefException(msg, nsnte);
                    }
                }
            }
        }

        /**
         * now build effective (i.e. merged and resolved) node type from
         * this node type definition; this will potentially detect more
         * conflicts or problems
         */
        if (ent == null) {
            try {
                ent = EffectiveNodeType.create(ntd, entCache, ntdCache);
            } catch (NodeTypeConflictException ntce) {
                String msg = "[" + name + "] failed to resolve node type definition";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, ntce);
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = "[" + name + "] failed to resolve node type definition";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        }
        return ent;
    }

    private static QNodeDefinition createRootNodeDef() {
        QNodeDefinitionBuilder def = new QNodeDefinitionBuilder();

        // FIXME need a fake declaring node type:
        // rep:root is not quite correct but better than a non-existing node type
        def.setDeclaringNodeType(NameConstants.REP_ROOT);
        def.setRequiredPrimaryTypes(new Name[]{NameConstants.REP_ROOT});
        def.setDefaultPrimaryType(NameConstants.REP_ROOT);
        def.setMandatory(true);
        def.setProtected(false);
        def.setOnParentVersion(OnParentVersionAction.VERSION);
        def.setAllowsSameNameSiblings(false);
        def.setAutoCreated(true);
        return def.build();
    }

    /**
     * Notify the listeners that a node type <code>ntName</code> has been registered.
     * @param ntName node type name
     */
    private void notifyRegistered(Name ntName) {
        // copy listeners to array to avoid ConcurrentModificationException
        NodeTypeRegistryListener[] la = listeners.values().toArray(
                        new NodeTypeRegistryListener[listeners.size()]);
        for (NodeTypeRegistryListener aLa : la) {
            if (aLa != null) {
                aLa.nodeTypeRegistered(ntName);
            }
        }
    }

    /**
     * Notify the listeners that a node type <code>ntName</code> has been re-registered.
     * @param ntName node type name
     */
    private void notifyReRegistered(Name ntName) {
        // copy listeners to array to avoid ConcurrentModificationException
        NodeTypeRegistryListener[] la = listeners.values().toArray(
                        new NodeTypeRegistryListener[listeners.size()]);
        for (NodeTypeRegistryListener aLa : la) {
            if (aLa != null) {
                aLa.nodeTypeReRegistered(ntName);
            }
        }
    }

    /**
     * Notify the listeners that oone or more node types have been unregistered.
     * @param names node type names
     */
    private void notifyUnregistered(Collection<Name> names) {
        // copy listeners to array to avoid ConcurrentModificationException
        NodeTypeRegistryListener[] la = listeners.values().toArray(
                        new NodeTypeRegistryListener[listeners.size()]);
        for (NodeTypeRegistryListener aLa : la) {
            if (aLa != null) {
                aLa.nodeTypesUnregistered(names);
            }
        }
    }
}
