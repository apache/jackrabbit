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
package org.apache.jackrabbit.core.observation;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.CachingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

import javax.jcr.observation.Event;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.NamespaceException;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * The <code>EventState</code> class encapsulates the session
 * independent state of an {@link javax.jcr.observation.Event}.
 */
public class EventState {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(EventState.class);

    /**
     * The caching path resolver.
     */
    private static CachingPathResolver cachingPathResolver;

    /**
     * The key <code>srcAbsPath</code> in the info map.
     */
    static final String SRC_ABS_PATH = "srcAbsPath";

    /**
     * The key <code>destAbsPath</code> in the info map.
     */
    static final String DEST_ABS_PATH = "destAbsPath";

    /**
     * The key <code>srcChildRelPath</code> in the info map.
     */
    static final String SRC_CHILD_REL_PATH = "srcChildRelPath";

    /**
     * The key <code>destChildRelPath</code> in the info map.
     */
    static final String DEST_CHILD_REL_PATH = "destChildRelPath";

    /**
     * The {@link javax.jcr.observation.Event} of this event.
     */
    private final int type;

    /**
     * The Id of the parent node associated with this event.
     */
    private final NodeId parentId;

    /**
     * The path of the parent node associated with this event.
     */
    private final Path parentPath;

    /**
     * The UUID of a child node, in case this EventState is of type
     * {@link javax.jcr.observation.Event#NODE_ADDED} or
     * {@link javax.jcr.observation.Event#NODE_REMOVED}.
     */
    private final NodeId childId;

    /**
     * The relative path of the child item associated with this event.
     * This is basically the name of the item with an optional index.
     */
    private final Path childRelPath;

    /**
     * The node type name of the parent node.
     */
    private final Name nodeType;

    /**
     * Set of mixin QNames assigned to the parent node.
     */
    private final Set<Name> mixins;

    /**
     * Set of node types. This Set consists of the primary node type and all
     * mixin types assigned to the associated parent node of this event state.
     * </p>
     * This <code>Set</code> is initialized when
     * {@link #getNodeTypes(NodeTypeManagerImpl)} is called for the first time.
     */
    private Set<NodeType> allTypes;

    /**
     * The session that caused this event.
     */
    private final Session session;

    /**
     * Cached String representation of this <code>EventState</code>.
     */
    private String stringValue;

    /**
     * Cached hashCode value for this <code>Event</code>.
     */
    private int hashCode;

    /**
     * Flag indicating whether this is an external event, e.g. originating from
     * another node in a clustered environment.
     */
    private final boolean external;

    /**
     * The info Map associated with this event.
     */
    private Map<String, InternalValue> info = Collections.emptyMap();

    /**
     * If set to <code>true</code>, indicates that the child node of a node
     * added or removed event is a shareable node.
     */
    private boolean shareableNode;

    /**
     * Creates a new <code>EventState</code> instance.
     *
     * @param type       the type of this event.
     * @param parentId   the id of the parent node associated with this event.
     * @param parentPath the path of the parent node associated with this
     *                   event.
     * @param childId    the id of the child node associated with this event.
     *                   If the event type is one of: <code>PROPERTY_ADDED</code>,
     *                   <code>PROPERTY_CHANGED</code> or <code>PROPERTY_REMOVED</code>
     *                   this parameter must be <code>null</code>.
     * @param childPath  the relative path of the child item associated with
     *                   this event.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the {@link javax.jcr.Session} that caused this event.
     */
    private EventState(int type, NodeId parentId, Path parentPath,
                       NodeId childId, Path childPath, Name nodeType,
                       Set<Name> mixins, Session session, boolean external) {

        int mask = (Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED);
        if ((type & mask) > 0) {
            if (childId != null) {
                throw new IllegalArgumentException("childId only allowed for Node events.");
            }
        } else {
            if (childId == null && type != Event.PERSIST) {
                throw new IllegalArgumentException("childId must not be null for Node events.");
            }
        }
        this.type = type;
        this.parentId = parentId;
        this.parentPath = parentPath;
        this.childId = childId;
        this.childRelPath = childPath;
        this.nodeType = nodeType;
        this.mixins = mixins;
        this.session = session;
        this.external = external;
    }

