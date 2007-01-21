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
package org.apache.jackrabbit.spi.xml;

import java.util.NoSuchElementException;

import org.apache.jackrabbit.spi.IdIterator;
import org.apache.jackrabbit.spi.ItemId;
import org.w3c.dom.NamedNodeMap;

public class XMLPropertyIdIterator implements IdIterator {

    private final ItemId primary;

    private final NamedNodeMap nodes;

    private int index;

    public XMLPropertyIdIterator(ItemId primary, NamedNodeMap nodes) {
        this.primary = primary;
        this.nodes = nodes;
        this.index = -1;
    }

    //----------------------------------------------------------< IdIterator >

    public ItemId nextId() {
        if (hasNext()) {
            if (++index == 0) {
                return primary;
            } else {
                return new XMLNodeId(nodes.item(index));
            }
        } else {
            throw new NoSuchElementException();
        }
    }

    public long getPosition() {
        return index + 1;
    }

    public long getSize() {
        if (nodes != null) {
            return nodes.getLength() + 1;
        } else {
            return 1;
        }
    }

    public void skip(long n) {
        while (n-- > 0) {
            next();
        }
    }

    public boolean hasNext() {
        return getPosition() < getSize();
    }

    public Object next() {
        return nextId();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
