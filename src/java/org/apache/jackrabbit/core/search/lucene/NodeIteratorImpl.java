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

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeId;
import org.apache.log4j.Logger;

import javax.jcr.NodeIterator;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.NoSuchElementException;

/**
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
class NodeIteratorImpl implements NodeIterator {

    private static final Logger log = Logger.getLogger(NodeIteratorImpl.class);

    private final String[] uuids;

    private final ItemManager itemMgr;

    private int pos = 0;

    NodeIteratorImpl(ItemManager itemMgr,
		     String[] uuids) {
	this.itemMgr = itemMgr;
	this.uuids = uuids;
    }

    public Node nextNode() {
	if (pos >= uuids.length) {
	    throw new NoSuchElementException();
	}
	try {
	    return (Node) itemMgr.getItem(new NodeId(uuids[pos++]));
	} catch (RepositoryException e) {
	    log.error("Exception retrieving Node with UUID: "
		    + uuids[pos] + ": " + e.toString());
	    // FIXME this is bad error handling!
	    throw new NoSuchElementException();
	}
    }

    public Object next() {
	return nextNode();
    }

    public void skip(long skipNum) {
	if ((pos + skipNum) > uuids.length) {
	    throw new NoSuchElementException();
	}
	pos += skipNum;
    }

    public long getSize() {
	return uuids.length;
    }

    public long getPos() {
	return pos;
    }

    public boolean hasNext() {
	return pos < uuids.length;
    }

    public void remove() {
	throw new UnsupportedOperationException("remove");
    }

}
