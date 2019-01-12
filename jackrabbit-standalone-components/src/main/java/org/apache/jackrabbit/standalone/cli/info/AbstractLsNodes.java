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
package org.apache.jackrabbit.standalone.cli.info;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.standalone.cli.CommandException;
import org.apache.jackrabbit.standalone.cli.CommandHelper;

/**
 * List nodes. <br>
 * The following attributes will be used in order to customize the output:
 * <ul>
 * <li>pathKey</li>
 * <li>uuidKey</li>
 * <li>mixinKey</li>
 * <li>nodesSizeKey</li>
 * <li>propertiesSizeKey</li>
 * <li>referencesSizeKey</li>
 * <li>versionableKey</li>
 * <li>lockableKey</li>
 * <li>referenceableKey</li>
 * <li>lockedKey</li>
 * <li>hasLockKey</li>
 * <li>newKey</li>
 * <li>modifiedKey</li>
 * </ul>
 */
public abstract class AbstractLsNodes extends AbstractLs {
    /** bundle */
    private static final ResourceBundle bundle = CommandHelper.getBundle();

    /** show path */
    private String pathKey = "path";

    /** show uuid */
    private String uuidKey = "uuid";

    /** show mixin */
    private String mixinKey = "mixin";

    /** show node size */
    private String nodesSizeKey = "nodeSize";

    /** show properties size */
    private String propertiesSizeKey = "propertiesSize";

    /** show references size */
    private String referencesSizeKey = "referencesSize";

    /** show is versionable */
    private String versionableKey = "versionable";

    /** show is lockable */
    private String lockableKey = "lockable";

    /** show is referenceable */
    private String referenceableKey = "referenceable";

    /** show is locked */
    private String lockedKey = "locked";

    /** show has lock */
    private String hasLockKey = "hasLock";

    /** show is new */
    private String newKey = "new";

    /** show is modified */
    private String modifiedKey = "modified";

    /** show lock tocken */
    private String lockTokenKey = "lockToken";

    /** uuid width */
    private int uuidWidth = 36;

    /** path width */
    private int nameWidth = 30;

    /** node type width */
    private int nodeTypeWidth = 20;

    /** node type width */
    private int pathWidth = 40;

    /** referenceable width */
    private int mixinWidth = 30;

    /**
     * {@inheritDoc}
     */
    public final boolean execute(Context ctx) throws Exception {
        OptionHolder oh = new OptionHolder(ctx);

        // Get children
        Iterator iter = getNodes(ctx);

        // write header
        writeHeader(ctx, oh);

        int index = 0;

        int maxItems = getMaxItems(ctx);

        // Write item
        while (iter.hasNext() && index < maxItems) {
            Node n = (Node) iter.next();
            writeItem(ctx, n, oh);
            index++;
        }

        // Write footer
        printFooter(ctx, iter);

        return false;
    }

    /**
     * @param ctx
     *        the <code>Context</code>
     * @return the <code>Node</code>s to show
     * @throws RepositoryException if the current working <code>Repository</code> throws a <code>RepositoryException</code>
     * @throws CommandException
     */
    protected abstract Iterator getNodes(Context ctx) throws CommandException,
            RepositoryException;

