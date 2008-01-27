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

import org.apache.jackrabbit.core.fs.FileSystemResource;

import java.io.IOException;

/**
 * <code>SynonymProvider</code> defines an interface for a component that
 * returns synonyms for a given term.
 */
public interface SynonymProvider {

    /**
     * Initializes the synonym provider and passes the file system resource to
     * the synonym provider configuration defined by the configuration value of
     * the <code>synonymProviderConfigPath</code> parameter. The resource may be
     * <code>null</code> if the configuration parameter is not set.
     *
     * @param fsr the file system resource to the synonym provider
     *            configuration.
     * @throws IOException if an error occurs while initializing the synonym
     *                     provider.
     */
    void initialize(FileSystemResource fsr) throws IOException;

    /**
     * Returns an array of terms that are considered synonyms for the given
     * <code>term</code>.
     *
     * @param term a search term.
     * @return an array of synonyms for the given <code>term</code> or an empty
     *         array if no synonyms are known.
     */
    String[] getSynonyms(String term);

}
