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
package org.apache.jackrabbit.core.security.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlEntry;

import javax.jcr.RepositoryException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Arrays;

/**
 * <code>AccessControlEntryIterator</code>...
 */
public class AccessControlEntryIterator implements Iterator<AccessControlEntry> {

    private static Logger log = LoggerFactory.getLogger(AccessControlEntryIterator.class);

    private final List<AccessControlList> acls = new ArrayList<AccessControlList>();
    private Iterator<AccessControlEntry> currentEntries;
    private AccessControlEntry next;

    public AccessControlEntryIterator(List<AccessControlEntry> aces) {
        this(new AccessControlList[] {new UnmodifiableAccessControlList(aces)});
    }

    public AccessControlEntryIterator(AccessControlList[] acls) {
        this.acls.addAll(Arrays.asList(acls));
        next = seekNext();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return next != null;
    }

    public AccessControlEntry next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        AccessControlEntry ret = next;
        next = seekNext();
        return ret;
    }

    private AccessControlEntry seekNext() {
        while (currentEntries == null || !currentEntries.hasNext()) {
            if (acls.isEmpty()) {
                // reached last acl -> break out of while loop
                currentEntries = null;
                break;
            } else {
                AccessControlEntry[] entries = new AccessControlEntry[0];
                try {
                    entries = (acls.remove(0)).getAccessControlEntries();
                } catch (RepositoryException e) {
                    log.error("Unable to retrieve ACEs: " + e.getMessage() + " -> try next.");
                }
                currentEntries = Arrays.asList(entries).iterator();
            }
        }
        return (currentEntries == null) ? null : currentEntries.next();
    }
}