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

import static javax.jcr.PropertyType.DATE;
import static javax.jcr.PropertyType.DECIMAL;
import static javax.jcr.PropertyType.DOUBLE;
import static javax.jcr.PropertyType.LONG;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LIKE;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO;

import java.util.Comparator;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.core.query.lucene.Util;

/**
 * Comparator for {@link Value} instances.
 */
public class ValueComparator implements Comparator<Value> {

    /**
     * Compares two values.
     */
    public int compare(Value a, Value b) {
        try {
            int ta = a.getType();
            int tb = b.getType();

            if ((ta == DECIMAL || ta == DOUBLE || ta == LONG)
                    && (tb == DECIMAL || tb == DOUBLE || tb == LONG)) {
                return a.getDecimal().compareTo(b.getDecimal());
            } else if (ta == DATE && tb == DATE) {
                return a.getDate().compareTo(b.getDate());
            } else {
                return a.getString().compareTo(b.getString());
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(
                    "Unable to compare values " + a + " and " + b, e);
        }
    }

    /**
     * Evaluates the given QOM comparison operation with the given values.
     *
     * @param operator QOM comparison operator
     * @param a left value
     * @param b right value
     * @return result of the comparison
     */
    public boolean evaluate(String operator, Value a, Value b) {
        if (JCR_OPERATOR_EQUAL_TO.equals(operator)) {
            return compare(a, b) == 0;
        } else if (JCR_OPERATOR_GREATER_THAN.equals(operator)) {
            return compare(a, b) > 0;
        } else if (JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO.equals(operator)) {
            return compare(a, b) >= 0;
        } else if (JCR_OPERATOR_LESS_THAN.equals(operator)) {
            return compare(a, b) < 0;
        } else if (JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO.equals(operator)) {
            return compare(a, b) <= 0;
        } else if (JCR_OPERATOR_NOT_EQUAL_TO.equals(operator)) {
            return compare(a, b) != 0;
        } else if (JCR_OPERATOR_LIKE.equals(operator)) {
            try {
                Pattern pattern = Util.createRegexp(b.getString());
                return pattern.matcher(a.getString()).matches();
            } catch (RepositoryException e) {
                throw new RuntimeException(
                        "Unable to compare values " + a + " and " + b, e);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unknown comparison operator: " + operator);
        }
    }

    /**
     * Evaluates the given QOM comparison operation with the given value arrays.
     *
     * @param operator QOM comparison operator
     * @param a left values
     * @param b right values
     * @return result of the comparison
     */
    public boolean evaluate(String operator, Value[] a, Value[] b) {
        if (JCR_OPERATOR_EQUAL_TO.equals(operator)) {
            for (int i = 0; i < a.length; i++) {
                for (int j = 0; j < b.length; j++) {
                    if (compare(a[i], b[j]) == 0) {
                        return true;
                    }
                }
            }
        }
        return false; // FIXME
    }

}