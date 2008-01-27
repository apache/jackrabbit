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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.Reader;

/**
 * This is the global jackrabbit lucene analyzer. By default, all
 * properties are indexed with the <code>StandardAnalyzer(new String[]{})</code>,
 * unless in the <SearchIndex> configuration a global analyzer is defined.
 *
 * In the indexing configuration, properties can be configured to be
 * indexed with a specific analyzer. If configured, this analyzer is used to
 * index the text of the property and to parse searchtext for this property.
 */

public class JackrabbitAnalyzer  extends Analyzer {

    /**
     * The default Jackrabbit analyzer if none is configured in <code><SearchIndex></code>
     * configuration.
     */
    private Analyzer defaultAnalyzer =  new StandardAnalyzer(new String[]{});

    /**
     * The indexing configuration.
     */
    private IndexingConfiguration indexingConfig;

    /**
     * A param indexingConfig the indexing configuration.
     */
    protected void setIndexingConfig(IndexingConfiguration indexingConfig) {
        this.indexingConfig = indexingConfig;
    }

    /**
     * @param analyzer the default jackrabbit analyzer
     */
    protected void setDefaultAnalyzer(Analyzer analyzer) {
        defaultAnalyzer = analyzer;
    }

    /**
     * Creates a TokenStream which tokenizes all the text in the provided
     * Reader. If the fieldName (property) is configured to have a different
     * analyzer than the default, this analyzer is used for tokenization
     */
    public TokenStream tokenStream(String fieldName, Reader reader) {
        if (indexingConfig != null) {
            Analyzer propertyAnalyzer = indexingConfig.getPropertyAnalyzer(fieldName);
            if (propertyAnalyzer != null) {
                return propertyAnalyzer.tokenStream(fieldName, reader);
            }
        }
        return defaultAnalyzer.tokenStream(fieldName, reader);
    }

}