    /**
     * Write a node to the current output
     * @param ctx
     *        the <code>Context</code>
     * @param n
     *        the <code>Node</code>
     * @throws RepositoryException
     * @throws CommandException
     */
    void writeItem(Context ctx, Node n, OptionHolder oh)
            throws RepositoryException, CommandException {
        // TODO do something with this long piece of code
        Collection widths = new ArrayList();
        Collection texts = new ArrayList();

        widths.add(new Integer(this.nameWidth));

        String name = n.getName();
        if (n.getIndex() > 1) {
            name += "[" + n.getIndex() + "]";
        }
        texts.add(name);

        widths.add(new Integer(this.nodeTypeWidth));
        texts.add(n.getPrimaryNodeType().getName());

        // uuid
        if (oh.isUuid()) {
            widths.add(new Integer(this.uuidWidth));
            if (n.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                texts.add(n.getUUID());
            } else {
                texts.add("");
            }
        }

        // is new
        if (oh.isNew()) {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n.isNew()));
        }

        // is new
        if (oh.isModified()) {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n.isModified()));
        }

        // mixin
        if (oh.isMixin()) {
            widths.add(new Integer(this.mixinWidth));
            Collection mixins = new ArrayList();
            // Assigned mixin types
            NodeType[] assigned = n.getMixinNodeTypes();
            for (int i = 0; i < assigned.length; i++) {
                mixins.add(assigned[i].getName());
            }

            // Inherited mixin types
            NodeType[] nt = n.getPrimaryNodeType().getSupertypes();
            for (int i = 0; i < nt.length; i++) {
                if (nt[i].isMixin()) {
                    mixins.add(nt[i].getName());
                }
            }
            texts.add(mixins);
        }

        // node size
        if (oh.isNodesSize()) {
            widths.add(new Integer(this.longWidth));
            texts.add(Long.toString(n.getNodes().getSize()));
        }

        // prop size
        if (oh.isPropertiesSize()) {
            widths.add(new Integer(this.longWidth));
            texts.add(Long.toString(n.getProperties().getSize()));
        }

        // ref size
        if (oh.isReferencesSize()) {
            widths.add(new Integer(this.longWidth));
            texts.add(Long.toString(n.getReferences().getSize()));
        }

        // is versionable
        if (oh.isVersionable()) {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n
                .isNodeType(JcrConstants.MIX_VERSIONABLE)));
        }

        // is lockable
        if (oh.isLockable()) {
            widths.add(new Integer(this.longWidth));
            texts
                .add(Boolean.toString(n.isNodeType(JcrConstants.MIX_LOCKABLE)));
        }

        // is referenceable
        if (oh.isReferenceable()) {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n
                .isNodeType(JcrConstants.MIX_REFERENCEABLE)));
        }

        // is locked
        if (oh.isLocked()) {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n.isLocked()));
        }

        // has lock
        if (oh.isHasLock()) {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n.holdsLock()));
        }

        // path
        if (oh.isPath()) {
            widths.add(new Integer(this.pathWidth));
            texts.add(n.getPath());
        }

        // lock token
        if (oh.isLockToken()) {
            widths.add(new Integer(this.nameWidth));
            if (n.isLocked()) {
                texts.add(n.getLock().getLockToken());
            } else {
                texts.add("");
            }
        }

        PrintHelper.printRow(ctx, widths, texts);
    }

    /**
     * Prints the header
     * @param ctx
     *        the <code>Context</code>
     * @throws CommandException
     */
    void writeHeader(Context ctx, OptionHolder oh) throws CommandException {
        // TODO do something with this long piece of code
        Collection widths = new ArrayList();
        Collection texts = new ArrayList();
        widths.add(new Integer(this.nameWidth));
        texts.add(bundle.getString("word.name"));
        widths.add(new Integer(this.nodeTypeWidth));
        texts.add(bundle.getString("word.nodetype"));

        // uuid
        if (oh.isUuid()) {
            widths.add(new Integer(this.uuidWidth));
            texts.add("uuid");
        }

        // is new
        if (oh.isNew()) {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("word.new"));
        }

        // is new
        if (oh.isModified()) {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("word.modified"));
        }

        // mixin
        if (oh.isMixin()) {
            widths.add(new Integer(this.mixinWidth));
            texts.add("mixin");
        }

        // node size
        if (oh.isNodesSize()) {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("word.nodes"));
        }

        // prop size
        if (oh.isPropertiesSize()) {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("word.properties"));
        }

        // ref size
        if (oh.isReferencesSize()) {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("word.references"));
        }

        // is versionable
        if (oh.isVersionable()) {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("word.versionable"));
        }

        // is lockable
        if (oh.isLockable()) {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("word.lockable"));
        }

        // is referenceable
        if (oh.isReferenceable()) {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("word.referenceable"));
        }

        // is locked
        if (oh.isLocked()) {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("word.locked"));
        }

        // has lock
        if (oh.isHasLock()) {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("phrase.haslock"));
        }

        // path
        if (oh.isPath()) {
            widths.add(new Integer(this.pathWidth));
            texts.add(bundle.getString("word.path"));
        }

        if (oh.isLockToken()) {
            widths.add(new Integer(this.nameWidth));
            texts.add(bundle.getString("word.locktoken"));
        }

        PrintHelper.printRow(ctx, widths, texts);
        PrintHelper.printSeparatorRow(ctx, widths, '-');
    }

    /**
     * option holder
     */
    private class OptionHolder {
        /** show path */
        private boolean path = false;

        /** show uuid */
        private boolean uuid = false;

        /** show mixin */
        private boolean mixin = false;

        /** show node size */
        private boolean nodesSize = false;

        /** show properties size */
        private boolean propertiesSize = false;

        /** show references size */
        private boolean referencesSize = false;

        /** show is versionable */
        private boolean versionable = false;

        /** show is lockable */
        private boolean lockable = false;

        /** show is referenceable */
        private boolean referenceable = false;

        /** show is locked */
        private boolean locked = false;

        /** show has lock */
        private boolean hasLock = false;

        /** show is new */
        private boolean new_ = false;

        /** show is modified */
        private boolean modified = false;

        /** lock tokeb */
        private boolean lockToken = false;

        /** context */
        private Context ctx;

        /**
         * @param key
         *        the key the flag key
         * @return the boolean value for the given key
         */
        private boolean getFlag(String key) {
            boolean flag = false;
            if (ctx.containsKey(key)) {
                flag = Boolean.valueOf((String) ctx.get(key)).booleanValue();
            }
            return flag;
        }

        /**
         * Constructor
         * @param ctx
         *        the <code>Context</code>
         */
        public OptionHolder(Context ctx) {
            super();
            this.ctx = ctx;
            path = getFlag(pathKey);
            uuid = getFlag(uuidKey);
            mixin = getFlag(mixinKey);
            nodesSize = getFlag(nodesSizeKey);
            propertiesSize = getFlag(propertiesSizeKey);
            referencesSize = getFlag(referencesSizeKey);
            versionable = getFlag(versionableKey);
            lockable = getFlag(lockableKey);
            referenceable = getFlag(referenceableKey);
            locked = getFlag(lockedKey);
            hasLock = getFlag(hasLockKey);
            new_ = getFlag(newKey);
            modified = getFlag(modifiedKey);
            lockToken = getFlag(lockTokenKey);
        }

        /**
         * @return the has lock
         */
        public boolean isHasLock() {
            return hasLock;
        }

        /**
         * @return Returns the lockable.
         */
        public boolean isLockable() {
            return lockable;
        }

        /**
         * @return Returns the locked.
         */
        public boolean isLocked() {
            return locked;
        }

        /**
         * @return Returns the mixin.
         */
        public boolean isMixin() {
            return mixin;
        }

        /**
         * @return Returns the modified.
         */
        public boolean isModified() {
            return modified;
        }

        /**
         * @return Returns the new_.
         */
        public boolean isNew() {
            return new_;
        }

        /**
         * @return Returns the nodesSize.
         */
        public boolean isNodesSize() {
            return nodesSize;
        }

        /**
         * @return Returns the propertiesSize.
         */
        public boolean isPropertiesSize() {
            return propertiesSize;
        }

        /**
         * @return Returns the referenceable.
         */
        public boolean isReferenceable() {
            return referenceable;
        }

        /**
         * @return Returns the referencesSize.
         */
        public boolean isReferencesSize() {
            return referencesSize;
        }

        /**
         * @return Returns the uuid.
         */
        public boolean isUuid() {
            return uuid;
        }

        /**
         * @return Returns the versionable.
         */
        public boolean isVersionable() {
            return versionable;
        }

        /**
         * @return Returns the path.
         */
        public boolean isPath() {
            return path;
        }

        public boolean isLockToken() {
            return lockToken;
        }
    }

    /**
     * @return Returns the hasLockKey.
     */
    public String getHasLockKey() {
        return hasLockKey;
    }

    /**
     * @param hasLockKey
     *        The hasLockKey to set.
     */
    public void setHasLockKey(String hasLockKey) {
        this.hasLockKey = hasLockKey;
    }

    /**
     * @return Returns the lockableKey.
     */
    public String getLockableKey() {
        return lockableKey;
    }

    /**
     * @param lockableKey
     *        The lockableKey to set.
     */
    public void setLockableKey(String lockableKey) {
        this.lockableKey = lockableKey;
    }

    /**
     * @return Returns the lockedKey.
     */
    public String getLockedKey() {
        return lockedKey;
    }

    /**
     * @param lockedKey
     *        The lockedKey to set.
     */
    public void setLockedKey(String lockedKey) {
        this.lockedKey = lockedKey;
    }

    /**
     * @return Returns the mixinKey.
     */
    public String getMixinKey() {
        return mixinKey;
    }

    /**
     * @param mixinKey
     *        The mixinKey to set.
     */
    public void setMixinKey(String mixinKey) {
        this.mixinKey = mixinKey;
    }

    /**
     * @return Returns the modifiedKey.
     */
    public String getModifiedKey() {
        return modifiedKey;
    }

    /**
     * @param modifiedKey
     *        The modifiedKey to set.
     */
    public void setModifiedKey(String modifiedKey) {
        this.modifiedKey = modifiedKey;
    }

    /**
     * @return Returns the newKey.
     */
    public String getNewKey() {
        return newKey;
    }

    /**
     * @param newKey
     *        The new_Key to set.
     */
    public void setNewKey(String newKey) {
        this.newKey = newKey;
    }

    /**
     * @return Returns the nodesSizeKey.
     */
    public String getNodesSizeKey() {
        return nodesSizeKey;
    }

    /**
     * @param nodesSizeKey
     *        The nodesSizeKey to set.
     */
    public void setNodesSizeKey(String nodesSizeKey) {
        this.nodesSizeKey = nodesSizeKey;
    }

    /**
     * @return Returns the pathKey.
     */
    public String getPathKey() {
        return pathKey;
    }

    /**
     * @param pathKey
     *        The pathKey to set.
     */
    public void setPathKey(String pathKey) {
        this.pathKey = pathKey;
    }

    /**
     * @return Returns the propertiesSizeKey.
     */
    public String getPropertiesSizeKey() {
        return propertiesSizeKey;
    }

    /**
     * @param propertiesSizeKey
     *        The propertiesSizeKey to set.
     */
    public void setPropertiesSizeKey(String propertiesSizeKey) {
        this.propertiesSizeKey = propertiesSizeKey;
    }

    /**
     * @return Returns the referenceableKey.
     */
    public String getReferenceableKey() {
        return referenceableKey;
    }

    /**
     * @param referenceableKey
     *        The referenceableKey to set.
     */
    public void setReferenceableKey(String referenceableKey) {
        this.referenceableKey = referenceableKey;
    }

    /**
     * @return Returns the referencesSizeKey.
     */
    public String getReferencesSizeKey() {
        return referencesSizeKey;
    }

    /**
     * @param referencesSizeKey
     *        The referencesSizeKey to set.
     */
    public void setReferencesSizeKey(String referencesSizeKey) {
        this.referencesSizeKey = referencesSizeKey;
    }

    /**
     * @return Returns the uuidKey.
     */
    public String getUuidKey() {
        return uuidKey;
    }

    /**
     * @param uuidKey
     *        The uuidKey to set.
     */
    public void setUuidKey(String uuidKey) {
        this.uuidKey = uuidKey;
    }

    /**
     * @return Returns the versionableKey.
     */
    public String getVersionableKey() {
        return versionableKey;
    }

    /**
     * @param versionableKey
     *        The versionableKey to set.
     */
    public void setVersionableKey(String versionableKey) {
        this.versionableKey = versionableKey;
    }

    /**
     * @return the lock token key
     */
    public String getLockTokenKey() {
        return lockTokenKey;
    }

    /**
     * @param lockTokenKey
     *        the lock token to set
     */
    public void setLockTokenKey(String lockTokenKey) {
        this.lockTokenKey = lockTokenKey;
    }
}
