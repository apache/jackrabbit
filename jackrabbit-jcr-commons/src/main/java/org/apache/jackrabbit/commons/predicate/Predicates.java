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
     * @param <T>
     *
     * @param predicates component predicates
     * @return AND predicate
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    public static <T> Predicate<T> and(final java.util.function.Predicate<? super T>... predicates) {
        return computeAnd(predicates);
    }

    /**
     * Creates an AND predicate over all the given component predicates.
     * All the component predicates must evaluate to <code>true</code>
     * for the AND predicate to do so.
     * @param <T>
     *
     * @param predicates component predicates
     * @return AND predicate
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static <T> Predicate<T> and(Predicate<? super T>... predicates) {
        return computeAnd(predicates);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private static <T> Predicate<T> computeAnd(final java.util.function.Predicate<? super T>... predicates) {
        return new Predicate<T>() {
            public boolean evaluate(T object) {
                for (java.util.function.Predicate<? super T> predicate : predicates) {
                    if (!predicate.test(object)) {
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
    @SuppressWarnings({ "unchecked", "deprecation" })
    public static <T> Predicate<T> or(final java.util.function.Predicate<? super T>... predicates) {
        return computeOr(predicates);
    }

    /**
     * Creates an OR predicate over all the given component predicates.
     * At least one of the component predicates must evaluate to
     * <code>true</code> for the OR predicate to do so.
     *
     * @param predicates component predicates
     * @return OR predicate
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static <T> Predicate<T> or(Predicate<? super T>... predicates) {
        return computeOr(predicates);
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private static <T> Predicate<T> computeOr(java.util.function.Predicate<? super T>... predicates) {
        return new Predicate<T>() {
            public boolean evaluate(T object) {
                for (java.util.function.Predicate<? super T> predicate : predicates) {
                    if (predicate.test(object)) {
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
    public static <T> java.util.function.Predicate<T> not(java.util.function.Predicate<? super T> target) {
        return new java.util.function.Predicate<T>() {
            public boolean test(T object) {
                return !target.test(object);
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
    @SuppressWarnings("deprecation")
    public static <T> Predicate<T> not(Predicate<? super T> target) {
        return new Predicate<T>() {
            public boolean evaluate(T object) {
                return !target.test(object);
            }
        };
    }
}
