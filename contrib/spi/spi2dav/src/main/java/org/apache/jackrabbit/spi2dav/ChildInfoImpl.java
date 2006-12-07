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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.name.QName;

/**
 * <code>ChildInfoImpl</code>...
 */
class ChildInfoImpl implements ChildInfo {

    private final QName qName;
    private final int index;
    private final String uniqueID;

    ChildInfoImpl(QName qName, int index, String uniqueID) {
        this.qName = qName;
        this.index = index;
        this.uniqueID = uniqueID;
    }

    /**
     * @see ChildInfo#getName()
     */
    public QName getName() {
        return qName;
    }

    /**
     * @see ChildInfo#getUniqueID()
     */
    public String getUniqueID() {
        return this.uniqueID;
    }

    /**
     * @see ChildInfo#getIndex()
     */
    public int getIndex() {
        return index;
    }
}