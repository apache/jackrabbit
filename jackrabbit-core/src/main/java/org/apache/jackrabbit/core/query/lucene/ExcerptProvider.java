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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.name.QName;
import org.apache.lucene.search.Query;

import java.io.IOException;

/**
 * <code>ExcerptProvider</code> defines an interface to get an XML excerpt
 * of a matching node.<br/>
 * E.g. if you search for 'jackrabbit' and 'query' you may get the following
 * result for a node:
 * <pre>
 * &lt;excerpt>
 *     &lt;fragment>&lt;highlight>Jackrabbit&lt;/highlight> implements both the mandatory XPath and optional SQL &lt;highlight>query&lt;/highlight> syntax.&lt;/fragment>
 *     &lt;fragment>Before parsing the XPath &lt;highlight>query&lt;/highlight> in &lt;highlight>Jackrabbit&lt;/highlight>, the statement is surrounded&lt;/fragment>
 * &lt;/excerpt>
 * </pre>
 */
public interface ExcerptProvider {

    /**
     * QName of the rep:excerpt function.
     */
    public final QName REP_EXCERPT = new QName(QName.NS_REP_URI, "excerpt(.)");

    /**
     * Initializes this excerpt provider.
     *
     * @param query excerpts will be based on this query.
     * @param index provides access to the search index.
     * @throws IOException if an error occurs while initializing this excerpt
     *                     provider.
     */
    public void init(Query query, SearchIndex index) throws IOException;

    /**
     * Returns the XML excerpt for the node with <code>id</code>.
     *
     * @param id              a node id.
     * @param maxFragments    the maximum number of fragments to create.
     * @param maxFragmentSize the maximum number of characters in a fragment.
     * @return the XML excerpt or <code>null</code> if there is no node with
     *         <code>id</code>.
     * @throws IOException if an error occurs while creating the excerpt.
     */
    public String getExcerpt(NodeId id, int maxFragments, int maxFragmentSize)
            throws IOException;
}
