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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.spi.ItemId;

import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * <code>DefaultIdKeyMap</code>
 */
public class DefaultIdKeyMap implements IdKeyMap {

    // TODO: use mixture of Path map and lookup by UUID
    // TODO: add possibility to limit size of map

    public boolean containsKey(ItemId id) {
        // TODO
        return false;
    }

    public Object get(ItemId id) {
        // TODO
        return null;
    }

    public Object put(ItemId id, Object value) {
        // TODO
        return null;
    }

    public Object remove(ItemId id) {
        // TODO
        return null;
    }

    public Set keySet() {
        // TODO
        return new HashSet(0);
    }

    public Collection values() {
        // TODO
        return new ArrayList(0);
    }

    public int size() {
        // TODO
        return 0;
    }

    public void clear() {
        // TODO
    }
}
