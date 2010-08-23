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

/**
 * Static utility class to help working with {@link Predicate}s.
 *
 * @since Apache Jackrabbit 2.2
 */
public class Predicates {

    /**
     * Creates an AND predicate over all the given component predicates.
     * All the component predicates must evaluate to <code>true</code>
     * for the AND predicate to do so.
     *
     * @param predicates component predicates
     * @return AND predicate
     */
    public static Predicate and(final Predicate... predicates) {
        return new Predicate() {
            public boolean evaluate(Object object) {
                for (Predicate predicate : predicates) {
                    if (!predicate.evaluate(object)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Creates an OR predicate over all the given component predicates.
     * At least one of the component predicates must evaluate to
     * <code>true</code> for the OR predicate to do so.
     *
     * @param predicates component predicates
     * @return OR predicate
     */
    public static Predicate or(final Predicate... predicates) {
        return new Predicate() {
            public boolean evaluate(Object object) {
                for (Predicate predicate : predicates) {
                    if (predicate.evaluate(object)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Creates a NOT predicate for the given component predicate.
     * The NOT predicate evaluates to <code>true</code> when the component
     * predicate doesn't, and vice versa.
     *
     * @param predicate component predicate
     * @return NOT predicate
     */
    public static Predicate not(final Predicate predicate) {
        return new Predicate() {
            public boolean evaluate(Object object) {
                return !predicate.evaluate(object);
            }
        };
    }

}
