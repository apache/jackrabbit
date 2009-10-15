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
package org.apache.jackrabbit.commons.query.sql2;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.ValueFactory;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.query.QueryObjectModelBuilder;

/**
 * <code>SQL2QOMBuilder</code> implements QOM builder that understands
 * {@link Query#JCR_SQL2} and {@link Query#JCR_JQOM}. <code>JCR_JQOM</code>
 * might be surprising, but JSR 283 says that the serialization format of
 * <code>JCR_JQOM</code> is <code>JCR_SQL2</code>. This is important when
 * a JQOM is stored on a node as a serialized String and a language property
 * set to <code>JCR_JQOM</code>.
 */
public class SQL2QOMBuilder implements QueryObjectModelBuilder {

    /**
     * Supports {@link Query#JCR_JQOM} and {@link Query#JCR_SQL2}.
     */
    private static final List<String> SUPPORTED = new ArrayList<String>(
            Arrays.asList(Query.JCR_JQOM, Query.JCR_SQL2));

    /**
     * {@inheritDoc}
     */
    public QueryObjectModel createQueryObjectModel(String statement,
                                                   QueryObjectModelFactory qf,
                                                   ValueFactory vf)
            throws InvalidQueryException, RepositoryException {
        return new Parser(qf, vf).createQueryObjectModel(statement);
    }

    /**
     * {@inheritDoc}
     */
    public boolean canHandle(String language) {
        return SUPPORTED.contains(language);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSupportedLanguages() {
        return SUPPORTED.toArray(new String[SUPPORTED.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public String toString(QueryObjectModel qom)
            throws InvalidQueryException {
        try {
            return QOMFormatter.format(qom);
        } catch (RepositoryException e) {
            throw new InvalidQueryException(e.getMessage(), e);
        }
    }
}
