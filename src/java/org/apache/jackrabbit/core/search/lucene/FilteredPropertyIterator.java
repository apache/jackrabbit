/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.search.lucene;

import org.apache.log4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.NoSuchElementException;

/**
 */
class FilteredPropertyIterator implements PropertyIterator {

    private static final Logger log = Logger.getLogger(FilteredPropertyIterator.class);

    private String[] props;

    private Node node;

    private int propIndex;

    FilteredPropertyIterator(String[] props, Node node) {
        this.props = props;
        this.node = node;
    }

    public Property nextProperty() {
        if (propIndex >= props.length) {
            throw new NoSuchElementException();
        }
        try {
            return node.getProperty(props[propIndex++]);
        } catch (RepositoryException e) {
            // FIXME find better error handling
            log.error("Exception retrieving property with name: "
                    + props[propIndex - 1]);
            throw new NoSuchElementException();
        }
    }

    public void skip(long skipNum) {
        if ((propIndex + skipNum) > props.length) {
            throw new NoSuchElementException();
        }
        propIndex += skipNum;
    }

    public long getSize() {
        return props.length;
    }

    public long getPos() {
        return propIndex;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    public boolean hasNext() {
        return propIndex < props.length;
    }

    public Object next() {
        return nextProperty();
    }

}
