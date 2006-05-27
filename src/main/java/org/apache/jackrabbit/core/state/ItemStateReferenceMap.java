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
package org.apache.jackrabbit.core.state;

import org.apache.commons.collections.map.ReferenceMap;

/**
 * <code>ItemStateReferenceMap</code> is a specialized <code>ItemStateMap</code>
 * that stores <code>WEAK</code> references to <code>ItemState</code> objects.
 */
public class ItemStateReferenceMap extends ItemStateMap {

    /**
     * Creates a new ReferenceMap-backed <code>ItemStateReferenceMap</code>
     * instance that stores <code>WEAK</code> references to
     * <code>ItemState</code> objects. An entry in this map is automatically
     * removed when the garbage collector determines that its value
     * (i.e. an <code>ItemState</code> object) is only weakly reachable.
     */
    public ItemStateReferenceMap() {
        super(new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK));
    }
}
