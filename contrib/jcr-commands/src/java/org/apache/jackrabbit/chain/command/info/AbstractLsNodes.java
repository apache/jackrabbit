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
package org.apache.jackrabbit.chain.command.info;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.chain.Context;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.chain.CtxHelper;
import org.apache.jackrabbit.chain.JcrCommandException;

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
 * <li>new_Key</li>
 * <li>modifiedKey</li>
 * </ul>
 */
public abstract class AbstractLsNodes extends AbstractLs
{
    // show path
    private String pathKey;

    // show uuid
    private String uuidKey;

    // show mixin
    private String mixinKey;

    // show node size
    private String nodesSizeKey;

    // show properties size
    private String propertiesSizeKey;

    // show references size
    private String referencesSizeKey;

    // show is versionable
    private String versionableKey;

    // show is lockable
    private String lockableKey;

    // show is referenceable
    private String referenceableKey;

    // show is locked
    private String lockedKey;

    // show has lock
    private String hasLockKey;

    // show is new
    private String new_Key;

    // show is modified
    private String modifiedKey;

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
     * @inheritDoc
     */
    public final boolean execute(Context ctx) throws Exception
    {
        OptionHolder oh = new OptionHolder(ctx);

        // Get children
        Iterator iter = getNodes(ctx);

        // write header
        writeHeader(ctx, oh);

        int index = 0;

        int maxItems = getMaxItems(ctx);

        // Write item
        while (iter.hasNext() && index < maxItems)
        {
            Node n = (Node) iter.next();
            writeItem(ctx, n, oh);
            index++;
        }

        // Write footer
        printFooter(ctx, iter);

        return false;
    }

    /**
     * Get nodes to show
     * 
     * @param ctx
     * @return
     * @throws RepositoryException
     * @throws JcrCommandException
     */
    protected abstract Iterator getNodes(Context ctx)
            throws JcrCommandException, RepositoryException;

