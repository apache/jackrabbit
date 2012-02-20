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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.version.Version;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.RemoteLock;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteNodeDefinition;
import org.apache.jackrabbit.rmi.remote.RemoteNodeType;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.remote.RemoteVersion;
import org.apache.jackrabbit.rmi.remote.RemoteVersionHistory;

/**
 * Remote adapter for the JCR {@link javax.jcr.Node Node} interface.
 * This class makes a local node available as an RMI service using
 * the {@link org.apache.jackrabbit.rmi.remote.RemoteNode RemoteNode}
 * interface.
 *
 * @see javax.jcr.Node
 * @see org.apache.jackrabbit.rmi.remote.RemoteNode
 */
public class ServerNode extends ServerItem implements RemoteNode {

    /** The adapted local node. */
    private Node node;

    /**
     * Creates a remote adapter for the given local node.
     *
     * @param node local node
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerNode(Node node, RemoteAdapterFactory factory)
            throws RemoteException {
        super(node, factory);
        this.node = node;
    }

    /** {@inheritDoc} */
    public RemoteNode addNode(String path)
            throws RepositoryException, RemoteException {
        try {
            return getRemoteNode(node.addNode(path));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNode addNode(String path, String type)
            throws RepositoryException, RemoteException {
        try {
            return getRemoteNode(node.addNode(path, type));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty getProperty(String path)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteProperty(node.getProperty(path));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator getProperties()
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemotePropertyIterator(node.getProperties());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteItem getPrimaryItem()
            throws RepositoryException, RemoteException {
        try {
            return getRemoteItem(node.getPrimaryItem());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator getProperties(String pattern)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemotePropertyIterator(node.getProperties(pattern));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator getProperties(String[] globs)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemotePropertyIterator(node.getProperties(globs));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator getReferences()
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemotePropertyIterator(node.getReferences());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator getReferences(String name)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemotePropertyIterator(node.getReferences(name));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getIdentifier() throws RepositoryException, RemoteException {
        try {
            return node.getIdentifier();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    public String getUUID() throws RepositoryException, RemoteException {
        try {
            return node.getUUID();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasNodes() throws RepositoryException, RemoteException {
        try {
            return node.hasNodes();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasProperties() throws RepositoryException, RemoteException {
        try {
            return node.hasProperties();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasProperty(String path)
            throws RepositoryException, RemoteException {
        try {
            return node.hasProperty(path);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNodeType[] getMixinNodeTypes()
            throws RepositoryException, RemoteException {
        try {
            return getRemoteNodeTypeArray(node.getMixinNodeTypes());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNodeType getPrimaryNodeType()
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNodeType(node.getPrimaryNodeType());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isNodeType(String type)
            throws RepositoryException, RemoteException {
        try {
            return node.isNodeType(type);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator getNodes() throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNodeIterator(node.getNodes());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator getNodes(String pattern)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNodeIterator(node.getNodes(pattern));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator getNodes(String[] globs)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNodeIterator(node.getNodes(globs));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNode getNode(String path)
            throws RepositoryException, RemoteException {
        try {
            return getRemoteNode(node.getNode(path));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasNode(String path)
            throws RepositoryException, RemoteException {
        try {
            return node.hasNode(path);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty setProperty(String name, Value value)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteProperty(node.setProperty(name, value));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty setProperty(String name, Value value, int type)
            throws RepositoryException, RemoteException {
        try {
            Property property = node.setProperty(name, value, type);
            if (property == null) {
                return null;
            } else {
                return getFactory().getRemoteProperty(property);
            }
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void addMixin(String name)
            throws RepositoryException, RemoteException {
        try {
            node.addMixin(name);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean canAddMixin(String name)
            throws RepositoryException, RemoteException {
        try {
            return node.canAddMixin(name);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeMixin(String name)
            throws RepositoryException, RemoteException {
        try {
            node.removeMixin(name);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void orderBefore(String src, String dst)
            throws RepositoryException, RemoteException {
        try {
            node.orderBefore(src, dst);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty setProperty(String name, Value[] values)
            throws RepositoryException, RemoteException {
        try {
            Property property = node.setProperty(name, values);
            if (property == null) {
                return null;
            } else {
                return getFactory().getRemoteProperty(property);
            }
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNodeDefinition getDefinition()
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNodeDefinition(node.getDefinition());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteVersion checkin() throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteVersion(node.checkin());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void checkout() throws RepositoryException, RemoteException {
        try {
            node.checkout();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getCorrespondingNodePath(String workspace)
            throws RepositoryException, RemoteException {
        try {
            return node.getCorrespondingNodePath(workspace);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public int getIndex() throws RepositoryException, RemoteException {
        try {
            return node.getIndex();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator merge(String workspace, boolean bestEffort)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNodeIterator(node.merge(workspace, bestEffort));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void cancelMerge(String versionUUID)
            throws RepositoryException, RemoteException {
        try {
            node.cancelMerge(getVersionByUUID(versionUUID));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void doneMerge(String versionUUID)
            throws RepositoryException, RemoteException {
        try {
            node.doneMerge(getVersionByUUID(versionUUID));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restore(String version, boolean removeExisting)
            throws RepositoryException, RemoteException {
        try {
            node.restore(version, removeExisting);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restoreByUUID(String versionUUID, boolean removeExisting)
            throws RepositoryException, RemoteException {
        try {
            node.restore(getVersionByUUID(versionUUID), removeExisting);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restore(String versionUUID, String path, boolean removeExisting)
            throws RepositoryException, RemoteException {
        try {
            node.restore(getVersionByUUID(versionUUID), path, removeExisting);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restoreByLabel(String label, boolean removeExisting)
            throws RepositoryException, RemoteException {
        try {
            node.restoreByLabel(label, removeExisting);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void update(String workspace)
            throws RepositoryException, RemoteException {
        try {
            node.update(workspace);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean holdsLock() throws RepositoryException, RemoteException {
        try {
            return node.holdsLock();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isCheckedOut() throws RepositoryException, RemoteException {
        try {
            return node.isCheckedOut();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteVersionHistory getVersionHistory()
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteVersionHistory(node.getVersionHistory());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteVersion getBaseVersion()
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteVersion(node.getBaseVersion());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isLocked() throws RepositoryException, RemoteException {
        try {
            return node.isLocked();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty setProperty(String name, Value[] values, int type)
            throws RepositoryException, RemoteException {
        try {
            Property property = node.setProperty(name, values, type);
            return getFactory().getRemoteProperty(property);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void unlock() throws RepositoryException, RemoteException {
        try {
            node.unlock();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteLock getLock() throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteLock(node.getLock());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteLock lock(boolean isDeep, boolean isSessionScoped)
            throws RepositoryException, RemoteException {
        try {
            Lock lock = node.lock(isDeep, isSessionScoped);
            return getFactory().getRemoteLock(lock);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator getSharedSet() 
    		throws RepositoryException, RemoteException {
    	try {
    		NodeIterator sharedSet = node.getSharedSet();
    		return getFactory().getRemoteNodeIterator(sharedSet);
    	} catch (RepositoryException ex) {
    		throw getRepositoryException(ex);
    	}
    }

    /** {@inheritDoc} */
    public void followLifecycleTransition(String transition)
    		throws RepositoryException, RemoteException {
    	try {
    		node.followLifecycleTransition(transition);
    	} catch (RepositoryException ex) {
    		throw getRepositoryException(ex);
    	}
    }

    /** {@inheritDoc} */
	public String[] getAllowedLifecycleTransistions()
			throws RepositoryException, RemoteException {
    	try {
    		return node.getAllowedLifecycleTransistions();
    	} catch (RepositoryException ex) {
    		throw getRepositoryException(ex);
    	}
	}

    /** {@inheritDoc} */
	public RemoteIterator getWeakReferences() 
			throws RepositoryException, RemoteException {
    	try {
    		return getFactory().getRemotePropertyIterator(node.getWeakReferences());
    	} catch (RepositoryException ex) {
    		throw getRepositoryException(ex);
    	}
	}

    /** {@inheritDoc} */
	public RemoteIterator getWeakReferences(String name)
			throws RepositoryException, RemoteException {
    	try {
    		return getFactory().getRemotePropertyIterator(node.getWeakReferences(name));
    	} catch (RepositoryException ex) {
    		throw getRepositoryException(ex);
    	}
	}

    /** {@inheritDoc} */
	public void removeShare() throws RepositoryException, RemoteException {
    	try {
    		node.removeShare();
    	} catch (RepositoryException ex) {
    		throw getRepositoryException(ex);
    	}
	}

    /** {@inheritDoc} */
	public void removeSharedSet() throws RepositoryException, RemoteException {
    	try {
    		node.removeSharedSet();
    	} catch (RepositoryException ex) {
    		throw getRepositoryException(ex);
    	}
	}

    /** {@inheritDoc} */
	public void setPrimaryType(String nodeTypeName) 
			throws RepositoryException, RemoteException {
    	try {
    		node.setPrimaryType(nodeTypeName);
    	} catch (RepositoryException ex) {
    		throw getRepositoryException(ex);
    	}
	}

    //---------- Implementation helper -----------------------------------------

    /**
     * Returns the {@link Version} instance for the given UUID.
     *
     * @param versionUUID The UUID of the version.
     *
     * @return The version node.
     *
     * @throws RepositoryException if an error occurrs accessing the version
     *      node or if the UUID does not denote a version.
     */
    protected Version getVersionByUUID(String versionUUID)
            throws RepositoryException {

        // get the version node by its UUID from the version history's session
        Session session = node.getSession();
        Node versionNode = session.getNodeByUUID(versionUUID);

        // check whether the node is a session, which it should be according
        // to the spec (methods returning nodes should automatically return
        // the correct type).
        if (versionNode instanceof Version) {
            return (Version) versionNode;
        }

        // otherwise fail
        throw new RepositoryException("Cannot find version " + versionUUID);
    }

}
