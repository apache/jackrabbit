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
package org.apache.jackrabbit.core.state.tx;

import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.state.*;

import javax.jcr.PropertyType;
import java.io.*;
import java.util.*;

/**
 * Represents the transaction on the behalf of the item state manager. Manages
 * the transaction log and registration of items that are participating in a
 * transaction.
 */
class TransactionImpl implements Transaction, PlaybackListener {

    /**
     * Status constant
     */
    public static final int STATUS_NONE = 0;

    /**
     * Status constant
     */
    public static final int STATUS_PREPARE = 1;

    /**
     * Status constant
     */
    public static final int STATUS_COMMIT = 2;

    /**
     * Status constant
     */
    public static final int STATUS_ROLLBACK = 3;

    /**
     * Status constant
     */
    public static final int STATUS_ROLLBACK_ONLY = 4;

    /**
     * Parent directory
     */
    private final File directory;

    /**
     * Transactional store
     */
    private final TransactionalStore store;

    /**
     * Transaction log
     */
    private final TransactionLog txLog;

    /**
     * Transaction listeners
     */
    private final List listeners = new ArrayList();

    /**
     * Transaction attributes
     */
    private final Map attributes = new HashMap();

    /**
     * Update
     */
    private TransactionalStore.Update update;

    /**
     * Transaction state
     */
    private int txStatus;

    /**
     * Number of stored items so far
     */
    private int counter;

    /**
     * Create a new instance of this class. Used when beginnning global
     * transactions.
     *
     * @param directory directory to use for writing the transaction log and
     *                  objects
     * @param store     store to use when preparing and committing the transaction
     */
    public TransactionImpl(File directory, TransactionalStore store)
            throws TransactionException {

        this.directory = directory;
        this.store = store;

        directory.mkdirs();
        if (!directory.isDirectory()) {
            throw new TransactionException("Unable to create directory.");
        }
        try {
            txLog = new TransactionLog(new File(directory, "tx.log"));
        } catch (IOException e) {
            throw new TransactionException("Unable to create transaction log.", e);
        }
    }

    /**
     * Enlist a item state to this transaction. Saves the state of the item
     * to a persistent store and adds a record to the transaction log.
     *
     * @param state  item state
     * @param status new status
     * @throws TransactionException if an error occurs
     */
    public void enlist(ItemState state, int status) throws TransactionException {
        if (txStatus != STATUS_NONE) {
            throw new TransactionException("Transaction not in clean state.");
        }
        switch (status) {
            case ItemState.STATUS_NEW:
                try {
                    txLog.logCreated(store(state));
                } catch (IOException e) {
                    throw new TransactionException("Unable to store state.", e);
                }
                break;
            case ItemState.STATUS_EXISTING:
                try {
                    txLog.logUpdated(store(state));
                } catch (IOException e) {
                    throw new TransactionException("Unable to store state.", e);
                }
                break;
            case ItemState.STATUS_EXISTING_REMOVED:
                try {
                    txLog.logDeleted(store(state));
                } catch (IOException e) {
                    throw new TransactionException("Unable to store state.", e);
                }
                break;
        }
    }

    //-----------------------------------------------------------< Transaction >

    /**
     * @see Transaction#prepare
     */
    public void prepare() throws TransactionException {
        if (txStatus != STATUS_NONE) {
            throw new TransactionException("Transaction not in clean state.");
        }

        update = store.beginUpdate();

        txLog.playback(this);
        txLog.logPrepare();

        txStatus = STATUS_PREPARE;
    }

    /**
     * @see Transaction#commit
     */
    public void commit() throws TransactionException {
        if (txStatus != STATUS_PREPARE) {
            throw new TransactionException("Transaction not prepared.");
        }

        try {
            update.end();
        } catch (ItemStateException e) {
            throw new TransactionException("Unable to end update.", e);
        }

        txLog.logCommit();
        txStatus = STATUS_COMMIT;

        notifyCommitted();
    }

    /**
     * @see Transaction#rollback
     */
    public void rollback() throws TransactionException {
        if (txStatus != STATUS_NONE && txStatus != STATUS_PREPARE &&
                txStatus != STATUS_ROLLBACK_ONLY) {
            throw new TransactionException("Transaction neither clean nor prepared.");
        }
        txLog.logRollback();
        txStatus = STATUS_ROLLBACK;

        notifyRolledBack();
    }

    /**
     * @see Transaction#setRollbackOnly
     */
    public void setRollbackOnly() throws TransactionException {
        if (txStatus != STATUS_NONE || txStatus != STATUS_ROLLBACK_ONLY) {
            throw new TransactionException("Transaction not clean.");
        }
        txStatus = STATUS_ROLLBACK_ONLY;
    }

