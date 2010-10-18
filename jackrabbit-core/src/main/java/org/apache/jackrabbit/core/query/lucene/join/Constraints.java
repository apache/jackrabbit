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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModelFactory;

public class Constraints {

    public static Constraint and(
            QueryObjectModelFactory factory, List<Constraint> constraints)
            throws RepositoryException {
        int n = constraints.size();
        if (n == 0) {
            return null;
        } else if (n == 1) {
            return constraints.get(0);
        } else {
            int m = n / 2;
            return factory.and(
                    and(factory, constraints.subList(0, m)),
                    and(factory, constraints.subList(m, n)));
        }
    }

    public static Constraint and(
            QueryObjectModelFactory factory, Constraint... constraints)
            throws RepositoryException {
        List<Constraint> list = new ArrayList<Constraint>(constraints.length);
        for (Constraint constraint : constraints) {
            if (constraint != null) {
                list.add(constraint);
            }
        }
        return and(factory, list);
    }

    public static Constraint or(
            QueryObjectModelFactory factory, List<Constraint> constraints)
            throws RepositoryException {
        int n = constraints.size();
        if (n == 0) {
            return null;
        } else if (n == 1) {
            return constraints.get(0);
        } else {
            int m = n / 2;
            return factory.or(
                    or(factory, constraints.subList(0, m)),
                    or(factory, constraints.subList(m, n)));
        }
    }

}
