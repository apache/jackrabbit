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
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.NamespaceResolver;

import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.NoSuchElementException;

/**
 */
class PropertyIteratorImpl implements PropertyIterator {

    private static final Logger log = Logger.getLogger(PropertyIteratorImpl.class);

    private QName[] props;

    private NodeIterator nodes;

    private NamespaceResolver resolver;

    private Property next;

    private PropertyIterator currentProps;

    private long pos;

    PropertyIteratorImpl(QName[] props, NodeIterator nodes, NamespaceResolver resolver) {
        this.nodes = nodes;
        this.resolver = resolver;

        if (props != null && props.length > 0) {
            this.props = props;
        }
        try {
            fetchNext();
        } catch (RepositoryException e) {
            // FIXME this is bad error handling!
            log.error("Exception retrieving property: " + e.toString());
        }
    }

    public Property nextProperty() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        try {
            Property tmp = next;
            fetchNext();
            pos++;
            return tmp;
        } catch (RepositoryException e) {
            log.error("Exception retrieving property: " + e.toString());
            // FIXME this is bad error handling!
            throw new NoSuchElementException();
        }
    }

    public void skip(long skipNum) {
        while (skipNum-- > 0) {
            next();
        }
    }

    public long getSize() {
        return -1;
    }

    public long getPos() {
        return pos;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    public boolean hasNext() {
        return (next != null);
    }

    public Object next() {
        return nextProperty();
    }

    //--------------------< internal >------------------------------------------

    private void fetchNext() throws RepositoryException {
        next = null;
        if (currentProps == null) {
            // try to get next PropertyIterator
            if (nodes.hasNext()) {
                if (props != null) {
                    currentProps = new FilteredPropertyIterator(props, nodes.nextNode(), resolver);
                } else {
                    currentProps = nodes.nextNode().getProperties();
                }
            }
        }

        if (currentProps != null) {
            next = currentProps.nextProperty();
            if (!currentProps.hasNext()) {
                // reset current iterator
                currentProps = null;
            }
        }
    }
}
