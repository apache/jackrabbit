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
package org.apache.jackrabbit.core.state.orm.ojb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.jcr.PropertyType;

import org.apache.jackrabbit.core.BLOBFileValue;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.state.AbstractPersistenceManager;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PMContext;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.orm.ORMBlobValue;
import org.apache.jackrabbit.core.state.orm.ORMNodeReference;
import org.apache.jackrabbit.core.state.orm.ORMPropertyState;
import org.apache.log4j.Logger;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryByIdentity;

/**
 * OJB implementation of a Jackrabbit persistence manager.
 */
public class OJBPersistenceManager extends AbstractPersistenceManager {

    private static Logger log = Logger.getLogger(OJBPersistenceManager.class);

    private PersistenceBroker broker = null;
    private boolean initialized = false;

    public OJBPersistenceManager() {
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#init
     */
    public void init(PMContext context) throws Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        broker = PersistenceBrokerFactory.defaultPersistenceBroker();
        initialized = true;
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#close
     */
    public void close() throws Exception {
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#load(NodeId)
     */
    public NodeState load(NodeId nodeId) throws NoSuchItemStateException,
        ItemStateException {
        log.debug("Request for " + nodeId.getUUID());
        OJBNodeState nodeState = new OJBNodeState();
        nodeState.setUuid(nodeId.getUUID());
        QueryByIdentity query = new QueryByIdentity(nodeState);
        OJBNodeState result = (OJBNodeState) broker.getObjectByQuery(query);
        if (result == null) {
            throw new NoSuchItemStateException(nodeId.getUUID());
        }
        NodeState state = createNew(nodeId);
        result.toPersistentNodeState(state);
        return state;
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#load(PropertyId)
     */
    public PropertyState load(PropertyId propId) throws
        NoSuchItemStateException, ItemStateException {
        log.debug("Request for property " + propId);
        ORMPropertyState propState = new ORMPropertyState(propId);
        QueryByIdentity query = new QueryByIdentity(propState);
        PropertyState state = createNew(propId);
        ORMPropertyState result = (ORMPropertyState) broker.getObjectByQuery(
            query);
        if (result == null) {
            throw new NoSuchItemStateException("Couldn't find property " +
                                               propId);
        }
        result.toPersistentPropertyState(state);
        if (result.getType().intValue() == PropertyType.BINARY) {
            // we must now load the binary values.
            ArrayList internalValueList = new ArrayList();
            Criteria criteria = new Criteria();
            criteria.addEqualTo("parentUUID", state.getParentUUID());
            criteria.addEqualTo("propertyName", state.getName().toString());
            QueryByCriteria blobQuery = new QueryByCriteria(ORMBlobValue.class,
                criteria);
            Iterator resultIter = broker.getCollectionByQuery(blobQuery).
                iterator();
            while (resultIter.hasNext()) {
                ORMBlobValue ormBlobValue = (ORMBlobValue) resultIter.next();
                ByteArrayInputStream in = new ByteArrayInputStream(ormBlobValue.
                    getBlobValue());
                try {
                    BLOBFileValue blobValue = new BLOBFileValue(in);
                    internalValueList.add(blobValue);
                } catch (Throwable t) {
                    throw new ItemStateException(
                        "Error while trying to load blob value", t);
                }
            }
            state.setValues( (InternalValue[]) internalValueList.toArray(new
                InternalValue[internalValueList.size()]));
        }
        return state;
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#load(NodeReferencesId)
     */
    public NodeReferences load(NodeReferencesId targetId) throws
        NoSuchItemStateException, ItemStateException {
        ORMNodeReference nodeRef = new ORMNodeReference();
        nodeRef.setTargetId(targetId.toString());
        QueryByCriteria query = new QueryByCriteria(nodeRef);
        Iterator resultIter = broker.getCollectionByQuery(query).iterator();
        NodeReferences refs = new NodeReferences(targetId);
        while (resultIter.hasNext()) {
            ORMNodeReference curNodeReference = (ORMNodeReference) resultIter.
                next();
            refs.addReference(new PropertyId(curNodeReference.
                                             getPropertyParentUUID(),
                                             QName.valueOf(curNodeReference.
                getPropertyName())));
        }
        return refs;
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#exists(NodeId)
     */
    public boolean exists(NodeId id) throws ItemStateException {
        OJBNodeState nodeState = new OJBNodeState(id);
        QueryByIdentity query = new QueryByIdentity(nodeState);
        OJBNodeState result = (OJBNodeState) broker.getObjectByQuery(query);
        if (result == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#exists(PropertyId)
     */
    public boolean exists(PropertyId id) throws ItemStateException {
        ORMPropertyState propState = new ORMPropertyState(id);
        // QueryByCriteria query = new QueryByCriteria(propState);
        QueryByIdentity query = new QueryByIdentity(propState);
        ORMPropertyState result = (ORMPropertyState) broker.
            getObjectByQuery(query);
        if (result == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#exists(NodeReferencesId)
     */
    public boolean exists(NodeReferencesId targetId)
        throws ItemStateException {
        ORMNodeReference nodeRef = new ORMNodeReference();
        nodeRef.setTargetId(targetId.toString());
        QueryByCriteria query = new QueryByCriteria(nodeRef);
        Iterator resultIter = broker.getCollectionByQuery(query).iterator();
        NodeReferences refs = new NodeReferences(targetId);
        if (resultIter.hasNext()) {
            return true;
        }
        return false;
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#store(NodeState)
     */
    public void store(NodeState state) throws ItemStateException {
        log.debug("Request to store node " + state.getId());
        broker.beginTransaction();
        OJBNodeState nodeState = new OJBNodeState(state.getId());
        QueryByIdentity query = new QueryByIdentity(nodeState);
        OJBNodeState result = (OJBNodeState) broker.getObjectByQuery(query);
        if (result == null) {
            result = new OJBNodeState();
        }
        result.fromPersistentNodeState(state);
        broker.store(result);
        broker.commitTransaction();
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#store(PropertyState)
     */
    public void store(PropertyState state) throws ItemStateException {
        log.debug("Request to store property " + state.getId());
        broker.beginTransaction();
        ORMPropertyState propState = new ORMPropertyState(state.getId());
        QueryByIdentity query = new QueryByIdentity(propState);
        ORMPropertyState result = (ORMPropertyState) broker.getObjectByQuery(
            query);
        if (result == null) {
            result = new ORMPropertyState();
        }
        result.fromPersistentPropertyState(state);

        InternalValue[] values = state.getValues();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                InternalValue val = values[i];
                if (val != null) {
                    if (state.getType() == PropertyType.BINARY) {
                        Criteria criteria = new Criteria();
                        criteria.addEqualTo("parentUUID", state.getParentUUID());
                        criteria.addEqualTo("propertyName",
                                            state.getName().toString());
                        criteria.addEqualTo("index", new Integer(i));
                        QueryByCriteria blobQuery = new QueryByCriteria(
                            ORMBlobValue.class, criteria);
                        Iterator resultIter = broker.getCollectionByQuery(
                            blobQuery).iterator();
                        ORMBlobValue ormBlobValue = null;
                        if (resultIter.hasNext()) {
                            ormBlobValue = (ORMBlobValue) resultIter.next();
                        } else {
                            ormBlobValue = new ORMBlobValue();
                            ormBlobValue.setParentUUID(state.getParentUUID());
                            ormBlobValue.setPropertyName(state.getName().
                                toString());
                            ormBlobValue.setIndex(new Integer(i));
                        }
                        BLOBFileValue blobVal = (BLOBFileValue) val.
                            internalValue();
                        result.setValues("");
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        try {
                            blobVal.spool(out);
                        } catch (Throwable t) {
                            broker.abortTransaction();
                            throw new ItemStateException(t.getMessage(), t);
                        }
                        ormBlobValue.setSize(new Long(blobVal.getLength()));
                        ormBlobValue.setBlobValue(out.toByteArray());
                        broker.store(ormBlobValue);
                    }
                }
            }
        }
        broker.store(result);
        broker.commitTransaction();
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#store(NodeReferences)
     */
    public void store(NodeReferences refs) throws ItemStateException {
        Iterator nodeRefPropIdIter = refs.getReferences().iterator();
        broker.beginTransaction();
        while (nodeRefPropIdIter.hasNext()) {
            PropertyId curPropertyId = (PropertyId) nodeRefPropIdIter.next();
            ORMNodeReference curNodeReference = new ORMNodeReference(refs.
                getTargetId().toString(), curPropertyId.getParentUUID(),
                curPropertyId.getName().toString());
            broker.store(curNodeReference);
        }
        broker.commitTransaction();
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#destroy(NodeState)
     */
    public void destroy(NodeState state) throws ItemStateException {
        log.debug("Deleting node " + state.getId());
        broker.beginTransaction();
        OJBNodeState nodeState = new OJBNodeState(state.getId());
        QueryByIdentity query = new QueryByIdentity(nodeState);
        OJBNodeState result = (OJBNodeState) broker.getObjectByQuery(query);
        if (result == null) {
            result = new OJBNodeState();
        }
        broker.delete(result);
        broker.commitTransaction();
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#destroy(PropertyState)
     */
    public void destroy(PropertyState state) throws
        ItemStateException {
        ORMPropertyState propState = new ORMPropertyState(state);
        broker.beginTransaction();
        broker.delete(propState);
        if (state.getType() == PropertyType.BINARY) {
            Criteria criteria = new Criteria();
            criteria.addEqualTo("parentUUID", state.getParentUUID());
            criteria.addEqualTo("propertyName", state.getName().toString());
            QueryByCriteria blobQuery = new QueryByCriteria(ORMBlobValue.class,
                criteria);
            broker.deleteByQuery(blobQuery);
        }
        broker.commitTransaction();
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#destroy(NodeReferences)
     */
    public void destroy(NodeReferences refs) throws ItemStateException {
        broker.beginTransaction();
        ORMNodeReference nodeRef = new ORMNodeReference();
        nodeRef.setTargetId(refs.getTargetId().toString());
        QueryByCriteria query = new QueryByCriteria(nodeRef);
        Iterator resultIter = broker.getCollectionByQuery(query).iterator();
        while (resultIter.hasNext()) {
            ORMNodeReference curNodeReference = (ORMNodeReference) resultIter.
                next();
            broker.delete(curNodeReference);
        }
        broker.commitTransaction();
    }


}
