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
package org.apache.jackrabbit.spi.commons.name;

import org.apache.jackrabbit.spi.Path;

/**
 * Utility class for matching {@link Pattern}s against {@link Path}es.
 */
public final class Matcher {

    private Matcher() {
        // Don't instantiate
    }

    /**
     * Match a pattern against an input path and return the remaining path.
     * @param pattern
     * @param input
     * @return The remaining path after the match or <code>null</code> if the whole path
     *   was matched.
     * @see MatchResult#getRemainder()
     */
    public static Path match(Pattern pattern, Path input) {
        return pattern.match(input).getRemainder();
    }

    /**
     * Checks whether a pattern matches an input path.
     * @param pattern
     * @param input
     * @return <code>true</code> if <code>pattern</code> matches the whole <code>input</code>.
     * @see MatchResult#isFullMatch()
     */
    public static boolean matches(Pattern pattern, Path input) {
        return pattern.match(input).isFullMatch();
    }

    /**
     * Find the first match of a pattern in a path.
     * @param pattern
     * @param input
     * @return A {@link MatchResult} or null if the pattern does not occur in the
     *   input.
     * @throws IllegalArgumentException if <code>input</code> is not normalized.
     */
    public static MatchResult findMatch(Pattern pattern, Path input) {
        return findMatch(pattern, input, 0);
    }

    /**
     * Find the first match of a pattern in a path starting at a given position.
     * @param pattern
     * @param input
     * @param pos
     * @return A {@link MatchResult} or null if the pattern does not occur in the
     *   input.
     * @throws IllegalArgumentException if <code>input</code> is not normalized.
     */
    public static MatchResult findMatch(Pattern pattern, Path input, int pos) {
        int length = input.getLength();
        if (pos < 0 || pos >= length) {
            throw new IllegalArgumentException("Index out of bounds");
        }

        for (int k = pos; k < length; k++) {
            Path path = input.subPath(k, length);
            MatchResult result = pattern.match(path);
            if (result.isMatch()) {
                return new MatchResult(input, k, result.getMatchLength());
            }
        }
        return null;
    }

}
