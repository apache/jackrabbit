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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.name.QName;

import java.util.Map;
import java.util.Iterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * <code>ChildPropertyEntriesImpl</code>...
 */
public class ChildPropertyEntriesImpl implements ChildPropertyEntries {

    private static Logger log = LoggerFactory.getLogger(ChildPropertyEntriesImpl.class);

    private final Map properties;
    private final NodeEntry parent;
    private final EntryFactory factory;

    ChildPropertyEntriesImpl(NodeEntry parent, EntryFactory factory) {
        properties = new HashMap();
        this.parent = parent;
        this.factory = factory;
    }

    /**
     * @see ChildPropertyEntries#contains(QName)
     */
    public boolean contains(QName propertyName) {
        return properties.containsKey(propertyName);
    }

    /**
     * @see ChildPropertyEntries#get(QName)
     */
    public PropertyEntry get(QName propertyName) {
        Object ref = properties.get(propertyName);
        if (ref == null) {
            // no entry exists with the given name
            return null;
        }

        PropertyEntry entry = (PropertyEntry) ((Reference) ref).get();
        if (entry == null) {
            // entry has been g-collected -> create new entry and return it.
            entry = factory.createPropertyEntry(parent, propertyName);
            add(entry);
        }
        return entry;
    }

    /**
     * @see ChildPropertyEntries#getPropertyEntries()
     */
    public Collection getPropertyEntries() {
        synchronized (properties) {
            Set entries = new HashSet(properties.size());
            for (Iterator it = getPropertyNames().iterator(); it.hasNext();) {
                QName propName = (QName) it.next();
                entries.add(get(propName));
            }
            return Collections.unmodifiableCollection(entries);
        }
    }

    /**
     * @see ChildPropertyEntries#getPropertyNames()
     */
    public Collection getPropertyNames() {
        return properties.keySet();
    }

    /**
     * @see ChildPropertyEntries#add(PropertyEntry)
     */
    public void add(PropertyEntry propertyEntry) {
        WeakReference ref = new WeakReference(propertyEntry);
        properties.put(propertyEntry.getQName(), ref);
    }

    /**
     * @see ChildPropertyEntries#addAll(Collection)
     */
    public void addAll(Collection propertyEntries) {
        for (Iterator it = propertyEntries.iterator(); it.hasNext();) {
            Object pe = it.next();
            if (pe instanceof PropertyEntry) {
                add((PropertyEntry) pe);
            }
        }
    }

    /**
     * @see ChildPropertyEntries#remove(QName) 
     */
    public boolean remove(QName propertyName) {
        WeakReference ref = (WeakReference) properties.remove(propertyName);
        return ref != null;
    }
}