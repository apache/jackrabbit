/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.nodetype.*;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.jcr.*;
import javax.jcr.access.AccessDeniedException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A <code>SessionImpl</code> ...
 */
public class SessionImpl implements Session {

    private static Logger log = Logger.getLogger(SessionImpl.class);

    /**
     * the repository that issued this session
     */
    protected final RepositoryImpl rep;

    /**
     * the user ID that was used to acquire this session
     */
    protected final String userId;
    /**
     * the attibutes of this session
     */
    protected final HashMap attributes = new HashMap();

    /**
     * the node type manager
     */
    protected final NodeTypeManagerImpl ntMgr;

    /**
     * the AccessManager associated with this session
     */
    protected AccessManagerImpl accessMgr;

    /**
     * the item state mgr associated with this session
     */
    protected final SessionItemStateManager itemStateMgr;

    /**
     * the HierarchyManager associated with this session
     */
    protected final HierarchyManager hierMgr;

    /**
     * the item mgr associated with this session
     */
    protected final ItemManager itemMgr;

    /**
     * the Workspace associated with this session
     */
    protected final WorkspaceImpl wsp;

    /**
     * the transient prefix/namespace mappings with session scope
     */
    protected final TransientNamespaceMappings nsMappings;

    /**
     * Package private constructor.
     *
     * @param rep
     * @param credentials
     * @param wspConfig
     */
    SessionImpl(RepositoryImpl rep, Credentials credentials, WorkspaceConfig wspConfig)
            throws RepositoryException {
        this.rep = rep;

        if (credentials instanceof SimpleCredentials) {
            SimpleCredentials sc = (SimpleCredentials) credentials;
            // clear password for security reasons
            char[] pwd = sc.getPassword();
            if (pwd != null) {
                for (int i = 0; i < pwd.length; i++) {
                    pwd[i] = 0;
                }
            }
            userId = sc.getUserId();
            String[] names = sc.getAttributeNames();
            for (int i = 0; i < names.length; i++) {
                attributes.put(names[i], sc.getAttribute(names[i]));
            }
        } else {
            userId = null;
        }

        nsMappings = new TransientNamespaceMappings(rep.getNamespaceRegistry());

        ntMgr = new NodeTypeManagerImpl(rep.getNodeTypeRegistry(), getNamespaceResolver());
        String wspName = wspConfig.getName();
        wsp = new WorkspaceImpl(wspConfig, rep.getWorkspaceStateManager(wspName),
                rep.getWorkspaceReferenceManager(wspName), rep, this);
        itemStateMgr = new SessionItemStateManager(rep.getRootNodeUUID(), wsp.getPersistentStateManager(), getNamespaceResolver());
        hierMgr = itemStateMgr.getHierarchyMgr();
        itemMgr = new ItemManager(itemStateMgr, hierMgr, this, ntMgr.getRootNodeDefinition(), rep.getRootNodeUUID());
        accessMgr = new AccessManagerImpl(credentials, hierMgr, getNamespaceResolver());
    }

    /**
     * Protected constructor.
     *
     * @param rep
     * @param userId
     * @param wspConfig
     */
    protected SessionImpl(RepositoryImpl rep, String userId, WorkspaceConfig wspConfig)
            throws RepositoryException {
        this.rep = rep;

        this.userId = userId;

        nsMappings = new TransientNamespaceMappings(rep.getNamespaceRegistry());

        ntMgr = new NodeTypeManagerImpl(rep.getNodeTypeRegistry(), getNamespaceResolver());
        String wspName = wspConfig.getName();
        wsp = new WorkspaceImpl(wspConfig, rep.getWorkspaceStateManager(wspName),
                rep.getWorkspaceReferenceManager(wspName), rep, this);
        itemStateMgr = new SessionItemStateManager(rep.getRootNodeUUID(), wsp.getPersistentStateManager(), getNamespaceResolver());
        hierMgr = itemStateMgr.getHierarchyMgr();
        itemMgr = new ItemManager(itemStateMgr, hierMgr, this, ntMgr.getRootNodeDefinition(), rep.getRootNodeUUID());
    }

    /**
     * Returns the <code>AccessManager</code> associated with this session.
     *
     * @return the <code>AccessManager</code> associated with this session
     */
    AccessManagerImpl getAccessManager() {
        return accessMgr;
    }