    /**
     * @see Transaction#setAttribute
     */
    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
        }
        attributes.put(name, value);
    }

    /**
     * @see Transaction#getAttribute
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * @see Transaction#removeAttribute
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    /**
     * Add a transaction listener
     *
     * @param listener listener to add
     */
    public void addListener(TransactionListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a transaction listener
     *
     * @param listener listener to remove
     */
    public void removeListener(TransactionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Notify listeners that transaction was committed. Since there is at most
     * one commit and one rollback event to be reported, the listeners can
     * safely be cleared at the same time.
     */
    private void notifyCommitted() {
        TransactionListener[] al;

        synchronized (listeners) {
            al = new TransactionListener[listeners.size()];
            listeners.toArray(al);
            listeners.clear();
        }

        for (int i = 0; i < al.length; i++) {
            al[i].transactionCommitted(this);
        }
    }

    /**
     * Notify listeners that transaction was rolled back. Since there is at most
     * one commit and one rollback event to be reported, the listeners can
     * safely be cleared at the same time.
     */
    private void notifyRolledBack() {
        TransactionListener[] al;

        synchronized (listeners) {
            al = new TransactionListener[listeners.size()];
            listeners.toArray(al);
            listeners.clear();
        }

        for (int i = 0; i < al.length; i++) {
            al[i].transactionRolledBack(this);
        }
    }

    //------------------------------------------------------< PlaybackListener >

    /**
     * @see PlaybackListener#elementCreated
     */
    public void elementCreated(String id) throws TransactionException {
        try {
            update.put(load(id));
        } catch (NoSuchItemStateException e) {
            throw new TransactionException("Unable to load item.", e);
        } catch (ItemStateException e) {
            throw new TransactionException("Unable to put item.", e);
        }
    }

    /**
     * @see PlaybackListener#elementUpdated
     */
    public void elementUpdated(String id) throws TransactionException {
        try {
            update.put(load(id));
        } catch (NoSuchItemStateException e) {
            throw new TransactionException("Unable to load item.", e);
        } catch (ItemStateException e) {
            throw new TransactionException("Unable to put item.", e);
        }
    }

    /**
     * @see PlaybackListener#elementDeleted
     */
    public void elementDeleted(String id) throws TransactionException {
        try {
            update.delete(load(id));
        } catch (NoSuchItemStateException e) {
            throw new TransactionException("Unable to load item.", e);
        } catch (ItemStateException e) {
            throw new TransactionException("Unable to delete item.", e);
        }
    }

    //---------------------------------------------------------------< Storage >

    /**
     * Load a state from the transaction-managed storage
     *
     * @param id element id
     * @throws NoSuchItemStateException if the item state does not exist
     * @throws ItemStateException       if loading the item state fails
     */
    public ItemState load(String id)
            throws NoSuchItemStateException, ItemStateException {

        if (id.startsWith("N")) {
            return loadNodeState(id);
        } else {
            return loadPropertyState(id);
        }
    }

    /**
     * Load a node state from the transaction-managed storage
     */
    private PersistentNodeState loadNodeState(String id)
            throws NoSuchItemStateException, ItemStateException {

        Properties p = new Properties();
        InputStream in = null;

        try {
            in = new BufferedInputStream(new FileInputStream(new File(directory, id)));
            p.load(in);

        } catch (IOException e) {
            throw new ItemStateException("Unable to load state from file.", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e2) {
                }
            }
        }

        PersistentNodeState state = new TransactionalNodeState(p.getProperty("uuid"), this);

        state.setParentUUID(p.getProperty("parentUUID"));
        state.setDefinitionId(NodeDefId.valueOf(p.getProperty("definitionId")));
        state.setNodeTypeName(QName.valueOf(p.getProperty("nodeTypeName")));

        state.setParentUUIDs(getList(p, "parentUUIDs"));
        state.setMixinTypeNames(getSet(p, "mixinTypeNames"));
        addPropertyEntries(state, p, "propertyEntries");
        addChildNodeEntries(state, p, "childNodeEntries");

        return state;
    }

    /**
     * Load a property state from the transaction-managed storage
     */
    private PersistentPropertyState loadPropertyState(String id)
            throws NoSuchItemStateException, ItemStateException {

        Properties p = new Properties();
        InputStream in = null;

        try {
            in = new BufferedInputStream(new FileInputStream(new File(directory, id)));
            p.load(in);

        } catch (IOException e) {
            throw new ItemStateException("Unable to load state from file.", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e2) {
                }
            }
        }

        PersistentPropertyState state = new TransactionalPropertyState(QName.valueOf(p.getProperty("name")),
                p.getProperty("parentUUID"), this);

        state.setType(PropertyType.valueFromName(p.getProperty("type")));
        state.setMultiValued(Boolean.getBoolean(p.getProperty("multiValued")));
        state.setDefinitionId(PropDefId.valueOf(p.getProperty("definitionId")));
        state.setValues(getValues(p, "values", state.getType()));

        return state;
    }

    /**
     * Store an item state to the transaction-managed storage
     *
     * @param state item state
     * @return independent element id that was assigned
     * @throws IOException if an error occurs
     */
    public String store(ItemState state) throws IOException {
        if (state.isNode()) {
            return store((NodeState) state);
        } else {
            return store((PropertyState) state);
        }
    }

    /**
     * Store a node state to the transaction-managed storage
     */
    private String store(NodeState state) throws IOException {
        Properties p = new Properties();

        p.setProperty("uuid", state.getUUID());
        if (state.getParentUUID() != null) {
            p.setProperty("parentUUID", state.getParentUUID());
        }
        p.setProperty("definitionId", state.getDefinitionId().toString());
        p.setProperty("nodeTypeName", state.getNodeTypeName().toString());
        setCollection(p, "parentUUIDs", state.getParentUUIDs());
        setCollection(p, "mixinTypeNames", state.getMixinTypeNames());
        setCollection(p, "propertyEntries", state.getPropertyEntries());
        setChildNodeEntries(p, "childNodeEntries", state.getChildNodeEntries());

        return store(p, "N");
    }

    /**
     * Store a property state to the transaction-managed storage
     */
    private String store(PropertyState state) throws IOException {
        Properties p = new Properties();

        p.setProperty("name", state.getName().toString());
        p.setProperty("parentUUID", state.getParentUUID());

        p.setProperty("type", PropertyType.nameFromValue(state.getType()));
        p.setProperty("definitionId", state.getDefinitionId().toString());
        p.setProperty("multiValued", Boolean.toString(state.isMultiValued()));
        setValues(p, "values", state.getValues());

        return store(p, "P");
    }

    /**
     * Store some properties for a specific item id
     */
    private String store(Properties p, String prefix) throws IOException {
        OutputStream out = null;

        try {
            String id = prefix + nextElementId();

            File f = new File(directory, id);
            f.getParentFile().mkdirs();

            out = new BufferedOutputStream(new FileOutputStream(f));
            p.store(out, null);

            return id;

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e2) {
                }
            }
        }
    }

    /**
     * Get the next element id
     */
    private String nextElementId() {
        return String.valueOf(++counter);
    }

    /**
     * Retrieve a list from a properties array.
     */
    private List getList(Properties p, String baseName) {
        List l = new ArrayList();

        int count = Integer.parseInt(p.getProperty(baseName));
        for (int i = 0; i < count; i++) {
            l.add(p.getProperty(baseName + "." + i));
        }
        return l;
    }

    /**
     * Retrieve a set from a properties array.
     */
    private Set getSet(Properties p, String baseName) {
        Set s = new HashSet();

        int count = Integer.parseInt(p.getProperty(baseName));
        for (int i = 0; i < count; i++) {
            s.add(p.getProperty(baseName + "." + i));
        }
        return s;
    }

    /**
     * Retrieve an array of values from a properties array.
     */
    private InternalValue[] getValues(Properties p, String baseName, int type) {
        int count = Integer.parseInt(p.getProperty(baseName));

        InternalValue[] values = new InternalValue[count];
        for (int i = 0; i < values.length; i++) {
            values[i] = InternalValue.valueOf(p.getProperty(baseName + "." + i), type);
        }
        return values;
    }

    /**
     * Add property entries.
     */
    private void addPropertyEntries(NodeState s, Properties p, String baseName) {
        int count = Integer.parseInt(p.getProperty(baseName));
        for (int i = 0; i < count; i++) {
            s.addPropertyEntry(QName.valueOf(p.getProperty(baseName + "." + i)));
        }
    }

    /**
     * Add child node entries.
     */
    private void addChildNodeEntries(NodeState s, Properties p, String baseName) {
        int count = Integer.parseInt(p.getProperty(baseName));
        for (int i = 0; i < count; i++) {
            QName name = QName.valueOf(p.getProperty(baseName + ".name." + i));
            String uuid = p.getProperty(baseName + ".uuid." + i);
            s.addChildNodeEntry(name, uuid);
        }
    }

    /**
     * Set properties from a list. The total number of items will be stored,
     * as well as the items themselves.
     */
    private void setCollection(Properties p, String baseName, Collection c) {
        p.setProperty(baseName, String.valueOf(c.size()));

        Iterator iter = c.iterator();
        int i = 0;
        while (iter.hasNext()) {
            p.setProperty(baseName + "." + i, iter.next().toString());
            i++;
        }
    }

    /**
     * Set properties from a list. The total number of items will be stored,
     * as well as the items themselves.
     */
    private void setChildNodeEntries(Properties p, String baseName, Collection c) {
        p.setProperty(baseName, String.valueOf(c.size()));

        Iterator iter = c.iterator();
        int i = 0;
        while (iter.hasNext()) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            p.setProperty(baseName + ".name." + i, entry.getName().toString());
            p.setProperty(baseName + ".uuid." + i, entry.getUUID());
            i++;
        }
    }

    /**
     * Set properties from an array of values. The total number of items will be
     * stored, as well as the items themselves.
     */
    private void setValues(Properties p, String baseName, Object[] values) {
        p.setProperty(baseName, String.valueOf(values.length));

        for (int i = 0; i < values.length; i++) {
            p.setProperty(baseName + "." + i, values[i].toString());
        }
    }
}
