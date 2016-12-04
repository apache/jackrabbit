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
package org.apache.jackrabbit.core.query.lucene.join;

import java.util.Collection;
import java.util.Comparator;

import javax.jcr.RepositoryException;
import javax.jcr.query.Row;

/**
 * Comparator for {@link Row} instances that looks only at the node paths.
 */
public class RowPathComparator implements Comparator<Row> {

    private boolean isNullEqual = true;

    /**
     * a superset of selectors, in cases where there are joins and such, the
     * possibility that a selector node does not exist in a row can happen
     */
    private Collection<String> selectors = null;

    public RowPathComparator(Collection<String> selectors) {
        this.selectors = selectors;
    }

    public RowPathComparator() {
        this(null);
    }

    /**
     * Compares two rows.
     */
    public int compare(Row a, Row b) {

        if (selectors != null) {
            // will look at equality for each path in the row
            for (String selector : selectors) {

                String pA = null;
                boolean aExists = true;
                String pB = null;
                boolean bExists = true;

                try {
                    pA = a.getPath(selector);
                } catch (RepositoryException e) {
                    // non existing A selector
                    aExists = false;
                }

                try {
                    pB = b.getPath(selector);
                } catch (RepositoryException e) {
                    // non existing B selector
                    bExists = false;
                }

                // in the case that there is a missing selector node that exists
                // on the other row, they are definitely not equal
                if ((!aExists && bExists) || (aExists && !bExists)) {
                    return aExists ? -1 : 1;
                }

                if (pA == null || pB == null) {
                    if (!isNullEqual) {
                        return -1;
                    }
                } else {
                    int local = pA.compareTo(pB);
                    if (local != 0) {
                        return local;
                    }
                }

            }
            return 0;
        }

        try {
            return a.getPath().compareTo(b.getPath());
        } catch (RepositoryException e) {
            throw new RuntimeException("Unable to compare rows " + a + " and "
                    + b, e);
        }
    }
}