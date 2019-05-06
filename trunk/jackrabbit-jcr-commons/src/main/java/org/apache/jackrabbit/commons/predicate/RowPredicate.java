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
package org.apache.jackrabbit.commons.predicate;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;

/**
 * Predicate for checking whether a given object is a {@link Row} and
 * optionally whether it contains a given selector. Subclasses can extend
 * this class to provide more complex checking of the row or the selected
 * node.
 *
 * @since Apache Jackrabbit 2.2
 */
public class RowPredicate implements Predicate {

    /**
     * Selector name, or <code>null</code>.
     */
    private final String selectorName;

    /**
     * Creates a row predicate that checks the existence of the given
     * selector (if given).
     *
     * @param selectorName selector name, or <code>null</code>
     */
    public RowPredicate(String selectorName) {
        this.selectorName = selectorName;
    }

    /**
     * Creates a row predicate.
     */
    public RowPredicate() {
        this(null);
    }

    /**
     * Checks whether the given object is a {@link Row} and calls the
     * protected {@link #evaluate(Row)} method to evaluate the row.
     */
    public boolean evaluate(Object object) {
        if (object instanceof Row) {
            try {
                return evaluate((Row) object);
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to evaluate " + object, e);
            }
        } else {
            return false;
        }
    }

    /**
     * Evaluates the given row. If a selector name is specified, then
     * the corresponding node in this row is evaluated by calling the
     * protected {@link #evaluate(Node)} method.
     */
    protected boolean evaluate(Row row) throws RepositoryException {
        if (selectorName != null) {
            return evaluate(row.getNode(selectorName));
        } else {
            return true;
        }
    }

    /**
     * Evaluates the given node. The default implementation always
     * returns <code>true</code>.
     */
    protected boolean evaluate(Node node) throws RepositoryException {
        return true;
    }

}
