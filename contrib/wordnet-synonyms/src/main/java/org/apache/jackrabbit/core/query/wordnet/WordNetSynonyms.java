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
package org.apache.jackrabbit.core.query.wordnet;

import org.apache.jackrabbit.core.query.lucene.SynonymProvider;
import org.apache.lucene.index.memory.SynonymMap;

import java.io.IOException;
import java.io.InputStream;

/**
 * <code>WordNetSynonyms</code> implements a {@link SynonymProvider} that is
 * backed by the WordNet prolog file
 * <a href="http://www.cogsci.princeton.edu/2.0/WNprolog-2.0.tar.gz">wn_s.pl</a>.
 */
public class WordNetSynonyms implements SynonymProvider {

    /**
     * The synonym map or <code>null</code> if an error occurred while reading
     * the prolog file.
     */
    private static final SynonymMap SYNONYM_MAP;

    static {
        InputStream in = WordNetSynonyms.class.getResourceAsStream("wn_s.pl");
        SynonymMap sm = null;
        try {
            sm = new SynonymMap(in);
        } catch (IOException e) {
            // ignore
        }
        SYNONYM_MAP = sm;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSynonyms(String string) {
        if (SYNONYM_MAP != null) {
            return SYNONYM_MAP.getSynonyms(string.toLowerCase());
        } else {
            return null;
        }
    }
}
