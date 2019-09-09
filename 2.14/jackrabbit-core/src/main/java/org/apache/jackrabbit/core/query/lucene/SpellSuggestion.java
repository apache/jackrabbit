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

import java.io.IOException;

import org.apache.jackrabbit.spi.commons.query.QueryRootNode;

/**
 * <code>SpellSuggestion</code> implements a spell suggestion, which uses the
 * spell checker.
 */
class SpellSuggestion {

    /**
     * The spell checker.
     */
    private final SpellChecker spellChecker;

    /**
     * The abstract query tree.
     */
    private final QueryRootNode root;

    /**
     * Creates a new spell suggestion.
     *
     * @param spellChecker the spell checker or <code>null</code> if none is
     *                     available.
     * @param root         the abstract query tree.
     */
    SpellSuggestion(SpellChecker spellChecker, QueryRootNode root) {
        this.spellChecker = spellChecker;
        this.root = root;
    }

    /**
     * @return a suggestion for the spellcheck query node in the abstract query
     *         tree passed in the constructor of this <code>SpellSuggestion</code>.
     *         This method returns <code>null</code> if the spell checker thinks
     *         the spelling is correct or no spell checker was provided.
     * @throws IOException if an error occurs while checking the spelling.
     */
    public String getSuggestion() throws IOException {
        if (spellChecker != null) {
            return spellChecker.check(root);
        } else {
            return null;
        }
    }
}
