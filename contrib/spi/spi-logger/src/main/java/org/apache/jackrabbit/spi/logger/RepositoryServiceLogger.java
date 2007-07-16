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
package org.apache.jackrabbit.spi.logger;

import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;

import javax.jcr.RepositoryException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ValueFormatException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ItemExistsException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.MergeException;
import javax.jcr.NamespaceException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Map;
import java.util.Iterator;
import java.io.InputStream;
import java.io.Writer;
import java.io.IOException;

/**
 * <code>RepositoryServiceLogger</code> implements the repository service
 * interface and logs each call to a <code>Writer</code>.
 */
public class RepositoryServiceLogger implements RepositoryService {

    private final RepositoryService service;

    private final Writer log;

    public RepositoryServiceLogger(RepositoryService rs, Writer log) {
        this.service = rs;
        this.log = log;
    }

    public IdFactory getIdFactory() {
        try {
            return (IdFactory) execute(new Callable() {
                public Object call() {
                    return service.getIdFactory();
                }
            }, "getIdFactory()", new Object[]{});
        } catch (RepositoryException e) {
            throw new InternalError();
        }
    }

    public QValueFactory getQValueFactory() {
        try {
            return (QValueFactory) execute(new Callable() {
                public Object call() {
                    return service.getQValueFactory();
                }
            }, "getQValueFactory()", new Object[]{});
        } catch (RepositoryException e) {
            throw new InternalError();
        }
    }

