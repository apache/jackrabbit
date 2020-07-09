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

package org.apache.jackrabbit.spi.commons.iterator;

/**
 * Utility class containing pre defined {@link Predicate}s
 * @deprecated use instances of {@link java.util.function.Predicate} instead
 */
public final class Predicates {

    /**
     * A predicate which is always true
     */
    public static final Predicate TRUE = new Predicate() {
        public boolean evaluate(Object arg) {
            return true;
        }
    };

    /**
     * A predicate which is always false
     */
    public static final Predicate FALSE = new Predicate() {
        public boolean evaluate(Object arg) {
            return false;
        }
    };

    private Predicates() {
        // no instances allowed
    }

    /**
     * A predicate which is always true
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> TRUE() {
        return TRUE;
    }

    /**
     * A predicate which is always false
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> FALSE() {
        return FALSE;
    }

}
