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
package org.apache.jackrabbit.core.search.lucene;

import org.apache.log4j.Logger;

import javax.jcr.*;
import java.util.NoSuchElementException;

/**
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
class PropertyIteratorImpl implements PropertyIterator {

    private static final Logger log = Logger.getLogger(PropertyIteratorImpl.class);

    private String[] props;

    private NodeIterator nodes;

    private Property next;

    private PropertyIterator currentProps;

    private long pos;

    PropertyIteratorImpl(String[] props, NodeIterator nodes) {
	this.nodes = nodes;

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
	if (next == null) throw new NoSuchElementException();
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
		    currentProps = new FilteredPropertyIterator(props, nodes.nextNode());
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
