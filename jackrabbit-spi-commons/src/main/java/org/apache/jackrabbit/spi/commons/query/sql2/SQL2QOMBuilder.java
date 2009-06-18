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
package org.apache.jackrabbit.spi.commons.query.sql2;

import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.ValueFactory;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.query.QueryObjectModelBuilder;

/**
 * <code>SQL2QOMBuilder</code> implements QOM builder that understands
 * {@link Query#JCR_SQL2}.
 */
public class SQL2QOMBuilder implements QueryObjectModelBuilder {

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
        return Query.JCR_SQL2.equals(language);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSupportedLanguages() {
        return new String[]{Query.JCR_SQL2};
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
