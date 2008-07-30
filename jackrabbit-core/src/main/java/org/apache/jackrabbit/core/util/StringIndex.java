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
package org.apache.jackrabbit.core.util;

/**
 * A persistent two-way mapping between strings and index integers. The
 * index may or may not be sequential.
 */
public interface StringIndex {

    /**
     * Returns the index for a given string. if the string does not exist in
     * the underlying index map a new index needs to be created.
     *
     * @param string the indexed (or to be indexed) string
     * @return index of the string
     */
    int stringToIndex(String string);

    /**
     * Returns the string for a given index. If the index does not exist
     * in the underlying index map, <code>null</code> is returned.
     *
     * @param idx index of a string
     * @return the indexed string, or <code>null</code>
     */
    String indexToString(int idx);

}
