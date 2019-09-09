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
 * <code>LocalNameRangeQuery</code> implements a range query on the local name
 * of nodes.
 */
@SuppressWarnings("serial")
public class LocalNameRangeQuery extends RangeQuery {

    /**
     * Creates a new <code>LocalNameRangeQuery</code>. The lower or the upper
     * bound may be null, but not both!
     *
     * @param lowerName the lower bound or <code>null</code>.
     * @param upperName the upper bound or <code>null</code>.
     * @param inclusive if bounds are inclusive.
     */
    public LocalNameRangeQuery(String lowerName,
                               String upperName,
                               boolean inclusive,
                               PerQueryCache cache) {
        super(getLowerTerm(lowerName), getUpperTerm(upperName), inclusive, cache);
    }

    /**
     * Creates a {@link Term} for the lower bound local name.
     *
     * @param lowerName the lower bound local name.
     * @return a {@link Term} for the lower bound local name.
     */
    private static Term getLowerTerm(String lowerName) {
        String text;
        if (lowerName == null) {
            text = "";
        } else {
            text = lowerName;
        }
        return new Term(FieldNames.LOCAL_NAME, text);
    }

    /**
     * Creates a {@link Term} for the upper bound local name.
     *
     * @param upperName the upper bound local name.
     * @return a {@link Term} for the upper bound local name.
     */
    private static Term getUpperTerm(String upperName) {
        String text;
        if (upperName == null) {
            text = "\uFFFF";
        } else {
            text = upperName;
        }
        return new Term(FieldNames.LOCAL_NAME, text);
    }
}
