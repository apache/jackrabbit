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

import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;

public class MatcherTest extends TestCase {
    private final static PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();
    private final static Path PATH1 = PATH_FACTORY.create("{}\t{}a\t{}b\t{}c\t{}d\t{}e\t{}f\t{}g\t{}h\t{}i\t{}j\t{}k");
    private static final Path PATH2 = PATH_FACTORY.create("{}a\t{}a\t{}a\t{}a\t{}a\t{}b");

    public void testFindMatch() throws RepositoryException {
        int k = 1;
        for (char c = 'a'; c <= 'k'; c++) {
            Pattern pattern = Pattern.name("", Character.toString(c));
            MatchResult result = Matcher.findMatch(pattern, PATH1);
            assertEquals("Match @ " + k, k, result.getMatchPos());
            assertEquals("Match @ " + k, PATH1.subPath(k, k + 1), result.getMatch());
            assertEquals("Match @ " + k, c == 'k'? null : PATH1.subPath(k + 1, PATH1.getLength()), result.getRemainder());
            assertEquals("MatchLength == 1", 1, result.getMatchLength());
            k++;
        }
    }

    public void testFindMatchPos() throws RepositoryException {
        Pattern pattern = Pattern.name("", ".");
        int k = 1;
        for (MatchResult result = Matcher.findMatch(pattern, PATH1);
                result.getMatchPos() + 1 < PATH1.getLength();
                result = Matcher.findMatch(pattern, PATH1, result.getMatchPos() + 1)) {

            assertEquals("Match @ " + k, k, result.getMatchPos());
            assertEquals("Match @ " + k, PATH1.subPath(k, k + 1), result.getMatch());
            assertEquals("Match @ " + k, PATH1.subPath(k + 1, PATH1.getLength()), result.getRemainder());
            assertEquals("MatchLength == 1", 1, result.getMatchLength());
            k++;
        }

        pattern = Pattern.name("", "any");
        assertEquals(pattern + " does not match " + PATH1, null, Matcher.findMatch(pattern, PATH1));
    }

    public void testGreedyRepeat() {
        Pattern pattern = Pattern.sequence(
                Pattern.repeat(
                        Pattern.sequence(
                                Pattern.name("", ".*"),
                                Pattern.name("", "a"))),
                Pattern.name("", "b"));

        assertEquals(pattern + " matches " + PATH2 + " @1", 1, Matcher.findMatch(pattern, PATH2).getMatchPos());
    }

}