    /**
     * Returns the <code>NodeTypeManager</code>.
     *
     * @return the <code>NodeTypeManager</code>
     */
    public NodeTypeManagerImpl getNodeTypeManager() {
        return ntMgr;
    }

    /**
     * Returns the <code>ItemManager</code> of this session.
     *
     * @return the <code>ItemManager</code>
     */
    ItemManager getItemManager() {
        return itemMgr;
    }

    /**
     * Returns the <code>NamespaceResolver</code> of this session.
     *
     * @return the <code>NamespaceResolver</code> of this session
     */
    public NamespaceResolver getNamespaceResolver() {
        return nsMappings;
    }

    /**
     * Returns the <code>SessionItemStateManager</code> associated with this session.
     *
     * @return the <code>SessionItemStateManager</code> associated with this session
     */
    SessionItemStateManager getItemStateManager() {
        return itemStateMgr;
    }

    /**
     * Returns the <code>HierarchyManager</code> associated with this session.
     *
     * @return the <code>HierarchyManager</code> associated with this session
     */
    HierarchyManager getHierarchyManager() {
        return hierMgr;
    }

    /**
     * Dumps the state of this <code>Session</code> instance
     * (used for diagnostic purposes).
     *
     * @param ps
     * @throws RepositoryException
     */
    void dump(PrintStream ps) throws RepositoryException {
        ps.println("Session: " + (userId == null ? "unknown" : userId) + " (" + this + ")");
        ps.println();
        itemMgr.dump(ps);
    }

    //--------------------------------------------------------------< Session >
    /**
     * @see Session#getWorkspace()
     */
    public Workspace getWorkspace() {
        return wsp;
    }

    /**
     * @see Session#impersonate(Credentials)
     */
    public Session impersonate(Credentials otherCredentials)
            throws LoginException, RepositoryException {
        // @todo reimplement impersonate(Credentials) correctly

        // check if the credentials of this session allow to 'impersonate'
        // the user represented by tha supplied credentials

        // FIXME: the original purpose of this method is to enable
        // a 'superuser' to impersonate another user without needing
        // to know its password.
        try {
            return rep.login(otherCredentials, null);
        } catch (NoSuchWorkspaceException nswe) {
            // should never get here...
            String msg = "impersonate failed";
            log.error(msg, nswe);
            throw new LoginException(msg, nswe);
        }
    }

    /**
     * @see Session#getRootNode
     */
    public Node getRootNode() throws RepositoryException {
        return (Node) itemMgr.getRootNode();
    }

