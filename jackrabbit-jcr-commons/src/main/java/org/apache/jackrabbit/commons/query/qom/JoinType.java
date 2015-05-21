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
package org.apache.jackrabbit.commons.query.qom;

import javax.jcr.RepositoryException;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Source;

/**
 * Enumeration of the JCR 2.0 join types.
 *
 * @since Apache Jackrabbit 2.0
 */
public enum JoinType {

    INNER(QueryObjectModelConstants.JCR_JOIN_TYPE_INNER),

    LEFT(QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER),

    RIGHT(QueryObjectModelConstants.JCR_JOIN_TYPE_RIGHT_OUTER);

    /**
     * JCR name of this join type.
     */
    private final String name;

    private JoinType(String name) {
        this.name = name;
    }

    /**
     * Returns the join of the given sources.
     *
     * @param factory factory for creating the join
     * @param left left join source
     * @param right right join source
     * @param condition join condition
     * @return join
     * @throws RepositoryException if the join can not be created
     */
    public Join join(
            QueryObjectModelFactory factory,
            Source left, Source right, JoinCondition condition)
            throws RepositoryException {
        return factory.join(left, right, name, condition);
    }

    /**
     * Returns the JCR 2.0 name of this join type.
     *
     * @see QueryObjectModelConstants
     * @return JCR name of this join type
     */
    public String toString() {
        return name;
    }

    /**
     * Returns the join type with the given JCR name.
     *
     * @param name JCR name of a join type
     * @return join type with the given name
     * @throws RepositoryException if the given name is unknown
     */
    public static JoinType getJoinTypeByName(String name)
            throws RepositoryException {
        for (JoinType type : JoinType.values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        throw new RepositoryException("Unknown join type name: " + name);
    }

}
