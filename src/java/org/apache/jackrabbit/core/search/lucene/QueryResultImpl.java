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
import org.apache.log4j.Logger;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.query.QueryResult;

/**
 * Implements the <code>javax.jcr.query.QueryResult</code> interface.
 */
public class QueryResultImpl implements QueryResult {

    private static final Logger log = Logger.getLogger(QueryResultImpl.class);

    private final ItemManager itemMgr;

    private final String[] uuids;

    private final String[] selectProps;

    public QueryResultImpl(ItemManager itemMgr,
                           String[] uuids,
                           String[] selectProps) {
        this.uuids = uuids;
        this.itemMgr = itemMgr;
        this.selectProps = selectProps;
    }

    public PropertyIterator getProperties() {
        return new PropertyIteratorImpl(selectProps,
                new NodeIteratorImpl(itemMgr, uuids));
    }

    public NodeIterator getNodes() {
        return new NodeIteratorImpl(itemMgr, uuids);
    }
}
