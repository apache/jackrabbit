/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import javax.jcr.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <code>LazyItemIterator</code> is an id-based iterator that instantiates
 * the <code>Item</code>s only when they are requested.
 */
public class LazyItemIterator implements NodeIterator, PropertyIterator {

    private final ItemManager itemMgr;
    private final List idList;
    private int pos;

    /**
     * Creates a new <code>LazyItemIterator</code> instance.
     *
     * @param itemMgr item manager
     * @param idList  list of item id's
     */
    LazyItemIterator(ItemManager itemMgr, List idList) {
        this.itemMgr = itemMgr;
        this.idList = new ArrayList(idList);
        pos = -1;
    }

    //---------------------------------------------------------< NodeIterator >
    public Node nextNode() {
        return (Node) next();
    }

    //-----------------------------------------------------< PropertyIterator >
    public Property nextProperty() {
        return (Property) next();
    }

    //--------------------------------------------------------< RangeIterator >
    public long getPos() {
        return pos + 1;
    }

    public long getSize() {
        return idList.size();
    }

    public void skip(long skipNum) {
        if (skipNum < 0) {
            throw new IllegalArgumentException("skipNum must be a positive number");
        }
        if (pos + skipNum >= idList.size()) {
            pos = idList.size() - 1;
            throw new NoSuchElementException();
        }
        pos += skipNum;
    }

    //-------------------------------------------------------------< Iterator >
    public boolean hasNext() {
        return pos < idList.size() - 1;
    }

    public Object next() {
        if (pos >= idList.size() - 1) {
            throw new NoSuchElementException();
        }
        while (true) {
            pos++;
            try {
                return itemMgr.getItem((ItemId) idList.get(pos));
            } catch (AccessDeniedException ade) {
                // silently ignore and try next
                continue;
            } catch (RepositoryException re) {
                // FIXME: not quite correct
                throw new NoSuchElementException(re.getMessage());
            }
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