    /**
     * @see Session#getNodeByUUID(String)
     */
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        try {
            NodeImpl node = (NodeImpl) itemMgr.getItem(new NodeId(uuid));
            if (node.isNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
                return node;
            } else {
                // there is a node with that uuid but the node does not expose it
                throw new ItemNotFoundException(uuid);
            }
        } catch (AccessDeniedException ade) {
            throw new ItemNotFoundException(uuid);
        }
    }

    /**
     * @see Session#getItem(String)
     */
    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        try {
            return itemMgr.getItem(Path.create(absPath, getNamespaceResolver(), true));
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(absPath);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path:" + absPath;
            log.error(msg);
            throw new RepositoryException(msg, mpe);
        }
    }

    /**
     * @see Session#itemExists(String)
     */
    public boolean itemExists(String absPath) {
        try {
            itemMgr.getItem(Path.create(absPath, getNamespaceResolver(), true));
            return true;
        } catch (RepositoryException re) {
            // fall through...
        } catch (MalformedPathException mpe) {
            // fall through...
        }
        return false;
    }

    /**
     * @see Session#save
     */
    public void save() throws AccessDeniedException, LockException, ConstraintViolationException, InvalidItemStateException, RepositoryException {
        itemMgr.getRootNode().save();
    }

    /**
     * @see Session#refresh(boolean)
     */
    public void refresh(boolean keepChanges) throws RepositoryException {
        if (!keepChanges) {
            // optimization
            itemStateMgr.disposeAllTransientItemStates();
            return;
        }
        itemMgr.getRootNode().refresh(keepChanges);
    }

    /**
     * @see Session#hasPendingChanges
     */
    public boolean hasPendingChanges() throws RepositoryException {
        return itemStateMgr.hasAnyTransientItemStates();
    }

    /**
     * @see Session#move(String, String)
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws ItemExistsException, PathNotFoundException,
            ConstraintViolationException, RepositoryException {

        // check paths & get node instances

        Path srcPath;
        Path.PathElement srcName;
        Path srcParentPath;
        NodeImpl targetNode;
        NodeImpl srcParentNode;
        try {
            srcPath = Path.create(srcAbsPath, getNamespaceResolver(), true);
            srcName = srcPath.getNameElement();
            srcParentPath = srcPath.getAncestor(1);
            ItemImpl item = itemMgr.getItem(srcPath);
            if (!item.isNode()) {
                throw new PathNotFoundException(srcAbsPath);
            }
            targetNode = (NodeImpl) item;
            srcParentNode = (NodeImpl) itemMgr.getItem(srcParentPath);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(srcAbsPath);
        } catch (MalformedPathException mpe) {
            String msg = srcAbsPath + ": invalid path";
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }

        Path destPath;
        Path.PathElement destName;
        Path destParentPath;
        NodeImpl destParentNode;
        try {
            destPath = Path.create(destAbsPath, getNamespaceResolver(), true);
            destName = destPath.getNameElement();
            destParentPath = destPath.getAncestor(1);
            destParentNode = (NodeImpl) itemMgr.getItem(destParentPath);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(destAbsPath);
        } catch (MalformedPathException mpe) {
            String msg = destAbsPath + ": invalid path";
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }
        int ind = destName.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg = destAbsPath + ": invalid destination path (subscript in name element is not allowed)";
            log.error(msg);
            throw new RepositoryException(msg);
        }

        // check for name collisions

        try {
            ItemImpl item = itemMgr.getItem(destPath);
            if (!item.isNode()) {
                // there's already a property with that name
                throw new ItemExistsException(item.safeGetJCRPath());
            } else {
                // there's already a node with that name
                // check same-name sibling setting of both new and existing node
                if (!destParentNode.getDefinition().allowSameNameSibs()
                        || !((NodeImpl) item).getDefinition().allowSameNameSibs()) {
                    throw new ItemExistsException(item.safeGetJCRPath());
                }
            }
        } catch (AccessDeniedException ade) {
            // FIXME by throwing ItemExistsException we're disclosing too much information
            throw new ItemExistsException(destAbsPath);
        } catch (PathNotFoundException pnfe) {
            // no name collision
        }

        // check constraints

        // get applicable definition of target node at new location
        NodeTypeImpl nt = (NodeTypeImpl) targetNode.getPrimaryNodeType();
        NodeDefImpl newTargetDef;
        try {
            newTargetDef = destParentNode.getApplicableChildNodeDef(destName.getName(), nt.getQName());
        } catch (RepositoryException re) {
            String msg = destAbsPath + ": no definition found in parent node's node type for new node";
            log.error(msg, re);
            throw new ConstraintViolationException(msg, re);
        }
        // check protected flag of old & new parent
        if (destParentNode.getDefinition().isProtected()) {
            String msg = destAbsPath + ": cannot add a child node to a protected node";
            log.error(msg);
            throw new ConstraintViolationException(msg);
        }
        if (srcParentNode.getDefinition().isProtected()) {
            String msg = srcAbsPath + ": cannot remove a child node from a protected node";
            log.error(msg);
            throw new ConstraintViolationException(msg);
        }

        // do move operation

        String targetUUID = ((NodeState) targetNode.getItemState()).getUUID();
        // add target to new parent
        destParentNode.createChildNodeLink(destName.getName(), targetUUID);
        // remove target from old parent
        srcParentNode.removeChildNode(srcName.getName(), srcName.getIndex() == 0 ? 1 : srcName.getIndex());
        // change definition of target if necessary
        NodeDefImpl oldTargetDef = (NodeDefImpl) targetNode.getDefinition();
        NodeDefId oldTargetDefId = new NodeDefId(oldTargetDef.unwrap());
        NodeDefId newTargetDefId = new NodeDefId(newTargetDef.unwrap());
        if (!oldTargetDefId.equals(newTargetDefId)) {
            targetNode.onRedefine(newTargetDefId);
        }
    }

    /**
     * @see Session#getImportContentHandler(String)
     */
    public ContentHandler getImportContentHandler(String parentAbsPath) throws PathNotFoundException, RepositoryException {
        Item item = null;
        try {
            item = itemMgr.getItem(Path.create(parentAbsPath, getNamespaceResolver(), true));
        } catch (MalformedPathException mpe) {
            String msg = parentAbsPath + ": invalid path";
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(parentAbsPath);
        }
        if (!item.isNode()) {
            String msg = parentAbsPath + ": node expected";
            log.error(msg);
            throw new RepositoryException(msg);
        }
        NodeImpl parent = (NodeImpl) item;
        return new ImportHandler(parent, rep.getNamespaceRegistry(), this);
    }

    /**
     * @see Session#importXML(String, InputStream)
     */
    public void importXML(String parentAbsPath, InputStream in)
            throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, InvalidSerializedDataException,
            RepositoryException {
        ImportHandler handler = (ImportHandler) getImportContentHandler(parentAbsPath);
        try {
            XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
/*
	    parser.setFeature("http://xml.org/sax/features/validation", true);
	    parser.setFeature("http://apache.org/xml/features/validation/schema", true);
	    parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
*/
            parser.setContentHandler(handler);
            parser.setErrorHandler(handler);
/*
	    // validate against system view schema
	    URL urlSchema = this.class.getClassLoader().getResource("javax/jcr/systemview.xsd");
	    parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", urlSchema.toString());
	    parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",
		    urlSchema.toString() + " " + "http://www.jcp.org/jcr/sv/1.0");
*/
            parser.parse(new InputSource(in));
        } catch (SAXException se) {
            // check for wrapped repository exception
            Exception e = se.getException();
            if (e != null && e instanceof RepositoryException) {
                throw (RepositoryException) e;
            } else {
                String msg = "failed to parse XML stream";
                log.error(msg, se);
                throw new InvalidSerializedDataException(msg, se);
            }
        }
    }

    /**
     * @see Session#logout()
     */
    public void logout() {
        // discard all transient changes
        itemStateMgr.disposeAllTransientItemStates();

        // @todo invalidate session, release session-scoped locks, free resources, prepare to get gc'ed etc.

        log.debug("disposing workspace...");
        wsp.dispose();
    }

    /**
     * @see Session#getRepository
     */
    public Repository getRepository() {
        return rep;
    }

    /**
     * @see Session#getUserId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @see Session#getAttribute
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * @see Session#getAttributeNames
     */
    public String[] getAttributeNames() {
        return (String[]) attributes.keySet().toArray(new String[attributes.size()]);
    }

    /**
     * @see Session#setNamespacePrefix(String, String)
     */
    public void setNamespacePrefix(String prefix, String uri) throws NamespaceException, RepositoryException {
        nsMappings.setNamespacePrefix(prefix, uri);
    }

    /**
     * @see Session#getNamespacePrefixes
     */
    public String[] getNamespacePrefixes() {
        return nsMappings.getPrefixes();
    }

    /**
     * @see Session#getNamespaceURI(String)
     */
    public String getNamespaceURI(String prefix) throws NamespaceException {
        return nsMappings.getURI(prefix);
    }

    /**
     * @see Session#getNamespaceURI(String)
     */
    public String getNamespacePrefix(String uri) throws NamespaceException {
        return nsMappings.getPrefix(uri);
    }

    //------------------------------------------------------< locking support >
    /**
     * @see Session#addLockToken(String)
     */
    public void addLockToken(String lt) {
        // @todo implement locking support
        throw new UnsupportedOperationException("Locking not implemented yet.");
    }

    /**
     * @see Session#getLockTokens()
     */
    public String[] getLockTokens() {
        // @todo implement locking support
        return new String[0];
    }

    /**
     * @see Session#removeLockToken(String)
     */
    public void removeLockToken(String lt) {
        // @todo implement locking support
        throw new UnsupportedOperationException("Locking not implemented yet.");
    }

    //--------------------------------------------------------< inner classes >
    class TransientNamespaceMappings implements NamespaceResolver {

        // the global persistent namespace registry
        private NamespaceRegistryImpl nsReg;

        // local prefix/namespace mappings
        private HashMap prefixToURI = new HashMap();
        private HashMap uriToPrefix = new HashMap();

        // prefixes in global namespace registry hidden by local mappings
        private Set hiddenPrefixes = new HashSet();

        TransientNamespaceMappings(NamespaceRegistryImpl nsReg) {
            this.nsReg = nsReg;
        }

        void setNamespacePrefix(String prefix, String uri)
                throws NamespaceException, RepositoryException {
            if (prefix == null || uri == null) {
                throw new IllegalArgumentException("prefix/uri can not be null");
            }
            if (NamespaceRegistryImpl.NS_EMPTY_PREFIX.equals(prefix)
                    || NamespaceRegistryImpl.NS_DEFAULT_URI.equals(uri)) {
                throw new NamespaceException("default namespace is reserved and can not be changed");
            }
            // check if namespace exists (the following call will
            // trigger a NamespaceException if it doesn't)
            String globalPrefix = nsReg.getPrefix(uri);

            // check new prefix for collision
            String globalURI = null;
            try {
                globalURI = nsReg.getURI(prefix);
            } catch (NamespaceException nse) {
                // ignore
            }
            if (globalURI != null) {
                // prefix is already mapped in global namespace registry;
                // check if it refers to a namespace that has been locally
                // remapped, thus hiding it
                if (!hiddenPrefixes.contains(prefix)) {
                    // we don't allow to hide a namespace because we can't
                    // guarantee that there are no references to it
                    // (in names of nodes/properties/node types etc.)
                    throw new NamespaceException(prefix + ": prefix is already mapped to the namespace: " + globalURI);
                }
            }

            // check if namespace is already locally mapped
            String oldPrefix = (String) uriToPrefix.get(uri);
            if (oldPrefix != null) {
                // resurrect hidden global prefix
                hiddenPrefixes.remove(nsReg.getPrefix(uri));
                // remove old mapping
                uriToPrefix.remove(uri);
                prefixToURI.remove(oldPrefix);
            }

            // check if prefix is already locally mapped
            String oldURI = (String) prefixToURI.get(prefix);
            if (oldURI != null) {
                // resurrect hidden global prefix
                hiddenPrefixes.remove(nsReg.getPrefix(oldURI));
                // remove old mapping
                uriToPrefix.remove(oldURI);
                prefixToURI.remove(prefix);
            }

            if (!prefix.equals(globalPrefix)) {
                // store new mapping
                prefixToURI.put(prefix, uri);
                uriToPrefix.put(uri, prefix);
                hiddenPrefixes.add(globalPrefix);
            }
        }

        String[] getPrefixes() {
            if (prefixToURI.isEmpty()) {
                // shortcut
                return nsReg.getPrefixes();
            }

            HashSet prefixes = new HashSet();
            // global prefixes
            String[] globalPrefixes = nsReg.getPrefixes();
            for (int i = 0; i < globalPrefixes.length; i++) {
                if (!hiddenPrefixes.contains(globalPrefixes[i])) {
                    prefixes.add(globalPrefixes[i]);
                }
            }
            // local prefixes
            prefixes.addAll(prefixToURI.keySet());

            return (String[]) prefixes.toArray(new String[prefixes.size()]);
        }

        //------------------------------------------------< NamespaceResolver >
        /**
         * @see NamespaceResolver#getURI
         */
        public String getURI(String prefix) throws NamespaceException {
            if (prefixToURI.isEmpty()) {
                // shortcut
                return nsReg.getURI(prefix);
            }
            // check local mappings
            if (prefixToURI.containsKey(prefix)) {
                return (String) prefixToURI.get(prefix);
            }

            // check global mappings
            if (!hiddenPrefixes.contains(prefix)) {
                return nsReg.getURI(prefix);
            }

            throw new NamespaceException(prefix + ": unknown prefix");
        }

        /**
         * @see NamespaceResolver#getPrefix
         */
        public String getPrefix(String uri) throws NamespaceException {
            if (prefixToURI.isEmpty()) {
                // shortcut
                return nsReg.getPrefix(uri);
            }

            // check local mappings
            if (uriToPrefix.containsKey(uri)) {
                return (String) uriToPrefix.get(uri);
            }

            // check global mappings
            return nsReg.getPrefix(uri);
        }
    }
}
