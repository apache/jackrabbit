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
package org.apache.jackrabbit.core.persistence.bundle.util;

/**
 * The <code>StringIndex</code> defines a very simple interface that mapps
 * strings to an integer and vice versa. the mapping must be unique and
 * stable across repository restarts.
 */
public interface StringIndex {

    /**
     * Returns the index for a given string. if the string does not exist in
     * the underlying index map a new index needs to be created.
     *
     * @param string the string to return the index for
     * @return the index of that string.
     */
    int stringToIndex(String string);

    /**
     * Returns the string for a given index. if the index does not exist in the
     * underlying index map, <code>null</code> is returned.
     *
     * @param idx the index tp returns the string for.
     * @return the string or <code>null</code>
     */
    String indexToString(int idx);

}