    //-----------------< factory methods >--------------------------------------

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#NODE_ADDED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childId    the id of the child node associated with this event.
     * @param childPath  the relative path of the child node that was added.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that added the node.
     * @return an <code>EventState</code> instance.
     */
    public static EventState childNodeAdded(NodeId parentId,
                                            Path parentPath,
                                            NodeId childId,
                                            Path childPath,
                                            Name nodeType,
                                            Set<Name> mixins,
                                            Session session) {

        return childNodeAdded(parentId, parentPath, childId,
                childPath, nodeType, mixins, session, false);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#NODE_ADDED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childId    the id of the child node associated with this event.
     * @param childPath  the relative path of the child node that was added.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that added the node.
     * @param external   flag indicating whether this is an external event
     * @return an <code>EventState</code> instance.
     */
    public static EventState childNodeAdded(NodeId parentId,
                                            Path parentPath,
                                            NodeId childId,
                                            Path childPath,
                                            Name nodeType,
                                            Set<Name> mixins,
                                            Session session,
                                            boolean external) {

        return new EventState(Event.NODE_ADDED, parentId, parentPath,
                childId, childPath, nodeType, mixins, session, external);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#NODE_REMOVED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childId    the id of the child node associated with this event.
     * @param childPath  the relative path of the child node that was removed.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that removed the node.
     * @return an <code>EventState</code> instance.
     */
    public static EventState childNodeRemoved(NodeId parentId,
                                              Path parentPath,
                                              NodeId childId,
                                              Path childPath,
                                              Name nodeType,
                                              Set<Name> mixins,
                                              Session session) {

        return childNodeRemoved(parentId, parentPath, childId,
                childPath, nodeType, mixins, session, false);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#NODE_REMOVED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childId    the id of the child node associated with this event.
     * @param childPath  the relative path of the child node that was removed.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that removed the node.
     * @param external   flag indicating whether this is an external event
     * @return an <code>EventState</code> instance.
     */
    public static EventState childNodeRemoved(NodeId parentId,
                                              Path parentPath,
                                              NodeId childId,
                                              Path childPath,
                                              Name nodeType,
                                              Set<Name> mixins,
                                              Session session,
                                              boolean external) {

        return new EventState(Event.NODE_REMOVED, parentId, parentPath,
                childId, childPath, nodeType, mixins, session, external);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * <code>NODE_MOVED</code>. The parent node associated with this event type
     * is the parent node of the destination of the move!
     * This method creates an event state without an info map. A caller of this
     * method must ensure that it is properly set afterwards.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childId    the id of the child node associated with this event.
     * @param childPath  the relative path of the child node that was moved.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that moved the node.
     * @param external   flag indicating whether this is an external event
     * @return an <code>EventState</code> instance.
     */
    public static EventState nodeMoved(NodeId parentId,
                                       Path parentPath,
                                       NodeId childId,
                                       Path childPath,
                                       Name nodeType,
                                       Set<Name> mixins,
                                       Session session,
                                       boolean external) {
        return new EventState(Event.NODE_MOVED, parentId, parentPath,
                childId, childPath, nodeType, mixins, session, external);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * <code>NODE_MOVED</code>. The parent node associated with this event type
     * is the parent node of the destination of the move!
     *
     * @param parentId the id of the parent node associated with this
     *                 <code>EventState</code>.
     * @param destPath the path of the destination of the move.
     * @param childId  the id of the child node associated with this event.
     * @param srcPath  the path of the source of the move.
     * @param nodeType the node type of the parent node.
     * @param mixins   mixins assigned to the parent node.
     * @param session  the session that removed the node.
     * @param external flag indicating whether this is an external event
     * @return an <code>EventState</code> instance.
     * @throws ItemStateException if <code>destPath</code> does not have a
     *                            parent.
     */
    public static EventState nodeMovedWithInfo(
            NodeId parentId, Path destPath, NodeId childId, Path srcPath,
            Name nodeType, Set<Name> mixins, Session session, boolean external)
            throws ItemStateException {
        try {
            EventState es = nodeMoved(parentId, destPath.getAncestor(1),
                    childId, destPath, nodeType, mixins,
                    session, external);
            Map<String, InternalValue> info = new HashMap<String, InternalValue>();
            info.put(SRC_ABS_PATH, InternalValue.create(srcPath));
            info.put(DEST_ABS_PATH, InternalValue.create(destPath));
            es.setInfo(info);
            return es;
        } catch (RepositoryException e) {
            // should never happen actually
            String msg = "Unable to resolve parent for path: " + destPath;
            log.error(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * <code>NODE_MOVED</code>. The parent node associated with this event type
     * is the parent node of the destination of the reorder!
     *
     * @param parentId      the id of the parent node associated with this
     *                      <code>EventState</code>.
     * @param parentPath    the path of the parent node associated with
     *                      this <code>EventState</code>.
     * @param childId       the id of the child node associated with this
     *                      event.
     * @param destChildPath the name element of the node before it was reordered.
     * @param srcChildPath  the name element of the reordered node before the
     *                      reorder operation.
     * @param beforeChildPath the name element of the node before which the
     *                      reordered node is placed. (may be <code>null</code>
     *                      if reordered to the end.
     * @param nodeType      the node type of the parent node.
     * @param mixins        mixins assigned to the parent node.
     * @param session       the session that removed the node.
     * @param external      flag indicating whether this is an external event
     * @return an <code>EventState</code> instance.
     */
    public static EventState nodeReordered(NodeId parentId,
                                           Path parentPath,
                                           NodeId childId,
                                           Path destChildPath,
                                           Path srcChildPath,
                                           Path beforeChildPath,
                                           Name nodeType,
                                           Set<Name> mixins,
                                           Session session,
                                           boolean external) {
        EventState es = nodeMoved(
                parentId, parentPath, childId, destChildPath,
                nodeType, mixins, session, external);
        Map<String, InternalValue> info = new HashMap<String, InternalValue>();
        info.put(SRC_CHILD_REL_PATH, createValue(srcChildPath));
        InternalValue value = null;
        if (beforeChildPath != null) {
            value = createValue(beforeChildPath);
        }
        info.put(DEST_CHILD_REL_PATH, value);
        es.setInfo(info);
        return es;
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_ADDED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childPath  the relative path of the property that was added.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that added the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyAdded(NodeId parentId,
                                           Path parentPath,
                                           Path childPath,
                                           Name nodeType,
                                           Set<Name> mixins,
                                           Session session) {

        return propertyAdded(parentId, parentPath, childPath,
                nodeType, mixins, session, false);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_ADDED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childPath  the relative path of the property that was added.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that added the property.
     * @param external   flag indicating whether this is an external event
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyAdded(NodeId parentId,
                                           Path parentPath,
                                           Path childPath,
                                           Name nodeType,
                                           Set<Name> mixins,
                                           Session session,
                                           boolean external) {

        return new EventState(Event.PROPERTY_ADDED, parentId, parentPath,
                null, childPath, nodeType, mixins, session, external);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_REMOVED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childPath  the relative path of the property that was removed.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that removed the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyRemoved(NodeId parentId,
                                             Path parentPath,
                                             Path childPath,
                                             Name nodeType,
                                             Set<Name> mixins,
                                             Session session) {

        return propertyRemoved(parentId, parentPath, childPath,
                nodeType, mixins, session, false);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_REMOVED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childPath  the relative path of the property that was removed.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that removed the property.
     * @param external   flag indicating whether this is an external event
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyRemoved(NodeId parentId,
                                             Path parentPath,
                                             Path childPath,
                                             Name nodeType,
                                             Set<Name> mixins,
                                             Session session,
                                             boolean external) {

        return new EventState(Event.PROPERTY_REMOVED, parentId, parentPath,
                null, childPath, nodeType, mixins, session, external);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_CHANGED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childPath  the relative path of the property that changed.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that changed the property.
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyChanged(NodeId parentId,
                                             Path parentPath,
                                             Path childPath,
                                             Name nodeType,
                                             Set<Name> mixins,
                                             Session session) {

        return propertyChanged(parentId, parentPath, childPath,
                nodeType, mixins, session, false);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PROPERTY_CHANGED}.
     *
     * @param parentId   the id of the parent node associated with
     *                   this <code>EventState</code>.
     * @param parentPath the path of the parent node associated with
     *                   this <code>EventState</code>.
     * @param childPath  the relative path of the property that changed.
     * @param nodeType   the node type of the parent node.
     * @param mixins     mixins assigned to the parent node.
     * @param session    the session that changed the property.
     * @param external   flag indicating whether this is an external event
     * @return an <code>EventState</code> instance.
     */
    public static EventState propertyChanged(NodeId parentId,
                                             Path parentPath,
                                             Path childPath,
                                             Name nodeType,
                                             Set<Name> mixins,
                                             Session session,
                                             boolean external) {

        return new EventState(Event.PROPERTY_CHANGED, parentId, parentPath,
                null, childPath, nodeType, mixins, session, external);
    }

    /**
     * Creates a new {@link javax.jcr.observation.Event} of type
     * {@link javax.jcr.observation.Event#PERSIST}.
     *
     * @param session    the session that changed the property.
     * @param external   flag indicating whether this is an external event
     * @return an <code>EventState</code> instance.
     */
    public static EventState persist(Session session, boolean external) {

        return new EventState(Event.PERSIST, null, null, null, null,
                null, null, session, external);
    }

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the uuid of the parent node.
     *
     * @return the uuid of the parent node.
     */
    public NodeId getParentId() {
        return parentId;
    }

    /**
     * Returns the path of the parent node.
     *
     * @return the path of the parent node.
     */
    public Path getParentPath() {
        return parentPath;
    }

    /**
     * Returns the Id of a child node operation.
     * If this <code>EventState</code> was generated for a property
     * operation this method returns <code>null</code>.
     *
     * @return the id of a child node operation.
     */
    public NodeId getChildId() {
        return childId;
    }

    /**
     * Returns the relative {@link Path} of the child
     * {@link javax.jcr.Item} associated with this event.
     *
     * @return the <code>Path</code> associated with this event.
     */
    public Path getChildRelPath() {
        return childRelPath;
    }

    /**
     * Returns the node type of the parent node associated with this event.
     *
     * @return the node type of the parent associated with this event.
     */
    public Name getNodeType() {
        return nodeType;
    }

    /**
     * Returns a set of <code>Name</code>s which are the names of the mixins
     * assigned to the parent node associated with this event.
     *
     * @return the mixin names as <code>Name</code>s.
     */
    public Set<Name> getMixinNames() {
        return mixins;
    }

    /**
     * Returns the <code>Set</code> of {@link javax.jcr.nodetype.NodeType}s
     * assigned to the parent node associated with this event. This
     * <code>Set</code> includes the primary type as well as all the mixin types
     * assigned to the parent node.
     *
     * @return <code>Set</code> of {@link javax.jcr.nodetype.NodeType}s.
     */
    public Set<NodeType> getNodeTypes(NodeTypeManagerImpl ntMgr) {
        if (allTypes == null) {
            Set<NodeType> tmp = new HashSet<NodeType>();
            try {
                tmp.add(ntMgr.getNodeType(nodeType));
            } catch (NoSuchNodeTypeException e) {
                log.warn("Unknown node type: " + nodeType);
            }
            Iterator<Name> it = mixins.iterator();
            while (it.hasNext()) {
                Name mixinName = it.next();
                try {
                    tmp.add(ntMgr.getNodeType(mixinName));
                } catch (NoSuchNodeTypeException e) {
                    log.warn("Unknown node type: " + mixinName);
                }
            }
            allTypes = Collections.unmodifiableSet(tmp);
        }
        return allTypes;
    }

    /**
     * {@inheritDoc}
     */
    public String getUserId() {
        return session.getUserID();
    }

    /**
     * Returns the <code>Session</code> that caused / created this
     * <code>EventState</code>.
     *
     * @return the <code>Session</code> that caused / created this
     *         <code>EventState</code>.
     */
    Session getSession() {
        return session;
    }

    /**
     * Returns the id of the associated item of this <code>EventState</code>.
     *
     * @return the <code>ItemId</code> or <code>null</code> for {@link Event#PERSIST} events 
     */
    ItemId getTargetId() {
        if (type == Event.PERSIST) {
            return null;
        } else if (childId == null) {
            // property event
            return new PropertyId(parentId, childRelPath.getName());
        } else {
            // node event
            return childId;
        }
    }

    /**
     * Return a flag indicating whether this is an externally generated event.
     *
     * @return <code>true</code> if this is an external event;
     *         <code>false</code> otherwise
     */
    boolean isExternal() {
        return external;
    }

    /**
     * @return an unmodifiable info Map.
     */
    public Map<String, InternalValue> getInfo() {
        return info;
    }

    /**
     * Sets a new info map for this event.
     *
     * @param info the new info map.
     */
    public void setInfo(Map<String, InternalValue> info) {
        this.info = Collections.unmodifiableMap(new HashMap<String, InternalValue>(info));
    }

    /**
     * Returns a flag indicating whether the child node of this event is a
     * shareable node. Only applies to node added/removed events.
     *
     * @return <code>true</code> for a shareable child node, <code>false</code>
     *         otherwise.
     */
    boolean isShareableNode() {
        return shareableNode;
    }

    /**
     * Sets a new value for the {@link #shareableNode} flag.
     *
     * @param shareableNode whether the child node is shareable.
     * @see #isShareableNode()
     */
    void setShareableNode(boolean shareableNode) {
        this.shareableNode = shareableNode;
    }

    /**
     * Returns a String representation of this <code>EventState</code>.
     *
     * @return a String representation of this <code>EventState</code>.
     */
    public String toString() {
        if (stringValue == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("EventState: ").append(valueOf(type));
            sb.append(", Parent: ").append(parentId);
            sb.append(", Child: ").append(childRelPath);
            sb.append(", UserId: ").append(session.getUserID());
            sb.append(", Info: ").append(info);
            stringValue = sb.toString();
        }
        return stringValue;
    }

    /**
     * Returns a hashCode for this <code>EventState</code>.
     *
     * @return a hashCode for this <code>EventState</code>.
     */
    public int hashCode() {
        int h = hashCode;
        if (h == 0) {
            h = 37;
            h = 37 * h + type;
            h = 37 * h + (parentId != null ? parentId.hashCode() : 0);
            h = 37 * h + (childRelPath != null ? childRelPath.hashCode() : 0);
            h = 37 * h + session.hashCode();
            h = 37 * h + info.hashCode();
            hashCode = h;
        }
        return hashCode;
    }

    /**
     * Returns <code>true</code> if this <code>EventState</code> is equal to
     * another object.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if object <code>obj</code> is equal to this
     *         <code>EventState</code>; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof EventState) {
            EventState other = (EventState) obj;
            return this.type == other.type
                    && this.parentId.equals(other.parentId)
                    && this.childRelPath.equals(other.childRelPath)
                    && this.session.equals(other.session)
                    && this.info.equals(other.info);
        }
        return false;
    }

    /**
     * Returns a String representation of <code>eventType</code>.
     *
     * @param eventType an event type defined by {@link Event}.
     * @return a String representation of <code>eventType</code>.
     */
    public static String valueOf(int eventType) {
        if (eventType == Event.NODE_ADDED) {
            return "NodeAdded";
        } else if (eventType == Event.NODE_MOVED) {
            return "NodeMoved";
        } else if (eventType == Event.NODE_REMOVED) {
            return "NodeRemoved";
        } else if (eventType == Event.PROPERTY_ADDED) {
            return "PropertyAdded";
        } else if (eventType == Event.PROPERTY_CHANGED) {
            return "PropertyChanged";
        } else if (eventType == Event.PROPERTY_REMOVED) {
            return "PropertyRemoved";
        } else if (eventType == Event.PERSIST) {
            return "Persist";
        } else {
            return "UnknownEventType";
        }
    }

    /**
     * Creates an internal path value from the given path.
     *
     * @param path the path
     * @return an internal value wrapping the path
     */
    private static InternalValue createValue(Path path) {
        return InternalValue.create(path);
    }

    /**
     * Get the longest common path of all event state paths.
     *
     * @param events The list of EventState
     * @param session The associated session; it can be null
     * @return the longest common path
     */
    public static String getCommonPath(List<EventState> events, SessionImpl session) {
        String common = null;
        try {
            for (int i = 0; i < events.size(); i++) {
                EventState state = events.get(i);
                Path parentPath = state.getParentPath();
                String s;
                if (session == null) {
                    s = getJCRPath(parentPath);
                } else {
                    s = session.getJCRPath(parentPath);
                }

                if (common == null) {
                    common = s;
                } else if (!common.equals(s)) {

                    // Assign the shorter path to common.
                    if (s.length() < common.length()) {
                        String temp = common;
                        common = s;
                        s = temp;
                    }

                    // Find the real common.
                    while (!s.startsWith(common)) {
                        int idx = s.lastIndexOf('/');
                        if (idx < 0) {
                            break;
                        }
                        common = s.substring(0, idx + 1);
                    }
                }
            }
        } catch (NamespaceException e) {
            log.debug("Problem in retrieving JCR path", e);
        }
        return common;
    }

    private static String getJCRPath(Path path) {
 
        setupCachingPathResolver();

        String jcrPath;
        try {
            jcrPath = cachingPathResolver.getJCRPath(path);
        } catch (NamespaceException e) {
            jcrPath = "";
            log.debug("Problem in retrieving JCR path", e);
        }
        return jcrPath;
    }

    private static void setupCachingPathResolver() {
        if (cachingPathResolver != null) {
            return;
        }

        PathResolver pathResolver = new ParsingPathResolver(PathFactoryImpl.getInstance(), new NameResolver() {
            public Name getQName(String name) throws IllegalNameException, NamespaceException {
                return null;
            }

            public String getJCRName(Name name) throws NamespaceException {
                return name.getLocalName();
            }
        });

        cachingPathResolver = new CachingPathResolver(pathResolver);
    }
}
