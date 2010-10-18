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

import javax.jcr.RepositoryException;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModelFactory;

public class Constraints {

    public static final Constraint TRUE = new Constraint() {};

    public static final Constraint FALSE = new Constraint() {};

    public static Constraint and(
            QueryObjectModelFactory factory, Constraint... constraints)
            throws RepositoryException {
        Constraint constraint = TRUE;
        for (int i = 0; constraints != null && i < constraints.length; i++) {
            if (constraints[i] == FALSE) {
                return FALSE;
            } else if (constraints[i] != TRUE) {
                if (constraint == TRUE) {
                    constraint = constraints[i];
                } else {
                    constraint = factory.and(constraint, constraints[i]);
                }
            }
        }
        return constraint;
    }


    public static Constraint or(
            QueryObjectModelFactory factory, Constraint... constraints)
            throws RepositoryException {
        Constraint constraint = FALSE;
        for (int i = 0; constraints != null && i < constraints.length; i++) {
            if (constraints[i] == TRUE) {
                return TRUE;
            } else if (constraints[i] != FALSE) {
                if (constraint == FALSE) {
                    constraint = constraints[i];
                } else {
                    constraint = factory.or(constraint, constraints[i]);
                }
            }
        }
        return constraint;
    }

    public static Constraint not(
            QueryObjectModelFactory factory, Constraint constraint)
            throws RepositoryException {
        if (constraint == TRUE) {
            return FALSE;
        } else if (constraint == FALSE) {
            return TRUE;
        } else {
            return factory.not(constraint);
        }
    }

}
