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
package org.apache.jackrabbit.core.state.orm.hibernate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.jcr.PropertyType;

import org.apache.jackrabbit.core.BLOBFileValue;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.state.AbstractPersistenceManager;
import org.apache.jackrabbit.core.state.ItemState;
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
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.ObjectNotFoundException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;
import net.sf.hibernate.cfg.Configuration;
import net.sf.hibernate.type.Type;

/**
 * Hibernate implementation of a Jackrabbit persistence manager.
 */
public class HibernatePersistenceManager extends AbstractPersistenceManager {

    private static Logger log = Logger.getLogger(HibernatePersistenceManager.class);

    private SessionFactory sessionFactory;

    public HibernatePersistenceManager() {
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#init
     */
    public void init(PMContext context) throws Exception {
        try {
            // Create the SessionFactory
            sessionFactory = new Configuration().configure().
                buildSessionFactory();
        } catch (Throwable ex) {
            log.error("Initial SessionFactory creation failed.", ex);
            throw new ExceptionInInitializerError(ex);
        }
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

        NodeState state = null;
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            List nodeList = session.find(
                "from org.apache.jackrabbit.core.state.orm.hibernate.HibernateNodeState as node WHERE " +
                "node.uuid = ?",
                new Object[] {nodeId.getUUID()},
                new Type[] {Hibernate.STRING});

            tx.commit();
            if (nodeList.size() != 1) {
                throw new NoSuchItemStateException("Couldn't find unique node " +
                    nodeId.getUUID() + ", found " +
                    nodeList.size() +
                    " results instead");
            }
            HibernateNodeState result = (HibernateNodeState) nodeList.get(0);
            state = createNew(nodeId);
            result.toPersistentNodeState(state);
        } catch (HibernateException he) {
            try {
                if (tx != null)
                    tx.rollback();
            } catch (HibernateException he2) {
                log.error("Error while rolling back transaction", he2);
            }
            throw new ItemStateException("Error loading " + nodeId.getUUID(),
                                         he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
        return state;
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#load(PropertyId)
     */
    public PropertyState load(PropertyId propId) throws
        NoSuchItemStateException, ItemStateException {

        PropertyState state = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            ORMPropertyState propState = null;
            try {

                List propertyList = session.find(
                    "from org.apache.jackrabbit.core.state.orm.ORMPropertyState as prop WHERE " +
                    "prop.parentUUID = ? and prop.name = ?",
                    new Object[] {propId.getParentUUID(),
                    propId.getName().toString()},
                    new Type[] {Hibernate.STRING, Hibernate.STRING});

                tx.commit();
                if (propertyList.size() != 1) {
                    throw new NoSuchItemStateException(
                        "Couldn't find unique property " + propId + ", found " +
                        propertyList.size() + " results instead");
                }
                propState = (ORMPropertyState) propertyList.get(0);
                state = createNew(propId);
                propState.toPersistentPropertyState(state);
                if (propState.getType().intValue() == PropertyType.BINARY) {
                    // we must now load the binary values.
                    ArrayList internalValueList = new ArrayList();
                    List blobValueList = session.find(
                        "from org.apache.jackrabbit.core.state.orm.ORMBlobValue as bv WHERE " +
                        "bv.parentUUID = ? and bv.propertyName = ?",
                        new Object[] {propId.getParentUUID(),
                        propId.getName().toString()},
                        new Type[] {Hibernate.STRING, Hibernate.STRING});

                    Iterator resultIter = blobValueList.iterator();
                    while (resultIter.hasNext()) {
                        ORMBlobValue ormBlobValue = (ORMBlobValue) resultIter.
                            next();
                        ByteArrayInputStream in = new ByteArrayInputStream(
                            ormBlobValue.getBlobValue());
                        try {
                            BLOBFileValue blobValue = new BLOBFileValue(in);
                            internalValueList.add(blobValue);
                        } catch (Throwable t) {
                            throw new ItemStateException(
                                "Error while trying to load blob value", t);
                        }
                    }
                    state.setValues( (InternalValue[]) internalValueList.
                                    toArray(new
                                            InternalValue[internalValueList.
                                            size()]));
                }
            } catch (ObjectNotFoundException onfe) {
                throw new NoSuchItemStateException("Couldn't find " + propId,
                    onfe);
            }
        } catch (HibernateException he) {
            throw new ItemStateException("Error loading " + propId, he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
        return state;
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#load(NodeReferencesId)
     */
    public NodeReferences load(NodeReferencesId targetId) throws
        NoSuchItemStateException, ItemStateException {
        log.debug("Loading node references for targetId=" +
                  targetId.toString());
        NodeReferences refs = null;
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Iterator nodeRefIter = session.iterate("from org.apache.jackrabbit.core.state.orm.ORMNodeReference as nf where nf.targetId='" +
                targetId.toString() +
                "'");
            refs = new NodeReferences(targetId);
            while (nodeRefIter.hasNext()) {
                ORMNodeReference curNodeReference = (ORMNodeReference)
                    nodeRefIter.
                    next();
                refs.addReference(new PropertyId(curNodeReference.
                                                 getPropertyParentUUID(),
                                                 QName.
                                                 valueOf(curNodeReference.
                    getPropertyName())));
            }
            tx.commit();
        } catch (HibernateException he) {
            try {
                if (tx != null)
                    tx.rollback();
            } catch (HibernateException he2) {
                log.error("Error while rolling back transaction", he2);
            }
            log.error("Error while loading node reference for targetId=" +
                      targetId.toString(), he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
        return refs;
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#exists(NodeId)
     */
    public boolean exists(NodeId id) throws ItemStateException {
        HibernateNodeState result = null;
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            List nodeList = session.find(
                "from org.apache.jackrabbit.core.state.orm.hibernate.HibernateNodeState as node WHERE " +
                "node.uuid = ?",
                new Object[] {id.toString()},
                new Type[] {Hibernate.STRING});

            tx.commit();
            if (nodeList.size() < 1) {
                return false;
            } else {
                if (nodeList.size() > 1) {
                    log.warn("Node " + id +
                             " exists more than once in database !");
                }
                return true;
            }
        } catch (HibernateException he) {
            try {
                if (tx != null)
                    tx.rollback();
            } catch (HibernateException he2) {
                log.error("Error while rolling back transaction", he2);
            }
            throw new ItemStateException("Error loading " + id, he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#exists(PropertyId)
     */
    public boolean exists(PropertyId id) throws ItemStateException {
        boolean result = false;
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            ORMPropertyState propState = null;
            PropertyId propId = (PropertyId) id;
            List propertyList = session.find(
                "from org.apache.jackrabbit.core.state.orm.ORMPropertyState as prop WHERE " +
                "prop.parentUUID = ? and prop.name = ?",
                new Object[] {propId.getParentUUID(),
                propId.getName().toString()},
                new Type[] {Hibernate.STRING, Hibernate.STRING});

            tx.commit();
            if (propertyList.size() < 1) {
                return false;
            } else {
                if (propertyList.size() > 1) {
                    log.warn("Property " + id +
                             " exists more than once in database !");
                }
                return true;
            }
        } catch (HibernateException he) {
            try {
                if (tx != null)
                    tx.rollback();
            } catch (HibernateException he2) {
                log.error("Error while rolling back transaction", he2);
            }
            throw new ItemStateException("Error loading " + id, he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistenceManager#exists(NodeReferencesId)
     */
    public boolean exists(NodeReferencesId targetId) throws ItemStateException {
        boolean result = false;
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Iterator nodeRefIter = session.iterate("from org.apache.jackrabbit.core.state.orm.ORMNodeReference as nf where nf.targetId='" +
                targetId.toString() +
                "'");
            NodeReferences refs = new NodeReferences(targetId);
            if (nodeRefIter.hasNext()) {
                result = true;
            }
            tx.commit();
        } catch (HibernateException he) {
            try {
                if (tx != null)
                    tx.rollback();
            } catch (HibernateException he2) {
                log.error("Error while rolling back transaction", he2);
            }
            throw new ItemStateException(
                "Error while testing reference existence for targetId=" +
                targetId, he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
        return result;
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#store(NodeState)
     */
    public void store(NodeState state) throws ItemStateException {
        log.debug("Request to store " + state.getId());
        boolean isUpdate = true;
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            HibernateNodeState nodeState = new HibernateNodeState(state);
            if (state.getStatus() == ItemState.STATUS_NEW) {
                session.save(nodeState);
            } else {
                session.update(nodeState);
            }
            tx.commit();
        } catch (HibernateException he) {
            try {
                if (tx != null)
                    tx.rollback();
            } catch (HibernateException he2) {
                log.error("Error while rolling back transaction", he2);
            }
            throw new ItemStateException("Error saving " + state.getId(), he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#store(PropertyState)
     */
    public void store(PropertyState state) throws ItemStateException {
        log.debug("Request to store " + state.getId());
        boolean isUpdate = true;
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            ORMPropertyState propState = new ORMPropertyState(state);

            InternalValue[] values = state.getValues();
            if (values != null) {
                for (int i = 0; i < values.length; i++) {

                    // first we delete any existing blob values (this is faster
                    // than trying to load and update each value seperately)
                    session.delete("from org.apache.jackrabbit.core.state.orm.ORMBlobValue as bv where bv.parentUUID=? AND bv.propertyName=?",
                                   new Object[] {state.getParentUUID(),
                                   state.getName().toString()},
                                   new Type[] {Hibernate.STRING,
                                   Hibernate.STRING});

                    InternalValue val = values[i];
                    if (val != null) {
                        if (state.getType() == PropertyType.BINARY) {
                            ORMBlobValue ormBlobValue = null;
                            ormBlobValue = new ORMBlobValue();
                            ormBlobValue.setParentUUID(state.getParentUUID());
                            ormBlobValue.setPropertyName(state.getName().
                                toString());
                            ormBlobValue.setIndex(new Integer(i));
                            BLOBFileValue blobVal = (BLOBFileValue) val.
                                internalValue();
                            propState.setValues("");
                            ByteArrayOutputStream out = new
                                ByteArrayOutputStream();
                            try {
                                blobVal.spool(out);
                            } catch (Throwable t) {
                                tx.rollback();
                                session.close();
                                throw new ItemStateException(t.getMessage(), t);
                            }
                            ormBlobValue.setSize(new Long(blobVal.getLength()));
                            ormBlobValue.setBlobValue(out.toByteArray());
                            session.save(ormBlobValue);
                        }
                    }
                }
            }

            if (state.getStatus() == ItemState.STATUS_NEW) {
                session.save(propState);
            } else {
                session.update(propState);
            }
            tx.commit();
        } catch (HibernateException he) {
            try {
                if (tx != null)
                    tx.rollback();
            } catch (HibernateException he2) {
                log.error("Error while rolling back transaction", he2);
            }
            throw new ItemStateException("Error saving " + state.getId(), he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#store(NodeReferences)
     */
    public void store(NodeReferences refs) throws ItemStateException {
        Iterator nodeRefPropIdIter = refs.getReferences().iterator();
        log.debug("Request to store node references for targetId=" +
                  refs.getTargetId());
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            int i = 0;
            while (nodeRefPropIdIter.hasNext()) {
                PropertyId curPropertyId = (PropertyId) nodeRefPropIdIter.next();
                ORMNodeReference curNodeReference = new ORMNodeReference(refs.
                    getTargetId().toString(), curPropertyId.getParentUUID(),
                    curPropertyId.getName().toString());
                session.save(curNodeReference);
                i++;
                if (i % 20 == 0) {
                    session.flush();
                    session.clear();
                }
            }
            tx.commit();
        } catch (HibernateException he) {
            try {
                if (tx != null)
                    tx.rollback();
            } catch (HibernateException he2) {
                log.error("Error while rolling back transaction", he2);
            }
            throw new ItemStateException(
                "Error storing node references for targetId=" +
                refs.getTargetId(), he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#destroy(NodeState)
     */
    public void destroy(NodeState state) throws ItemStateException {
        log.debug("Deleting node " + state.getUUID());
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            HibernateNodeState nodeState = null;
            try {
                List nodeList = session.find(
                    "from org.apache.jackrabbit.core.state.orm.hibernate.HibernateNodeState as node WHERE " +
                    "node.uuid = ?",
                    new Object[] {state.getId().toString()},
                    new Type[] {Hibernate.STRING});

                if (nodeList.size() != 1) {
                } else {
                    nodeState = (HibernateNodeState) nodeList.get(0);
                    session.delete(nodeState);
                }
            } catch (ObjectNotFoundException onfe) {
            }
            tx.commit();
        } catch (HibernateException he) {
            try {
                if (tx != null)
                    tx.rollback();
            } catch (HibernateException he2) {
                log.error("Error while rolling back transaction", he2);
            }
            throw new ItemStateException("Error deleting " + state.getId(), he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#destroy(PropertyState)
     */
    public void destroy(PropertyState state) throws ItemStateException {
        log.debug("Deleting property " + state.getId());
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            ORMPropertyState propState = null;
            try {
                List propertyList = session.find(
                    "from org.apache.jackrabbit.core.state.orm.ORMPropertyState as prop WHERE " +
                    "prop.itemId = ?",
                    new Object[] {state.getId().toString()},
                    new Type[] {Hibernate.STRING});

                if (propertyList.size() != 1) {
                } else {
                    propState = (ORMPropertyState) propertyList.get(0);
                    session.delete(propState);
                    if (state.getType() == PropertyType.BINARY) {
                        session.delete("from org.apache.jackrabbit.core.state.orm.ORMBlobValue as bv where bv.parentUUID=? AND bv.propertyName=?",
                                       new Object[] {state.getParentUUID(),
                                       state.getName().toString()},
                                       new Type[] {Hibernate.STRING,
                                       Hibernate.STRING});
                    }
                }
            } catch (ObjectNotFoundException onfe) {
            }
            tx.commit();
        } catch (HibernateException he) {
            try {
                if (tx != null)
                    tx.rollback();
            } catch (HibernateException he2) {
                log.error("Error while rolling back transaction", he2);
            }
            throw new ItemStateException("Error deleting " + state.getId(), he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.AbstractPersistenceManager#destroy(NodeReferences)
     */
    public void destroy(NodeReferences refs) throws ItemStateException {
        log.debug("Deleting node refences for targetId=" +
                  refs.getTargetId().toString());
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            session.delete("from org.apache.jackrabbit.core.state.orm.ORMNodeReference as nf where nf.targetId='" +
                           refs.getTargetId().toString() +
                           "'");
            refs.clearAllReferences();
            tx.commit();
        } catch (HibernateException he) {
            try {
                if (tx != null)
                    tx.rollback();
            } catch (HibernateException he2) {
                log.error("Error while rolling back transaction", he2);
            }
            throw new ItemStateException(
                "Error deleting node references for targetId=" +
                refs.getTargetId().toString(), he);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (HibernateException he) {
                    throw new ItemStateException(
                        "Error while closing hibernate session", he);
                }
            }
        }
    }

}
