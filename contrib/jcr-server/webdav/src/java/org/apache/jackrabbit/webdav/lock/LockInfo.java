/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.jackrabbit.webdav.lock;

import org.apache.jackrabbit.webdav.DavConstants;
import org.jdom.Element;

import java.util.List;
import java.util.Iterator;

/**
 * <code>LockInfo</code> is a simple utility class encapsulating the information
 * passed with a LOCK request. It combines both the request body (which if present
 * is required to by a 'lockinfo' Xml element) and the lock relevant request
 * headers '{@link DavConstants#HEADER_TIMEOUT Timeout}' and
 * '{@link DavConstants#HEADER_DEPTH Depth}'.<br>
 * Note that is class is not intended to perform any validation of the information
 * given, since this left to those objects responsible for the lock creation
 * on the requested resource.
 */
public class LockInfo {

    private Type type;
    private Scope scope;
    private String owner;
    private boolean isDeep;
    private long timeout;

    private boolean isRefreshLock;

    /**
     * Create a new <code>LockInfo</code> object from the given information. If
     * <code>liElement</code> is <code>null</code> this lockinfo is assumed to
     * be issued from a 'Refresh Lock' request.
     *
     * @param liElement 'lockinfo' element present in the request body of a LOCK request
     * or <code>null</code> if the request was intended to refresh an existing lock.
     * @param timeout Requested timespan until the lock should expire. A LOCK
     * request MUST contain a '{@link DavConstants#HEADER_TIMEOUT Timeout}'
     * according to RFC 2518.
     * @param isDeep boolean value indicating whether the lock should be applied
     * with depth infinity or only to the requested resource.
     * @throws IllegalArgumentException if the <code>liElement</code> is not
     * <code>null</null> but does not start with an 'lockinfo' element.
     */
    public LockInfo(Element liElement, long timeout, boolean isDeep) {
        this.timeout = timeout;
        this.isDeep = isDeep;

        if (liElement != null) {
            if (!DavConstants.XML_LOCKINFO.equals(liElement.getName())) {
                throw new IllegalArgumentException("Element must have name 'lockinfo'.");
            }

            List childList = liElement.getChildren();
            for (int i = 0; i < childList.size(); i++) {
                Element child = (Element) childList.get(i);
                String nodeName = child.getName();
                if (DavConstants.XML_LOCKTYPE.equals(nodeName)) {
                    Element typeElement = getFirstChildElement(child);
                    type = Type.create(typeElement);
                } else if (DavConstants.XML_LOCKSCOPE.equals(nodeName)) {
                    Element scopeElement = getFirstChildElement(child);
                    scope = Scope.create(scopeElement);
                } else if (DavConstants.XML_OWNER.equals(nodeName)) {
                    owner = child.getChildTextTrim(DavConstants.XML_HREF);
                    if (owner==null) {
                        // check if child is a text element
                        owner = child.getTextTrim();
                    }
                }
            }
            isRefreshLock = false;
        } else {
            isRefreshLock = true;
        }
    }

    /**
     * Retrieve the first element from the content list of the specified Xml element.
     *
     * @param elem
     * @return
     */
    private static Element getFirstChildElement(Element elem) {
        if (elem.getContentSize() > 0) {
            Iterator it = elem.getContent().iterator();
            while (it.hasNext()) {
                Object content = it.next();
                if (content instanceof Element) {
                    return (Element) content;
                }
            }
        }
        return null;
    }

    /**
     * Returns the lock type or <code>null</null> if no 'lockinfo' element was
     * passed to the constructor or did not contain an 'type' element and the
     * type has not been set otherwise.
     *
     * @return type or <code>null</code>
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the lock type.
     *
     * @param type
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Return the lock scope or <code>null</null> if no 'lockinfo' element was
     * passed to the constructor or did not contain an 'scope' element and the
     * scope has not been set otherwise.
     *
     * @return scope or <code>null</code>
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Set the lock scope.
     *
     * @param scope
     */
    public void setScope(Scope scope) {
        this.scope = scope;
    }

    /**
     * Return the owner indicated by the corresponding child element from the
     * 'lockinfo' element or <code>null</null> if no 'lockinfo' element was
     * passed to the constructor or did not contain an 'owner' element.
     *
     * @return owner or <code>null</code>
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Returns true if the lock must be applied with depth infinity.
     *
     * @return true if a deep lock must be created.
     */
    public boolean isDeep() {
        return isDeep;
    }

    /**
     * Returns the time until the lock is requested to expire.
     *
     * @return time until the lock should expire.
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Returns true if this <code>LockInfo</code> was created for a LOCK
     * request intended to refresh an existing lock rather than creating a
     * new one.
     *
     * @return true if the corresponding LOCK request was intended to refresh
     * an existing lock.
     */
    public boolean isRefreshLock() {
        return isRefreshLock;
    }
}