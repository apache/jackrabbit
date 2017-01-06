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
package org.apache.jackrabbit.webdav.simple;

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.apache.jackrabbit.webdav.version.VersionHistoryResource;
import org.apache.jackrabbit.webdav.version.VersionResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.Version;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>VersionHistoryResourceImpl</code> represents a JCR version history.
 *
 * @see VersionHistory
 */
public class VersionHistoryResourceImpl extends DeltaVResourceImpl implements VersionHistoryResource {

    private static final Logger log = LoggerFactory.getLogger(VersionHistoryResourceImpl.class);

    public VersionHistoryResourceImpl(DavResourceLocator locator, DavResourceFactory factory, DavSession session, ResourceConfig config, Item item) throws DavException {
        super(locator, factory, session, config, item);
            if (getNode() == null || !(getNode() instanceof VersionHistory)) {
                throw new IllegalArgumentException("VersionHistory item expected.");
        }
    }

    //--------------------------------------------------------< DavResource >---
    /**
     * Show all versions of this history as members.
     *
     * @return
     * @see DavResource#getMembers()
     */
    @Override
    public DavResourceIterator getMembers() {
        ArrayList<DavResource> list = new ArrayList<DavResource>();
        if (exists() && isCollection()) {
            try {
                // only display versions as members of the vh. the jcr:versionLabels
                // node is an internal structure.
                VersionIterator it = ((VersionHistory) getNode()).getAllVersions();
                while (it.hasNext()) {
                    // omit item filter here. if the version history is visible
                    // its versions should be visible as well.
                    Version v = it.nextVersion();
                    DavResourceLocator vhLocator = getLocator();
                    DavResourceLocator resourceLocator = vhLocator.getFactory().createResourceLocator(vhLocator.getPrefix(), vhLocator.getWorkspacePath(), v.getPath(), false);
                    DavResource childRes = getFactory().createResource(resourceLocator, getSession());
                    list.add(childRes);
                }
            } catch (RepositoryException e) {
                // should not occur
                log.error("Unexpected error",e);
            } catch (DavException e) {
                // should not occur
                log.error("Unexpected error",e);
            }
        }
        return new DavResourceIteratorImpl(list);
    }

    /**
     * The version storage is read-only -&gt; fails with 403.
     *
     * @see DavResource#addMember(DavResource, InputContext)
     */
    @Override
    public void addMember(DavResource member, InputContext inputContext) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Removing a version resource is achieved by calling <code>removeVersion</code>
     * on the versionhistory item this version belongs to.
     *
     * @throws DavException if the version does not exist or if an error occurs
     * while deleting.
     * @see DavResource#removeMember(org.apache.jackrabbit.webdav.DavResource)
     */
    @Override
    public void removeMember(DavResource member) throws DavException {
        if (exists()) {
            VersionHistory versionHistory = (VersionHistory) getNode();
            try {
                String itemPath = member.getLocator().getRepositoryPath();
                // Retrieve the last segment of the given path and removes the index if present.
                if (itemPath == null) {
                    throw new IllegalArgumentException("Cannot retrieve name from a 'null' item path.");
                }
                String name = Text.getName(itemPath);
                // remove index
                if (name.endsWith("]")) {
                    name = name.substring(0, name.lastIndexOf('['));
                }
                versionHistory.removeVersion(name);
            } catch (RepositoryException e) {
                throw new JcrDavException(e);
            }
        } else {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Version storage is read-only -&gt; fails with 403.
     *
     * @see DavResource#setProperty(DavProperty)
     */
    @Override
    public void setProperty(DavProperty<?> property) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Version storage is read-only -&gt; fails with 403.
     *
     * @see DavResource#removeProperty(DavPropertyName)
     */
    @Override
    public void removeProperty(DavPropertyName propertyName) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    /**
     * Version storage is read-only -&gt; fails with 403.
     *
     * @see DavResource#alterProperties(List)
     */
    @Override
    public MultiStatusResponse alterProperties(List<? extends PropEntry> changeList) throws DavException {
        throw new DavException(DavServletResponse.SC_FORBIDDEN);
    }

    //---------------------------------------------< VersionHistoryResource >---
    /**
     * Return an array of {@link org.apache.jackrabbit.webdav.version.VersionResource}s representing all versions
     * present in the underlying JCR version history.
     *
     * @return array of {@link org.apache.jackrabbit.webdav.version.VersionResource}s representing all versions
     * present in the underlying JCR version history.
     * @throws org.apache.jackrabbit.webdav.DavException
     * @see org.apache.jackrabbit.webdav.version.VersionHistoryResource#getVersions()
     */
    public VersionResource[] getVersions() throws DavException {
        try {
            VersionIterator vIter = ((VersionHistory)getNode()).getAllVersions();
            ArrayList<VersionResource> l = new ArrayList<VersionResource>();
            while (vIter.hasNext()) {
                    DavResourceLocator versionLoc = getLocatorFromNode(vIter.nextVersion());
                    DavResource vr = createResourceFromLocator(versionLoc);
                    if (vr instanceof VersionResource) {
                        l.add((VersionResource) vr);
                    } else {
                        // severe error since resource factory doesn't behave correctly.
                        throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
            }
            return l.toArray(new VersionResource[l.size()]);
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Fill the property set for this resource.
     */
    @Override
    protected void initProperties() {
        if (!propsInitialized) {
            super.initProperties();

            // change resource type defined by default item collection
            properties.add(new ResourceType(new int[] {ResourceType.COLLECTION, ResourceType.VERSION_HISTORY}));

            // required root-version property for version-history resource
            try {
                String rootVersionHref = getLocatorFromNode(((VersionHistory)getNode()).getRootVersion()).getHref(false);
                properties.add(new HrefProperty(VersionHistoryResource.ROOT_VERSION, rootVersionHref, false));
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }

            // required, protected version-set property for version-history resource
            try {
                VersionIterator vIter = ((VersionHistory)getNode()).getAllVersions();
                ArrayList<Version> l = new ArrayList<Version>();
                while (vIter.hasNext()) {
                    l.add(vIter.nextVersion());
                }
                properties.add(getHrefProperty(VersionHistoryResource.VERSION_SET, l.toArray(new Version[l.size()]), true, false));
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
    }
}
