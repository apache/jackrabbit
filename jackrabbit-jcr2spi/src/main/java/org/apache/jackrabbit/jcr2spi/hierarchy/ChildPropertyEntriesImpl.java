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
import org.apache.jackrabbit.spi.Name;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

/**
 * <code>ChildPropertyEntriesImpl</code>...
 */
public class ChildPropertyEntriesImpl implements ChildPropertyEntries {

    private static Logger log = LoggerFactory.getLogger(ChildPropertyEntriesImpl.class);

    private final Map<Name, Reference<PropertyEntry>> properties;
    private final NodeEntry parent;
    private final EntryFactory factory;

    ChildPropertyEntriesImpl(NodeEntry parent, EntryFactory factory) {
        this.properties = new HashMap<Name, Reference<PropertyEntry>>();
        this.parent = parent;
        this.factory = factory;
    }

    /**
     * @see ChildPropertyEntries#contains(Name)
     */
    public boolean contains(Name propertyName) {
        return properties.containsKey(propertyName);
    }

    /**
     * @see ChildPropertyEntries#get(Name)
     */
    public PropertyEntry get(Name propertyName) {
        Reference<PropertyEntry> ref = properties.get(propertyName);
        if (ref == null) {
            // no entry exists with the given name
            return null;
        }

        PropertyEntry entry = ref.get();
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
    public Collection<PropertyEntry> getPropertyEntries() {
        synchronized (properties) {
            Set<PropertyEntry> entries = new HashSet<PropertyEntry>(properties.size());
            for (Name propName : properties.keySet()) {
                entries.add(get(propName));
            }
            return Collections.unmodifiableCollection(entries);
        }
    }

    /**
     * @see ChildPropertyEntries#getPropertyNames()
     */
    public Collection<Name> getPropertyNames() {
        return Collections.unmodifiableCollection(properties.keySet());
    }

    /**
     * @see ChildPropertyEntries#add(PropertyEntry)
     */
    public void add(PropertyEntry propertyEntry) {
        synchronized (properties) {
            Reference<PropertyEntry> ref = new SoftReference<PropertyEntry>(propertyEntry);
            properties.put(propertyEntry.getName(), ref);
        }
    }

    /**
     * @see ChildPropertyEntries#addAll(Collection)
     */
    public void addAll(Collection<PropertyEntry> propertyEntries) {
        for (PropertyEntry pe : propertyEntries) {
            add(pe);
        }
    }

    /**
     * @see ChildPropertyEntries#remove(PropertyEntry)
     */
    public boolean remove(PropertyEntry propertyEntry) {
        synchronized (properties) {
            Name pName = propertyEntry.getName();
            PropertyEntry pe = get(pName);
            if (pe == propertyEntry) {
                properties.remove(pName);
                return true;
            } else {
                return false;
            }
        }
    }
}
