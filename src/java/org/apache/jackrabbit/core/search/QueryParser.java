/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.search;

import org.apache.jackrabbit.core.search.jcrql.JCRQLQueryBuilder;
import org.apache.jackrabbit.core.search.xpath.XPathQueryBuilder;
import org.apache.jackrabbit.core.search.sql.JCRSQLQueryBuilder;
import org.apache.jackrabbit.core.NamespaceResolver;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

/**
 * This class acts as the central entry point for parsing query statements from
 * different query syntaxes into a query tree.
 */
public class QueryParser {

    /**
     * This class cannot be instanciated.
     */
    private QueryParser() {
    }

    /**
     * Parses a query <code>statement</code> according to a query
     * <code>language</code> into a query tree.
     * <p/>
     * <code>language</code> must be one of: {@link javax.jcr.query.Query#JCRQL},
     * {@link javax.jcr.query.Query#XPATH_DOCUMENT_VIEW},
     * {@link javax.jcr.query.Query#XPATH_SYSTEM_VIEW}.
     *
     * @param statement the query statement.
     * @param language  the language of the query statement.
     * @return the root node of the generated query tree.
     * @throws InvalidQueryException if an error occurs while parsing the
     *                               statement.
     */
    public static QueryRootNode parse(String statement,
                                      String language,
                                      NamespaceResolver resolver)
            throws InvalidQueryException {

        if (Query.JCRQL.equals(language)) {
            return JCRQLQueryBuilder.createQuery(statement);
        } else if (Query.XPATH_DOCUMENT_VIEW.equals(language)) {
            return XPathQueryBuilder.createQuery(statement, resolver);
        } else if ("sql".equals(language)) {
            return JCRSQLQueryBuilder.createQuery(statement, resolver);
        } else {
            throw new InvalidQueryException("unknown language");
        }
    }

}
