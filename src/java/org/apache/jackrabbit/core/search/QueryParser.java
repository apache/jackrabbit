/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
     * <code>language</code> must be one of: {@link javax.jcr.query.Query#SQL},
     * {@link javax.jcr.query.Query#XPATH}.
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

        if (Query.XPATH.equals(language)) {
            return XPathQueryBuilder.createQuery(statement, resolver);
        } else if (Query.SQL.equals(language)) {
            return JCRSQLQueryBuilder.createQuery(statement, resolver);
        } else {
            throw new InvalidQueryException("Unsupported language: " + language);
        }
    }

    /**
     * Creates a String representation of the QueryNode tree argument
     * <code>root</code>. The argument <code>language</code> specifies the
     * syntax.
     * See also: {@link javax.jcr.query.QueryManager#getSupportedQueryLanguages()}.
     *
     * @param root the query node tree.
     * @param language one of the languages returned by:
     *   {@link javax.jcr.query.QueryManager#getSupportedQueryLanguages()}.
     * @param resolver to resolve QNames.
     *
     * @return a String representation of the query node tree.
     *
     * @throws InvalidQueryException if the query node tree cannot be converted
     * into a String representation of the given language. This might be due to
     * syntax restrictions of the given language. This exception is also thrown
     * if <code>language</code> is not one of the supported query languages
     * returned by the {@link javax.jcr.query.QueryManager}.
     */
    public static String toString(QueryRootNode root,
                                  String language,
                                  NamespaceResolver resolver)
            throws InvalidQueryException {

        if (Query.XPATH.equals(language)) {
            return XPathQueryBuilder.toString(root, resolver);
        } else if (Query.SQL.equals(language)) {
            return JCRSQLQueryBuilder.toString(root, resolver);
        } else {
            throw new InvalidQueryException("Unsupported language: " + language);
        }
    }

}