    /**
     * Write a node to the current output
     * 
     * @param ctx
     * @param n
     * @throws RepositoryException
     * @throws JcrCommandException
     */
    void writeItem(Context ctx, Node n, OptionHolder oh)
            throws RepositoryException, JcrCommandException
    {
        // TODO do something with this long piece of code
        Collection widths = new ArrayList();
        Collection texts = new ArrayList();

        widths.add(new Integer(this.nameWidth));

        String name = n.getName();
        if (n.getIndex() > 1)
        {
            name += "[" + n.getIndex() + "]";
        }
        texts.add(name);

        widths.add(new Integer(this.nodeTypeWidth));
        texts.add(n.getPrimaryNodeType().getName());

        // uuid
        if (oh.isUuid())
        {
            widths.add(new Integer(this.uuidWidth));
            if (n.isNodeType(JcrConstants.MIX_REFERENCEABLE))
            {
                texts.add(n.getUUID());
            } else
            {
                texts.add("");
            }
        }

        // is new
        if (oh.isNew_())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n.isNew()));
        }

        // is new
        if (oh.isModified())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n.isModified()));
        }

        // mixin
        if (oh.isMixin())
        {
            widths.add(new Integer(this.mixinWidth));
            Collection mixins = new ArrayList();
            // Assigned mixin types
            NodeType[] assigned = n.getMixinNodeTypes();
            for (int i = 0; i < assigned.length; i++)
            {
                mixins.add(assigned[i].getName());
            }

            // Inherited mixin types
            NodeType[] nt = n.getPrimaryNodeType().getSupertypes();
            for (int i = 0; i < nt.length; i++)
            {
                if (nt[i].isMixin())
                {
                    mixins.add(nt[i].getName());
                }
            }
            texts.add(mixins);
        }

        // node size
        if (oh.isNodesSize())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(Long.toString(n.getNodes().getSize()));
        }

        // prop size
        if (oh.isPropertiesSize())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(Long.toString(n.getProperties().getSize()));
        }

        // ref size
        if (oh.isReferencesSize())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(Long.toString(n.getReferences().getSize()));
        }

        // is versionable
        if (oh.isVersionable())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n
                .isNodeType(JcrConstants.MIX_VERSIONABLE)));
        }

        // is lockable
        if (oh.isLockable())
        {
            widths.add(new Integer(this.longWidth));
            texts
                .add(Boolean.toString(n.isNodeType(JcrConstants.MIX_LOCKABLE)));
        }

        // is referenceable
        if (oh.isReferenceable())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n
                .isNodeType(JcrConstants.MIX_REFERENCEABLE)));
        }

        // is locked
        if (oh.isLocked())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n.isLocked()));
        }

        // has lock
        if (oh.isHasLock())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(Boolean.toString(n.holdsLock()));
        }

        // path
        if (oh.isPath())
        {
            widths.add(new Integer(this.pathWidth));
            texts.add(n.getPath());
        }

        PrintHelper.printRow(ctx, widths, texts);
    }

    /**
     * Prints the header
     * 
     * @param ctx
     * @throws JcrCommandException
     */
    void writeHeader(Context ctx, OptionHolder oh) throws JcrCommandException
    {
        // TODO do something with this long piece of code
        Collection widths = new ArrayList();
        Collection texts = new ArrayList();
        widths.add(new Integer(this.nameWidth));
        texts.add(bundle.getString("name"));
        widths.add(new Integer(this.nodeTypeWidth));
        texts.add(bundle.getString("nodetype"));

        // uuid
        if (oh.isUuid())
        {
            widths.add(new Integer(this.uuidWidth));
            texts.add("uuid");
        }

        // is new
        if (oh.isNew_())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("new"));
        }

        // is new
        if (oh.isModified())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("modified"));
        }

        // mixin
        if (oh.isMixin())
        {
            widths.add(new Integer(this.mixinWidth));
            texts.add("mixin");
        }

        // node size
        if (oh.isNodesSize())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("nodes"));
        }

        // prop size
        if (oh.isPropertiesSize())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("properties"));
        }

        // ref size
        if (oh.isReferencesSize())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("references"));
        }

        // is versionable
        if (oh.isVersionable())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("versionable"));
        }

        // is lockable
        if (oh.isLockable())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("lockable"));
        }

        // is referenceable
        if (oh.isReferenceable())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("referenceable"));
        }

        // is locked
        if (oh.isLocked())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("locked"));
        }

        // has lock
        if (oh.isHasLock())
        {
            widths.add(new Integer(this.longWidth));
            texts.add(bundle.getString("has.lock"));
        }

        // path
        if (oh.isPath())
        {
            widths.add(new Integer(this.pathWidth));
            texts.add(bundle.getString("path"));
        }

        PrintHelper.printRow(ctx, widths, texts);
        PrintHelper.printSeparatorRow(ctx, widths, '-');
    }

    private class OptionHolder
    {
        // show path
        boolean path = false;

        // show uuid
        boolean uuid = false;

        // show mixin
        boolean mixin = false;

        // show node size
        boolean nodesSize = false;

        // show properties size
        boolean propertiesSize = false;

        // show references size
        boolean referencesSize = false;

        // show is versionable
        boolean versionable = false;

        // show is lockable
        boolean lockable = false;

        // show is referenceable
        boolean referenceable = false;

        // show is locked
        boolean locked = false;

        // show has lock
        boolean hasLock = false;

        // show is new
        boolean new_ = false;

        // show is modified
        boolean modified = false;

        /**
         * 
         */
        public OptionHolder(Context ctx)
        {
            super();
            path = CtxHelper.getBooleanAttr(pathKey, false, ctx);
            uuid = CtxHelper.getBooleanAttr(uuidKey, false, ctx);
            mixin = CtxHelper.getBooleanAttr(mixinKey, false, ctx);
            nodesSize = CtxHelper.getBooleanAttr(nodesSizeKey, false, ctx);
            propertiesSize = CtxHelper.getBooleanAttr(propertiesSizeKey, false,
                ctx);
            referencesSize = CtxHelper.getBooleanAttr(referenceableKey, false,
                ctx);
            versionable = CtxHelper.getBooleanAttr(versionableKey, false, ctx);
            lockable = CtxHelper.getBooleanAttr(lockableKey, false, ctx);
            referenceable = CtxHelper.getBooleanAttr(referenceableKey, false,
                ctx);
            locked = CtxHelper.getBooleanAttr(lockedKey, false, ctx);
            hasLock = CtxHelper.getBooleanAttr(hasLockKey, false, ctx);
            new_ = CtxHelper.getBooleanAttr(new_Key, false, ctx);
            modified = CtxHelper.getBooleanAttr(modifiedKey, false, ctx);
        }

        /**
         * @return Returns the hasLock.
         */
        public boolean isHasLock()
        {
            return hasLock;
        }

        /**
         * @return Returns the lockable.
         */
        public boolean isLockable()
        {
            return lockable;
        }

        /**
         * @return Returns the locked.
         */
        public boolean isLocked()
        {
            return locked;
        }

        /**
         * @return Returns the mixin.
         */
        public boolean isMixin()
        {
            return mixin;
        }

        /**
         * @return Returns the modified.
         */
        public boolean isModified()
        {
            return modified;
        }

        /**
         * @return Returns the new_.
         */
        public boolean isNew_()
        {
            return new_;
        }

        /**
         * @return Returns the nodesSize.
         */
        public boolean isNodesSize()
        {
            return nodesSize;
        }

        /**
         * @return Returns the propertiesSize.
         */
        public boolean isPropertiesSize()
        {
            return propertiesSize;
        }

        /**
         * @return Returns the referenceable.
         */
        public boolean isReferenceable()
        {
            return referenceable;
        }

        /**
         * @return Returns the referencesSize.
         */
        public boolean isReferencesSize()
        {
            return referencesSize;
        }

        /**
         * @return Returns the uuid.
         */
        public boolean isUuid()
        {
            return uuid;
        }

        /**
         * @return Returns the versionable.
         */
        public boolean isVersionable()
        {
            return versionable;
        }

        /**
         * @return Returns the path.
         */
        public boolean isPath()
        {
            return path;
        }
    }

    /**
     * @return Returns the hasLockKey.
     */
    public String getHasLockKey()
    {
        return hasLockKey;
    }

    /**
     * @param hasLockKey
     *            The hasLockKey to set.
     */
    public void setHasLockKey(String hasLockKey)
    {
        this.hasLockKey = hasLockKey;
    }

    /**
     * @return Returns the lockableKey.
     */
    public String getLockableKey()
    {
        return lockableKey;
    }

    /**
     * @param lockableKey
     *            The lockableKey to set.
     */
    public void setLockableKey(String lockableKey)
    {
        this.lockableKey = lockableKey;
    }

    /**
     * @return Returns the lockedKey.
     */
    public String getLockedKey()
    {
        return lockedKey;
    }

    /**
     * @param lockedKey
     *            The lockedKey to set.
     */
    public void setLockedKey(String lockedKey)
    {
        this.lockedKey = lockedKey;
    }

    /**
     * @return Returns the mixinKey.
     */
    public String getMixinKey()
    {
        return mixinKey;
    }

    /**
     * @param mixinKey
     *            The mixinKey to set.
     */
    public void setMixinKey(String mixinKey)
    {
        this.mixinKey = mixinKey;
    }

    /**
     * @return Returns the modifiedKey.
     */
    public String getModifiedKey()
    {
        return modifiedKey;
    }

    /**
     * @param modifiedKey
     *            The modifiedKey to set.
     */
    public void setModifiedKey(String modifiedKey)
    {
        this.modifiedKey = modifiedKey;
    }

    /**
     * @return Returns the new_Key.
     */
    public String getNew_Key()
    {
        return new_Key;
    }

    /**
     * @param new_Key
     *            The new_Key to set.
     */
    public void setNew_Key(String new_Key)
    {
        this.new_Key = new_Key;
    }

    /**
     * @return Returns the nodesSizeKey.
     */
    public String getNodesSizeKey()
    {
        return nodesSizeKey;
    }

    /**
     * @param nodesSizeKey
     *            The nodesSizeKey to set.
     */
    public void setNodesSizeKey(String nodesSizeKey)
    {
        this.nodesSizeKey = nodesSizeKey;
    }

    /**
     * @return Returns the pathKey.
     */
    public String getPathKey()
    {
        return pathKey;
    }

    /**
     * @param pathKey
     *            The pathKey to set.
     */
    public void setPathKey(String pathKey)
    {
        this.pathKey = pathKey;
    }

    /**
     * @return Returns the propertiesSizeKey.
     */
    public String getPropertiesSizeKey()
    {
        return propertiesSizeKey;
    }

    /**
     * @param propertiesSizeKey
     *            The propertiesSizeKey to set.
     */
    public void setPropertiesSizeKey(String propertiesSizeKey)
    {
        this.propertiesSizeKey = propertiesSizeKey;
    }

    /**
     * @return Returns the referenceableKey.
     */
    public String getReferenceableKey()
    {
        return referenceableKey;
    }

    /**
     * @param referenceableKey
     *            The referenceableKey to set.
     */
    public void setReferenceableKey(String referenceableKey)
    {
        this.referenceableKey = referenceableKey;
    }

    /**
     * @return Returns the referencesSizeKey.
     */
    public String getReferencesSizeKey()
    {
        return referencesSizeKey;
    }

    /**
     * @param referencesSizeKey
     *            The referencesSizeKey to set.
     */
    public void setReferencesSizeKey(String referencesSizeKey)
    {
        this.referencesSizeKey = referencesSizeKey;
    }

    /**
     * @return Returns the uuidKey.
     */
    public String getUuidKey()
    {
        return uuidKey;
    }

    /**
     * @param uuidKey
     *            The uuidKey to set.
     */
    public void setUuidKey(String uuidKey)
    {
        this.uuidKey = uuidKey;
    }

    /**
     * @return Returns the versionableKey.
     */
    public String getVersionableKey()
    {
        return versionableKey;
    }

    /**
     * @param versionableKey
     *            The versionableKey to set.
     */
    public void setVersionableKey(String versionableKey)
    {
        this.versionableKey = versionableKey;
    }
}
