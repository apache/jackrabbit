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

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.lucene.search.Query;

import java.io.IOException;

/**
 * <code>ExcerptProvider</code> defines an interface to create an excerpt for
 * a matching node. The format of the excerpt is implementation specific.
 */
public interface ExcerptProvider {

    /**
     * Name of the rep:excerpt function.
     */
    Name REP_EXCERPT = NameFactoryImpl.getInstance().create(
            Name.NS_REP_URI, "excerpt(.)");

    /**
     * Initializes this excerpt provider.
     *
     * @param query excerpts will be based on this query.
     * @param index provides access to the search index.
     * @throws IOException if an error occurs while initializing this excerpt
     *                     provider.
     */
    void init(Query query, SearchIndex index) throws IOException;

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
    String getExcerpt(NodeId id, int maxFragments, int maxFragmentSize)
        throws IOException;

}
