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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.util.ISO9075;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.Collections;
import java.util.Set;

/**
 * 
 */
class IndexNodeResolver extends NodeResolver {

    private final QueryManager queryManager;

    IndexNodeResolver(Session session, NamePathResolver resolver) throws RepositoryException {
        super(session, resolver);
        queryManager = session.getWorkspace().getQueryManager();
    }

    //-------------------------------------------------------< NodeResolver >---
    /**
     * @inheritDoc
     */
    @Override
    public Node findNode(Name nodeName, Name ntName) throws RepositoryException {
        Query query = buildQuery(nodeName, ntName);
        NodeIterator res = query.execute().getNodes();
        if (res.hasNext()) {
            return res.nextNode();
        }
        return null;
    }
    
    /**
     * @inheritDoc
     */
    @Override
    public Node findNode(Name propertyName, String value, Name ntName) throws RepositoryException {
        Query query = buildQuery(value, Collections.singleton(propertyName), ntName, true, 1);
        NodeIterator res = query.execute().getNodes();
        if (res.hasNext()) {
            return res.nextNode();
        }
        return null;
    }

    /**
     * Search nodes. Take the arguments as search criteria.
     * The queried value has to be a string fragment of one of the Properties
     * contained in the given set. And the node have to be of a requested nodetype
     *
     * @param propertyNames
     * @param value
     * @param ntName NodeType the hits have to have
     * @param exact  if <code>true</code> match must be exact
     * @return
     * @throws javax.jcr.RepositoryException
     */
    @Override
    public NodeIterator findNodes(Set<Name> propertyNames, String value, Name ntName,
                                  boolean exact, long maxSize) throws RepositoryException {
        Query query = buildQuery(value, propertyNames, ntName, exact, maxSize);
        return query.execute().getNodes();
    }

    //--------------------------------------------------------------------------
    /**
     *
     * @param nodeName
     * @param ntName
     * @return
     * @throws RepositoryException
     */
    private Query buildQuery(Name nodeName, Name ntName)
            throws RepositoryException {
        StringBuilder stmt = new StringBuilder("/jcr:root");
        stmt.append(getSearchRoot(ntName));
        stmt.append("//element(");
        stmt.append(ISO9075.encode(getNamePathResolver().getJCRName(nodeName)));
        stmt.append(",");
        stmt.append(getNamePathResolver().getJCRName(ntName));
        stmt.append(")");
        return queryManager.createQuery(stmt.toString(), Query.XPATH);
    }

    /**
     *
     * @param value
     * @param props
     * @param ntName
     * @param exact
     * @param maxSize Currently ignored!
     * @return
     * @throws RepositoryException
     */
    private Query buildQuery(String value, Set<Name> props, Name ntName,
                             boolean exact, long maxSize)
            throws RepositoryException {
        StringBuilder stmt = new StringBuilder("/jcr:root");
        String searchRoot = getSearchRoot(ntName);
        if (!"/".equals(searchRoot)) {
            stmt.append(searchRoot);
        }
        stmt.append("//element(*,");
        stmt.append(getNamePathResolver().getJCRName(ntName));

        if (value == null) {
            stmt.append(")");
        } else {
            stmt.append(")[");
            int i = 0;
            for (Name prop : props) {
                stmt.append((exact) ? "@" : "jcr:like(@");
                String pName = getNamePathResolver().getJCRName(prop);
                stmt.append(ISO9075.encode(pName));
                if (exact) {
                    stmt.append("='");
                    stmt.append(value.replaceAll("'", "''"));
                    stmt.append("'");
                } else {
                    stmt.append(",'%");
                    stmt.append(escapeForQuery(value));
                    stmt.append("%')");
                }
                if (++i < props.size()) {
                    stmt.append(" or ");
                }
            }
            stmt.append("]");
        }
        Query q = queryManager.createQuery(stmt.toString(), Query.XPATH);
        q.setLimit(maxSize);
        return q;
    }

    private static String escapeForQuery(String value) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                ret.append("\\\\");
            } else if (c == '\'') {
                ret.append("''");
            } else {
                ret.append(c);
            }
        }
        return ret.toString();
    }
}
