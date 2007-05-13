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
package org.apache.jackrabbit.ocm.persistence.collectionconverter.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.jackrabbit.ocm.persistence.collectionconverter.ManageableCollection;

/**
 * The <code>ManagedHashMap</code> class provides Map support to JCR Mapping
 *
 * @author <a href="mailto:fmeschbe[at]apache[dot]com">Felix Meschberger</a>
 */
public class ManagedHashMap extends HashMap implements ManageableCollection {

    public ManagedHashMap() {
    }

    public ManagedHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public ManagedHashMap(Map m) {
        super(m);
    }

    public ManagedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }


    public void addObject(Object object) {
        put(object, object);
    }

    public Iterator getIterator() {
        return values().iterator();
    }


    public int getSize() {
        return size();
    }
}
