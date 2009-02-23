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
package org.apache.jackrabbit.core.query.lucene;

/**
 * Represents a boolean that implement {@link Comparable}. This class can
 * be removed when we move to Java 5.
 */
public final class ComparableBoolean implements Comparable {

    private static final ComparableBoolean TRUE = new ComparableBoolean(true);

    private static final ComparableBoolean FALSE = new ComparableBoolean(false);

    private final boolean value;

    private ComparableBoolean(boolean value) {
        this.value = value;
    }

    public int compareTo(Object o) {
        ComparableBoolean b = (ComparableBoolean) o;
        return (b.value == value ? 0 : (value ? 1 : -1));
    }

    public static ComparableBoolean valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }
}
