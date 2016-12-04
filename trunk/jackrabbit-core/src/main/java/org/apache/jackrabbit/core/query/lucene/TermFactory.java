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

import org.apache.lucene.index.Term;

/**
 * <code>TermFactory</code> is a factory for <code>Term</code> instances with
 * frequently used field names.
 */
public final class TermFactory {

    /**
     * Template for UUID terms.
     */
    private static final Term UUID_TERM_TEMPLATE = new Term(FieldNames.UUID);

    /**
     * Creates a Term with the given <code>id</code> value and with a field
     * name {@link FieldNames#UUID}.
     *
     * @param id the id.
     * @return the UUIDTerm.
     */
    public static Term createUUIDTerm(String id) {
        return UUID_TERM_TEMPLATE.createTerm(id);
    }
}
