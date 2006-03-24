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
package org.apache.jackrabbit.core.state.orm.ojb;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PMContext;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.orm.ORMBlobValue;
import org.apache.jackrabbit.core.state.orm.ORMNodeReference;
import org.apache.jackrabbit.core.state.orm.ORMPropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.QName;
import org.apache.log4j.Logger;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.QueryByIdentity;

import javax.jcr.PropertyType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * OJB implementation of a Jackrabbit persistence manager.
 */
public class OJBPersistenceManager implements PersistenceManager
{

    private static Logger log = Logger.getLogger(OJBPersistenceManager.class);

    private boolean initialized = false;

    public OJBPersistenceManager()
    {
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#init
     */
    public void init(PMContext context) throws Exception
    {
        // FIXME: A config param to set the broker name would be handy
        if (initialized)
        {
            throw new IllegalStateException("already initialized");
        }
        initialized = true;
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#close
     */
    public void close() throws Exception
    {
        // Nothing to do
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#load(NodeId)
     */
    public NodeState load(NodeId nodeId) throws NoSuchItemStateException,
            ItemStateException
    {
        PersistenceBroker broker = PersistenceBrokerFactory
                .defaultPersistenceBroker();
        try
        {
            log.debug("Request for " + nodeId.getUUID());
            OJBNodeState nodeState = new OJBNodeState();
            nodeState.setUuid(nodeId.getUUID().toString());
            QueryByIdentity query = new QueryByIdentity(nodeState);
            OJBNodeState result = (OJBNodeState) broker.getObjectByQuery(query);
            if (result == null)
            {
                throw new NoSuchItemStateException(nodeId.getUUID().toString());
            }
            NodeState state = createNew(nodeId);
            result.toPersistentNodeState(state);
            return state;
        } catch (PersistenceBrokerException e)
        {
            throw new ItemStateException(e.getMessage(), e);
        } finally
        {
            if (broker != null)
                broker.close();
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#load(PropertyId)
     */
    public PropertyState load(PropertyId propId)
            throws NoSuchItemStateException, ItemStateException
    {
        PersistenceBroker broker = PersistenceBrokerFactory
                .defaultPersistenceBroker();
        try
        {
            log.debug("Request for property " + propId);
            ORMPropertyState propState = new ORMPropertyState(propId);
            QueryByIdentity query = new QueryByIdentity(propState);
            PropertyState state = createNew(propId);
            ORMPropertyState result = (ORMPropertyState) broker
                    .getObjectByQuery(query);
            if (result == null)
            {
                throw new NoSuchItemStateException("Couldn't find property "
                        + propId);
            }
            result.toPersistentPropertyState(state);
            if (result.getType().intValue() == PropertyType.BINARY)
            {
                // we must now load the binary values.
                ArrayList internalValueList = new ArrayList();
                Criteria criteria = new Criteria();
                criteria.addEqualTo("parentUUID", state.getParentId().toString());
                criteria.addEqualTo("propertyName", state.getName().toString());
                QueryByCriteria blobQuery = new QueryByCriteria(
                        ORMBlobValue.class, criteria);
                Iterator resultIter = broker.getCollectionByQuery(blobQuery)
                        .iterator();
                while (resultIter.hasNext())
                {
                    ORMBlobValue ormBlobValue = (ORMBlobValue) resultIter
                            .next();
                    ByteArrayInputStream in = new ByteArrayInputStream(
                            ormBlobValue.getBlobValue());
                    try
                    {
                        internalValueList.add(InternalValue.create(in));
                    } catch (Throwable t)
                    {
                        throw new ItemStateException(
                                "Error while trying to load blob value", t);
                    }
                }
                state.setValues((InternalValue[]) internalValueList
                        .toArray(new InternalValue[internalValueList.size()]));
            }
            return state;
        } catch (PersistenceBrokerException e)
        {
            throw new ItemStateException(e.getMessage(), e);
        } finally
        {
            if (broker != null)
                broker.close();
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#load(NodeReferencesId)
     */
    public NodeReferences load(NodeReferencesId targetId)
            throws NoSuchItemStateException, ItemStateException
    {
        PersistenceBroker broker = PersistenceBrokerFactory
                .defaultPersistenceBroker();
        try
        {
            ORMNodeReference nodeRef = new ORMNodeReference();
            nodeRef.setTargetId(targetId.toString());
            QueryByCriteria query = new QueryByCriteria(nodeRef);
            Iterator resultIter = broker.getCollectionByQuery(query).iterator();
            NodeReferences refs = new NodeReferences(targetId);
            while (resultIter.hasNext())
            {
                ORMNodeReference curNodeReference = (ORMNodeReference) resultIter
                        .next();
                refs.addReference(new PropertyId(NodeId.valueOf(curNodeReference
                        .getPropertyParentUUID()), QName
                        .valueOf(curNodeReference.getPropertyName())));
            }
            return refs;
        } catch (PersistenceBrokerException e)
        {
            throw new ItemStateException(e.getMessage(), e);
        } finally
        {
            if (broker != null)
                broker.close();
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#exists(NodeId)
     */
    public boolean exists(NodeId id) throws ItemStateException
    {
        PersistenceBroker broker = PersistenceBrokerFactory
                .defaultPersistenceBroker();
        try
        {
            OJBNodeState nodeState = new OJBNodeState(id);
            QueryByIdentity query = new QueryByIdentity(nodeState);
            OJBNodeState result = (OJBNodeState) broker.getObjectByQuery(query);
            if (result == null)
            {
                return false;
            } else
            {
                return true;
            }
        } catch (PersistenceBrokerException e)
        {
            throw new ItemStateException(e.getMessage(), e);
        } finally
        {
            if (broker != null)
                broker.close();
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#exists(PropertyId)
     */
    public boolean exists(PropertyId id) throws ItemStateException
    {
        PersistenceBroker broker = PersistenceBrokerFactory
                .defaultPersistenceBroker();
        try
        {
            ORMPropertyState propState = new ORMPropertyState(id);
            // QueryByCriteria query = new QueryByCriteria(propState);
            QueryByIdentity query = new QueryByIdentity(propState);
            ORMPropertyState result = (ORMPropertyState) broker
                    .getObjectByQuery(query);
            if (result == null)
            {
                return false;
            } else
            {
                return true;
            }
        } catch (PersistenceBrokerException e)
        {
            throw new ItemStateException(e.getMessage(), e);
        } finally
        {
            if (broker != null)
                broker.close();
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#exists(NodeReferencesId)
     */
    public boolean exists(NodeReferencesId targetId) throws ItemStateException
    {
        PersistenceBroker broker = PersistenceBrokerFactory
                .defaultPersistenceBroker();
        try
        {

            ORMNodeReference nodeRef = new ORMNodeReference();
            nodeRef.setTargetId(targetId.toString());
            QueryByCriteria query = new QueryByCriteria(nodeRef);
            Iterator resultIter = broker.getCollectionByQuery(query).iterator();
            NodeReferences refs = new NodeReferences(targetId);
            if (resultIter.hasNext())
            {
                return true;
            }
            return false;
        } catch (PersistenceBrokerException e)
        {
            throw new ItemStateException(e.getMessage(), e);
        } finally
        {
            if (broker != null)
                broker.close();
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#store(NodeState)
     */
    private void store(NodeState state, PersistenceBroker broker)
            throws ItemStateException
    {
        log.debug("Request to store node " + state.getId());
        OJBNodeState nodeState = new OJBNodeState(state.getId());
        QueryByIdentity query = new QueryByIdentity(nodeState);
        OJBNodeState result = (OJBNodeState) broker.getObjectByQuery(query);
        if (result == null)
        {
            result = new OJBNodeState();
        }
        result.fromPersistentNodeState(state);
        broker.store(result);
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#store(PropertyState)
     */
    private void store(PropertyState state, PersistenceBroker broker)
            throws ItemStateException
    {
        log.debug("Request to store property " + state.getId());
        ORMPropertyState propState = new ORMPropertyState(state.getId());
        QueryByIdentity query = new QueryByIdentity(propState);
        ORMPropertyState result = (ORMPropertyState) broker
                .getObjectByQuery(query);
        if (result == null)
        {
            result = new ORMPropertyState();
        }
        result.fromPersistentPropertyState(state);

        InternalValue[] values = state.getValues();
        if (values != null)
        {
            for (int i = 0; i < values.length; i++)
            {
                InternalValue val = values[i];
                if (val != null)
                {
                    if (state.getType() == PropertyType.BINARY)
                    {
                        Criteria criteria = new Criteria();
                        criteria
                                .addEqualTo("parentUUID", state.getParentId().getUUID().toString());
                        criteria.addEqualTo("propertyName", state.getName()
                                .toString());
                        criteria.addEqualTo("index", new Integer(i));
                        QueryByCriteria blobQuery = new QueryByCriteria(
                                ORMBlobValue.class, criteria);
                        Iterator resultIter = broker.getCollectionByQuery(
                                blobQuery).iterator();
                        ORMBlobValue ormBlobValue = null;
                        if (resultIter.hasNext())
                        {
                            ormBlobValue = (ORMBlobValue) resultIter.next();
                        } else
                        {
                            ormBlobValue = new ORMBlobValue();
                            ormBlobValue.setParentUUID(state.getParentId().getUUID().toString());
                            ormBlobValue.setPropertyName(state.getName()
                                    .toString());
                            ormBlobValue.setIndex(new Integer(i));
                        }
                        BLOBFileValue blobVal = (BLOBFileValue) val
                                .internalValue();
                        result.setValues("");
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        try
                        {
                            blobVal.spool(out);
                        } catch (Throwable t)
                        {
                            // The caller is responsible of aborting the
                            // transaction
                            // broker.abortTransaction();
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
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#store(NodeReferences)
     */
    private void store(NodeReferences refs, PersistenceBroker broker)
            throws ItemStateException
    {
        // destroy all the references before saving
        destroy(refs, broker);

        Iterator nodeRefPropIdIter = refs.getReferences().iterator();
        while (nodeRefPropIdIter.hasNext())
        {
            PropertyId curPropertyId = (PropertyId) nodeRefPropIdIter.next();
            ORMNodeReference curNodeReference = new ORMNodeReference(refs
                    .getTargetId().toString(), curPropertyId.getParentId().getUUID().toString(),
                    curPropertyId.getName().toString());
            broker.store(curNodeReference);
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#destroy(NodeState)
     */
    private void destroy(NodeState state, PersistenceBroker broker)
            throws ItemStateException
    {
        log.debug("Deleting node " + state.getId());

        // Destroy node
        OJBNodeState nodeState = new OJBNodeState(state.getId());
        QueryByIdentity query = new QueryByIdentity(nodeState);
        OJBNodeState result = (OJBNodeState) broker.getObjectByQuery(query);
        if (result == null)
        {
            result = new OJBNodeState();
        }
        broker.delete(result);
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#destroy(PropertyState)
     */
    private void destroy(PropertyState state, PersistenceBroker broker)
            throws ItemStateException
    {
        ORMPropertyState propState = new ORMPropertyState(state);
        broker.delete(propState);
        if (state.getType() == PropertyType.BINARY)
        {
            Criteria criteria = new Criteria();
            criteria.addEqualTo("parentUUID", state.getParentId().getUUID().toString());
            criteria.addEqualTo("propertyName", state.getName().toString());
            QueryByCriteria blobQuery = new QueryByCriteria(ORMBlobValue.class,
                    criteria);
            broker.deleteByQuery(blobQuery);
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#destroy(NodeReferences)
     */
    private void destroy(NodeReferences refs, PersistenceBroker broker)
            throws ItemStateException
    {
        ORMNodeReference nodeRef = new ORMNodeReference();
        nodeRef.setTargetId(refs.getTargetId().toString());
        QueryByCriteria query = new QueryByCriteria(nodeRef);
        Iterator resultIter = broker.getCollectionByQuery(query).iterator();
        while (resultIter.hasNext())
        {
            ORMNodeReference curNodeReference = (ORMNodeReference) resultIter
                    .next();
            broker.delete(curNodeReference);
        }
    }

    /**
     * @see PersistenceManager#createNew
     */
    public NodeState createNew(NodeId id)
    {
        return new NodeState(id, null, null, NodeState.STATUS_NEW, false);
    }

    /**
     * @see PersistenceManager#createNew
     */
    public PropertyState createNew(PropertyId id)
    {
        return new PropertyState(id, PropertyState.STATUS_NEW, false);
    }

    /**
     * @see PersistenceManager#store(ChangeLog)
     *
     * This method ensures that changes are either written completely to the
     * underlying persistence layer, or not at all.
     */
    public void store(ChangeLog changeLog) throws ItemStateException
    {
        PersistenceBroker broker = PersistenceBrokerFactory
                .defaultPersistenceBroker();
        try
        {
            broker.beginTransaction() ;
            Iterator iter = changeLog.deletedStates();
            while (iter.hasNext())
            {
                ItemState state = (ItemState) iter.next();
                if (state.isNode())
                {
                    destroy((NodeState) state, broker);
                } else
                {
                    destroy((PropertyState) state, broker);
                }
            }
            iter = changeLog.addedStates();
            while (iter.hasNext())
            {
                ItemState state = (ItemState) iter.next();
                if (state.isNode())
                {
                    store((NodeState) state, broker);
                } else
                {
                    store((PropertyState) state, broker);
                }
            }
            iter = changeLog.modifiedStates();
            while (iter.hasNext())
            {
                ItemState state = (ItemState) iter.next();
                if (state.isNode())
                {
                    store((NodeState) state, broker);
                } else
                {
                    store((PropertyState) state, broker);
                }
            }
            iter = changeLog.modifiedRefs();
            while (iter.hasNext())
            {
                NodeReferences refs = (NodeReferences) iter.next();
                if (refs.hasReferences())
                {
                    store(refs, broker);
                } else
                {
                    destroy(refs, broker);
                }
            }
            broker.commitTransaction() ;
        } catch (ItemStateException e)
        {
            if (broker != null)
                broker.abortTransaction();
            throw e;
        } catch (PersistenceBrokerException e)
        {
            if (broker != null)
                broker.abortTransaction();
            throw new ItemStateException("Unable to store", e);
        } finally
        {
            if (broker != null)
                broker.close();
        }

    }

}
