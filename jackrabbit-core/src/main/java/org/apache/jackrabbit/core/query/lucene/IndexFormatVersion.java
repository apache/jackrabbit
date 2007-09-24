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

import java.util.Collection;

import org.apache.lucene.index.IndexReader;

/**
 * This class indicates the lucene index format that is used. Version 1 formats
 * do not have the <code>PROPERTIES_SET</code> lucene fieldname and queries
 * assuming this format also run on newer versions. When the index is recreated
 * from scratch, the Version 2 format will automatically be used. This format is
 * faster certain queries, so if the index does not contain
 * <code>PROPERTIES_SET</code> fieldname and re-indexing is an option, this is
 * advisable. Existing indexes are not automatically upgraded to a newer
 * version!
 */
public class IndexFormatVersion {

    /**
     * V1 is the index format for Jackrabbit releases 1.0 to 1.3.x.
     */
    public static final IndexFormatVersion V1 = new IndexFormatVersion(1);

    /**
     * V2 is the index format for Jackrabbit releases >= 1.4
     */
    public static final IndexFormatVersion V2 = new IndexFormatVersion(2);
    
    /**
     * The used version of the index format
     */
    private final int version;
    
    /**
     * Creates a index format version.
     *
     * @param version       The version of the index. 
     */
    private IndexFormatVersion(int version) {
        this.version = version;
    }

    /**
     * Returns the index format version
     * @return the index format version.
     */
    public int getVersion(){
        return version;
    }

    /**
     * @return a string representation of this index format version.
     */
    public String toString() {
        return String.valueOf(getVersion());
    }
    
    /**
     * @return the index format version of the index used by the given
     * index reader.
     */
    public static IndexFormatVersion getVersion(IndexReader indexReader) {
        Collection fields = indexReader.getFieldNames(IndexReader.FieldOption.ALL);
        if (fields.contains(FieldNames.PROPERTIES_SET)
                || indexReader.numDocs() == 0) {
            return IndexFormatVersion.V2;
        } else {
            return IndexFormatVersion.V1;
        }
    }
}
