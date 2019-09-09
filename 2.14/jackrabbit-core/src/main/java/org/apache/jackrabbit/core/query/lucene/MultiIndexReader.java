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

import org.apache.lucene.index.IndexReader;
import org.apache.jackrabbit.core.id.NodeId;

import java.io.IOException;

/**
 * <code>MultiIndexReader</code> exposes methods to get access to the contained
 * {@link IndexReader}s of this <code>MultiIndexReader</code>.
 */
public interface MultiIndexReader extends ReleaseableIndexReader {

    /**
     * @return the <code>IndexReader</code>s that are contained in this
     *         <code>MultiIndexReader</code>.
     */
    IndexReader[] getIndexReaders();

    /**
     * Creates a document id for the given node identifier.
     *
     * @param id the id of the node.
     * @return a foreign segment doc id or <code>null</code> if there is no node
     *         with the given <code>id</code>.
     * @throws IOException if an error occurs while reading from the index.
     */
    ForeignSegmentDocId createDocId(NodeId id) throws IOException;

    /**
     * Returns the document number for the passed <code>docId</code>. If the id
     * is invalid <code>-1</code> is returned.
     *
     * @param docId the document id to resolve.
     * @return the document number or <code>-1</code> if it is invalid (e.g.
     *         does not exist).
     * @throws IOException if an error occurs while reading from the index.
     */
    int getDocumentNumber(ForeignSegmentDocId docId) throws IOException;
}
