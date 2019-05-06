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

import java.io.Reader;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.AbstractField;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.FieldInfo.IndexOptions;

/**
 * <code>IDField</code> implements a lucene field for the id of a node.
 */
public class IDField extends AbstractField {

    private static final long serialVersionUID = 3322062255855425638L;

    private final NodeId id;

    public IDField(NodeId id) {
        this.id = id;
        this.name = FieldNames.UUID;
        this.isStored = true;
        this.isTokenized = false;
        this.omitNorms = true;
        setIndexOptions(IndexOptions.DOCS_ONLY);
        setStoreTermVector(TermVector.NO);
    }

    public String stringValue() {
        return id.toString();
    }

    public Reader readerValue() {
        return null;
    }

    public TokenStream tokenStreamValue() {
        return null;
    }
}
