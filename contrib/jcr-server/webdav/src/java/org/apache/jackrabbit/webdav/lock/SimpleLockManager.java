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

import java.util.HashMap;
import java.util.Iterator;

import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.util.Text;

/**
 * Simple manager for webdav locks.<br>
 * NOTE: the timeout requested is always replace by a infinite timeout and
 * expiration of locks is not checked.
 */
public class SimpleLockManager implements LockManager {

    /** map of locks */
    private HashMap locks = new HashMap();

    /**
     *
     * @param lockToken
     * @param resource
     * @return
     * @see LockManager#hasLock(String, org.apache.jackrabbit.webdav.DavResource)
     */
    public boolean hasLock(String lockToken, DavResource resource) {
	ActiveLock lock = (ActiveLock) locks.get(resource.getResourcePath());
	if (lock != null && lock.getToken().equals(lockToken)) {
	    return true;
	}
	return false;
    }

    /**
     * Returns the lock applying to the given resource or <code>null</code> if
     * no lock can be found.
     *
     * @param type
     * @param scope
     * @param resource
     * @return lock that applies to the given resource or <code>null</code>.
     */
    public ActiveLock getLock(Type type, Scope scope, DavResource resource) {
	if (!(Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope))) {
	    return null;
	}
	String key = resource.getResourcePath();
	ActiveLock lock = (locks.containsKey(key)) ? (ActiveLock)locks.get(key) : null;

	// look for an inherited lock
	if (lock == null) {
	    // cut path instead of retrieving the parent resource
	    String parentPath = Text.getRelativeParent(key, 1);
	    boolean found = false;
	    /* stop as soon as parent lock is found:
	    if the lock is deep or the parent is a collection the lock
	    applies to the given resource. */
	    while (!"/".equals(parentPath) && !(found = locks.containsKey(parentPath))) {
		parentPath = Text.getRelativeParent(parentPath, 1);
	    }
	    if (found) {
		ActiveLock parentLock = (ActiveLock)locks.get(parentPath);
		if (parentLock.isDeep()) {
		    lock = parentLock;
		}
	    }
	}
	// since locks have infinite timeout, check for expired lock is omitted.
	return lock;
    }

    /**
     * Adds the lock for the given resource, replacing any existing lock.
     *
     * @param lockInfo
     * @param resource being the lock holder
     */
    public synchronized ActiveLock createLock(LockInfo lockInfo, DavResource resource)
	    throws DavException {
	if (lockInfo == null || resource == null) {
	    throw new IllegalArgumentException("Neither lockInfo nor resource must be null.");
	}

	String resourcePath = resource.getResourcePath();
	// test if there is already a lock present on this resource
	if (locks.containsKey(resourcePath)) {
	    throw new DavException(DavServletResponse.SC_LOCKED, "Resource '" + resource.getResourcePath() + "' already holds a lock.");
	}
	// test if the new lock would conflict with any lock inherited from the
	// collection or with a lock present on any member resource.
	Iterator it = locks.keySet().iterator();
	while (it.hasNext()) {
	    String key = (String) it.next();
	    // TODO: is check for lock on internal-member correct?
	    if (Text.isDescendant(key, resourcePath)) {
		ActiveLock l = (ActiveLock) locks.get(key);
		if (l.isDeep() || (key.equals(Text.getRelativeParent(resourcePath, 1)) && !resource.isCollection())) {
		    throw new DavException(DavServletResponse.SC_LOCKED, "Resource '" + resource.getResourcePath() + "' already inherits a lock by its collection.");
		}
	    } else if (Text.isDescendant(resourcePath, key)) {
		if (lockInfo.isDeep() || isInternalMember(resource, key)) {
		    throw new DavException(DavServletResponse.SC_CONFLICT, "Resource '" + resource.getResourcePath() + "' cannot be locked due to a lock present on the member resource '" + key + "'.");
		}

	    }
	}
	ActiveLock lock = new DefaultActiveLock(lockInfo);
	// Lazy: reset the timeout to 'Infinite', in order to omit the tests for
	// lock expiration.
	lock.setTimeout(DavConstants.INFINITE_TIMEOUT);
	locks.put(resource.getResourcePath(), lock);
	return lock;
    }

    /**
     *
     * @param lockInfo
     * @param lockToken
     * @param resource
     * @return
     * @throws DavException
     * @see DavResource#refreshLock(org.apache.jackrabbit.webdav.lock.LockInfo, String)
     */
    public ActiveLock refreshLock(LockInfo lockInfo, String lockToken, DavResource resource)
	    throws DavException {
	// timeout is always infinite > no test for expiration or adjusting timeout needed.
	ActiveLock lock = getLock(lockInfo.getType(), lockInfo.getScope(), resource);
	if (lock == null) {
	    throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
	} else if (!lock.getToken().equals(lockToken)) {
	    throw new DavException(DavServletResponse.SC_LOCKED);
	}
	return lock;
    }

    /**
     * Remove the lock hold by the given resource.
     *
     * @param lockToken
     * @param resource that is the lock holder
     */
    public synchronized void releaseLock(String lockToken, DavResource resource)
	    throws DavException {
	if (!locks.containsKey(resource.getResourcePath())) {
	    throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
	}
	// since locks have infinite timeout, check for expiration is omitted.

	ActiveLock lock = (ActiveLock) locks.get(resource.getResourcePath());
	if (lock.getToken().equals(lockToken)) {
	    locks.remove(resource.getResourcePath());
	} else {
	    throw new DavException(DavServletResponse.SC_LOCKED);
	}
    }

    /**
     * Return true, if the resource with the given memberPath is a internal
     * non-collection member of the given resource, thus affected by a
     * non-deep lock present on the resource.
     *
     * @param resource
     * @param memberPath
     * @return
     */
    private static boolean isInternalMember(DavResource resource, String memberPath) {
	if (resource.getResourcePath().equals(Text.getRelativeParent(memberPath, 1))) {
	    // find the member with the given path
	    DavResourceIterator it = resource.getMembers();
	    while (it.hasNext()) {
		DavResource member = it.nextResource();
		if (member.getResourcePath().equals(memberPath)) {
		    // return true if that member is not a collection
		    return !member.isCollection();
		}
	    }
	}
	return false;
    }
}