    public Map getRepositoryDescriptors() throws RepositoryException {
        return (Map) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getRepositoryDescriptors();
            }
        }, "getRepositoryDescriptors()", new Object[]{});
    }

    public SessionInfo obtain(final Credentials credentials, final String s)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return (SessionInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.obtain(credentials, s);
            }
        }, "obtain(Credentials,String)", new Object[]{s});
    }

    public SessionInfo obtain(final SessionInfo sessionInfo, final String s)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return (SessionInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.obtain(sessionInfo, s);
            }
        }, "obtain(SessionInfo,String)", new Object[]{s});
    }

    public SessionInfo impersonate(final SessionInfo sessionInfo,
                                   final Credentials credentials)
            throws LoginException, RepositoryException {
        return (SessionInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.impersonate(sessionInfo, credentials);
            }
        }, "impersonate(SessionInfo,Credentials)", new Object[]{});
    }

    public void dispose(final SessionInfo sessionInfo) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.dispose(sessionInfo);
                return null;
            }
        }, "dispose(SessionInfo)", new Object[]{});
    }

    public String[] getWorkspaceNames(final SessionInfo sessionInfo)
            throws RepositoryException {
        return (String[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getWorkspaceNames(sessionInfo);
            }
        }, "getWorkspaceNames(SessionInfo)", new Object[]{});
    }

    public boolean isGranted(final SessionInfo sessionInfo,
                             final ItemId itemId,
                             final String[] strings) throws RepositoryException {
        return ((Boolean) execute(new Callable() {
            public Object call() throws RepositoryException {
                return new Boolean(service.isGranted(sessionInfo, itemId, strings));
            }
        }, "isGranted(SessionInfo,ItemId,String[])", new Object[]{itemId})).booleanValue();
    }

    public NodeId getRootId(final SessionInfo sessionInfo)
            throws RepositoryException {
        return (NodeId) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getRootId(sessionInfo);
            }
        }, "getRootId(SessionInfo)", new Object[]{});
    }

    public QNodeDefinition getNodeDefinition(final SessionInfo sessionInfo,
                                             final NodeId nodeId)
            throws RepositoryException {
        return (QNodeDefinition) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getNodeDefinition(sessionInfo, nodeId);
            }
        }, "getNodeDefinition(SessionInfo,NodeId)", new Object[]{nodeId});
    }

    public QPropertyDefinition getPropertyDefinition(final SessionInfo sessionInfo,
                                                     final PropertyId propertyId)
            throws RepositoryException {
        return (QPropertyDefinition) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getPropertyDefinition(sessionInfo, propertyId);
            }
        }, "getPropertyDefinition(SessionInfo,PropertyId)", new Object[]{propertyId});
    }

    public boolean exists(final SessionInfo sessionInfo, final ItemId itemId)
            throws RepositoryException {
        return ((Boolean) execute(new Callable() {
            public Object call() throws RepositoryException {
                return new Boolean(service.exists(sessionInfo, itemId));
            }
        }, "exists(SessionInfo,ItemId)", new Object[]{itemId})).booleanValue();
    }

    public NodeInfo getNodeInfo(final SessionInfo sessionInfo,
                                final NodeId nodeId)
            throws ItemNotFoundException, RepositoryException {
        return (NodeInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getNodeInfo(sessionInfo, nodeId);
            }
        }, "getNodeInfo(SessionInfo,NodeId)", new Object[]{nodeId});
    }

    public Iterator getItemInfos(final SessionInfo sessionInfo,
                                 final NodeId nodeId)
            throws ItemNotFoundException, RepositoryException {
        return (Iterator) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getItemInfos(sessionInfo, nodeId);
            }
        }, "getItemInfos(SessionInfo,NodeId)", new Object[]{nodeId});
    }

    public Iterator getChildInfos(final SessionInfo sessionInfo,
                                  final NodeId nodeId)
            throws ItemNotFoundException, RepositoryException {
        return (Iterator) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getChildInfos(sessionInfo, nodeId);
            }
        }, "getChildInfos(SessionInfo,NodeId)", new Object[]{nodeId});
    }

    public PropertyInfo getPropertyInfo(final SessionInfo sessionInfo,
                                        final PropertyId propertyId)
            throws ItemNotFoundException, RepositoryException {
        return (PropertyInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getPropertyInfo(sessionInfo, propertyId);
            }
        }, "getPropertyInfo(SessionInfo,PropertyId)", new Object[]{propertyId});
    }

    public Batch createBatch(final SessionInfo sessionInfo, final ItemId itemId)
            throws RepositoryException {
        return (Batch) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.createBatch(sessionInfo, itemId);
            }
        }, "createBatch(ItemId,SessionInfo)", new Object[]{itemId});
    }

    public void submit(final Batch batch) throws PathNotFoundException, ItemNotFoundException, NoSuchNodeTypeException, ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.submit(batch);
                return null;
            }
        }, "submit(Batch)", new Object[]{});
    }

    public void importXml(final SessionInfo sessionInfo,
                          final NodeId nodeId,
                          final InputStream inputStream,
                          final int i) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.importXml(sessionInfo, nodeId, inputStream, i);
                return null;
            }
        }, "importXml(SessionInfo,NodeId,InputStream,int)",
                new Object[]{nodeId});
    }

    public void move(final SessionInfo sessionInfo,
                     final NodeId nodeId,
                     final NodeId nodeId1,
                     final QName name) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.move(sessionInfo, nodeId, nodeId1, name);
                return null;
            }
        }, "move(SessionInfo,NodeId,NodeId,QName)",
                new Object[]{nodeId, nodeId1, name});
    }

    public void copy(final SessionInfo sessionInfo,
                     final String s,
                     final NodeId nodeId,
                     final NodeId nodeId1,
                     final QName name) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.copy(sessionInfo, s, nodeId, nodeId1, name);
                return null;
            }
        }, "copy(SessionInfo,String,NodeId,NodeId,QName)",
                new Object[]{s, nodeId, nodeId1, name});
    }

    public void update(final SessionInfo sessionInfo,
                       final NodeId nodeId,
                       final String s)
            throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.update(sessionInfo, nodeId, s);
                return null;
            }
        }, "update(SessionInfo,NodeId,String)", new Object[]{nodeId, s});
    }

    public void clone(final SessionInfo sessionInfo,
                      final String s,
                      final NodeId nodeId,
                      final NodeId nodeId1,
                      final QName name,
                      final boolean b) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.clone(sessionInfo, s, nodeId, nodeId1, name, b);
                return null;
            }
        }, "clone(SessionInfo,String,NodeId,NodeId,QName,boolean)",
                new Object[]{s, nodeId, nodeId1, name, new Boolean(b)});
    }

    public LockInfo getLockInfo(final SessionInfo sessionInfo,
                                final NodeId nodeId)
            throws LockException, RepositoryException {
        return (LockInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getLockInfo(sessionInfo, nodeId);
            }
        }, "getLockInfo(SessionInfo,NodeId)", new Object[]{nodeId});
    }

    public LockInfo lock(final SessionInfo sessionInfo,
                         final NodeId nodeId,
                         final boolean b,
                         final boolean b1)
            throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        return (LockInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.lock(sessionInfo, nodeId, b, b1);
            }
        }, "lock(SessionInfo,NodeId,boolean,boolean)",
                new Object[]{nodeId, new Boolean(b), new Boolean(b1)});
    }

    public void refreshLock(final SessionInfo sessionInfo, final NodeId nodeId)
            throws LockException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.refreshLock(sessionInfo, nodeId);
                return null;
            }
        }, "refreshLock(SessionInfo,NodeId)", new Object[]{nodeId});
    }

    public void unlock(final SessionInfo sessionInfo, final NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.unlock(sessionInfo, nodeId);
                return null;
            }
        }, "unlock(SessionInfo,NodeId)", new Object[]{nodeId});
    }

    public void checkin(final SessionInfo sessionInfo, final NodeId nodeId)
            throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.checkin(sessionInfo, nodeId);
                return null;
            }
        }, "checkin(SessionInfo,NodeId)", new Object[]{nodeId});
    }

    public void checkout(final SessionInfo sessionInfo, final NodeId nodeId)
            throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.checkout(sessionInfo, nodeId);
                return null;
            }
        }, "checkout(SessionInfo,NodeId)", new Object[]{nodeId});
    }

    public void removeVersion(final SessionInfo sessionInfo,
                              final NodeId nodeId,
                              final NodeId nodeId1)
            throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.removeVersion(sessionInfo, nodeId, nodeId1);
                return null;
            }
        }, "removeVersion(SessionInfo,NodeId,NodeId)",
                new Object[]{nodeId, nodeId1});
    }

    public void restore(final SessionInfo sessionInfo,
                        final NodeId nodeId,
                        final NodeId nodeId1,
                        final boolean b) throws VersionException, PathNotFoundException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.restore(sessionInfo, nodeId, nodeId1, b);
                return null;
            }
        }, "restore(SessionInfo,NodeId,NodeId,boolean)",
                new Object[]{nodeId, nodeId1, new Boolean(b)});
    }

    public void restore(final SessionInfo sessionInfo,
                        final NodeId[] nodeIds,
                        final boolean b)
            throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.restore(sessionInfo, nodeIds, b);
                return null;
            }
        }, "restore(SessionInfo,NodeId[],boolean)",
                new Object[]{nodeIds, new Boolean(b)});
    }

    public Iterator merge(final SessionInfo sessionInfo,
                          final NodeId nodeId,
                          final String s,
                          final boolean b) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        return (Iterator) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.merge(sessionInfo, nodeId, s, b);
            }
        }, "merge(SessionInfo,NodeId,String,boolean)",
                new Object[]{nodeId, s, new Boolean(b)});
    }

    public void resolveMergeConflict(final SessionInfo sessionInfo,
                                     final NodeId nodeId,
                                     final NodeId[] nodeIds,
                                     final NodeId[] nodeIds1)
            throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.resolveMergeConflict(sessionInfo, nodeId, nodeIds, nodeIds1);
                return null;
            }
        }, "resolveMergeConflict(SessionInfo,NodeId,NodeId[],NodeId[])",
                new Object[]{nodeId, nodeIds, nodeIds1});
    }

    public void addVersionLabel(final SessionInfo sessionInfo,
                                final NodeId nodeId,
                                final NodeId nodeId1,
                                final QName name,
                                final boolean b) throws VersionException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.addVersionLabel(sessionInfo, nodeId, nodeId1, name, b);
                return null;
            }
        }, "addVersionLabel(SessionInfo,NodeId,NodeId,QName,boolean)",
                new Object[]{nodeId, nodeId1, name, new Boolean(b)});
    }

    public void removeVersionLabel(final SessionInfo sessionInfo,
                                   final NodeId nodeId,
                                   final NodeId nodeId1,
                                   final QName name) throws VersionException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.removeVersionLabel(sessionInfo, nodeId, nodeId1, name);
                return null;
            }
        }, "removeVersionLabel(SessionInfo,NodeId,NodeId,QName)",
                new Object[]{nodeId, nodeId1, name});
    }

    public String[] getSupportedQueryLanguages(final SessionInfo sessionInfo)
            throws RepositoryException {
        return (String[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getSupportedQueryLanguages(sessionInfo);
            }
        }, "getSupportedQueryLanguages(SessionInfo)", new Object[]{});
    }

    public void checkQueryStatement(final SessionInfo sessionInfo,
                                    final String s,
                                    final String s1,
                                    final Map map) throws InvalidQueryException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.checkQueryStatement(sessionInfo, s, s1, map);
                return null;
            }
        }, "checkQueryStatement(SessionInfo,String,String,Map)",
                new Object[]{s, s1});
    }

    public QueryInfo executeQuery(final SessionInfo sessionInfo,
                                  final String s,
                                  final String s1,
                                  final Map map) throws RepositoryException {
        return (QueryInfo) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.executeQuery(sessionInfo, s, s1, map);
            }
        }, "executeQuery(SessionInfo,String,String,Map)",
                new Object[]{s, s1});
    }

    public EventFilter createEventFilter(final SessionInfo sessionInfo,
                                         final int i,
                                         final Path path,
                                         final boolean b,
                                         final String[] strings,
                                         final QName[] qNames,
                                         final boolean b1)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return (EventFilter) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.createEventFilter(sessionInfo, i, path, b, strings, qNames, b1);
            }
        }, "createEventFilter(SessionInfo,int,Path,boolean,String[],QName[],boolean)",
                new Object[]{new Integer(i), path, new Boolean(b), strings, qNames, new Boolean(b1)});
    }

    public EventBundle[] getEvents(final SessionInfo sessionInfo,
                                   final long l,
                                   final EventFilter[] eventFilters)
            throws RepositoryException, UnsupportedRepositoryOperationException, InterruptedException {
        final InterruptedException[] ex = new InterruptedException[1];
        EventBundle[] bundles = (EventBundle[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                try {
                    return service.getEvents(sessionInfo, l, eventFilters);
                } catch (InterruptedException e) {
                    ex[0] = e;
                    return null;
                }
            }
        }, "getEvents(SessionInfo,long,EventFilter[])", new Object[]{new Long(l)});
        if (ex[0] != null) {
            throw ex[0];
        }
        return bundles;
    }

    public Map getRegisteredNamespaces(final SessionInfo sessionInfo)
            throws RepositoryException {
        return (Map) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getRegisteredNamespaces(sessionInfo);
            }
        }, "getRegisteredNamespaces(SessionInfo)", new Object[]{});
    }

    public String getNamespaceURI(final SessionInfo sessionInfo,
                                  final String s)
            throws NamespaceException, RepositoryException {
        return (String) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getNamespaceURI(sessionInfo, s);
            }
        }, "getNamespaceURI(SessionInfo,String)", new Object[]{s});
    }

    public String getNamespacePrefix(final SessionInfo sessionInfo,
                                     final String s)
            throws NamespaceException, RepositoryException {
        return (String) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getNamespacePrefix(sessionInfo, s);
            }
        }, "getNamespacePrefix(SessionInfo,String)", new Object[]{s});
    }

    public void registerNamespace(final SessionInfo sessionInfo,
                                  final String s,
                                  final String s1) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.registerNamespace(sessionInfo, s, s1);
                return null;
            }
        }, "registerNamespace(SessionInfo,String,String)", new Object[]{s, s1});
    }

    public void unregisterNamespace(final SessionInfo sessionInfo,
                                    final String s)
            throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                service.unregisterNamespace(sessionInfo, s);
                return null;
            }
        }, "unregisterNamespace(SessionInfo,String)", new Object[]{s});
    }

    public Iterator getQNodeTypeDefinitions(final SessionInfo sessionInfo)
            throws RepositoryException {
        return (Iterator) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getQNodeTypeDefinitions(sessionInfo);
            }
        }, "getQNodeTypeDefinitions(SessionInfo)", new Object[]{});
    }

    public Iterator getQNodeTypeDefinitions(
            final SessionInfo sessionInfo,final QName[] ntNames)
            throws RepositoryException {
        return (Iterator) execute(new Callable() {
            public Object call() throws RepositoryException {
                return service.getQNodeTypeDefinitions(sessionInfo, ntNames);
            }
        }, "getQNodeTypeDefinitions(SessionInfo,QName[])", new Object[]{ntNames});
    }

    private Object execute(Callable callable, String methodName, Object[] args)
            throws RepositoryException {
        boolean success = false;
        long time = System.nanoTime();
        try {
            Object obj = callable.call();
            success = true;
            return obj;
        } finally {
            time = System.nanoTime() - time;
            try {
                StringBuffer b = new StringBuffer();
                b.append(String.valueOf(System.currentTimeMillis()));
                b.append("|").append(methodName);
                b.append("|").append(time).append("|");
                if (success) {
                    b.append("succeeded");
                } else {
                    b.append("failed");
                }
                b.append("|");
                String separator = "";
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Object[]) {
                        Object[] nested = (Object[]) args[i];
                        for (int j = 0; j < nested.length; j++) {
                            b.append(separator);
                            b.append(nested[j]);
                            separator = ",";
                        }
                    } else {
                        b.append(separator);
                        b.append(args[i]);
                    }
                    separator = ",";
                }
                b.append("\r\n");
                log.write(b.toString());
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public interface Callable {

        public Object call() throws RepositoryException;
    }
}
