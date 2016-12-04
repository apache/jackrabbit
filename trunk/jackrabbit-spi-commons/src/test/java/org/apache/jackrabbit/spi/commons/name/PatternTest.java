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

import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.Path.Element;

public class PatternTest extends TestCase {
    private static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();
    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();

    private static final Path PATH1 = PATH_FACTORY.create("{}\t{}www\t{}day\t{}com\t{}jcr\t{}1\t{}0");
    private static final Path PATH2 = PATH_FACTORY.create("{}\t{}jackrabbit\t{}apache\t{}org");
    private static final Path PATH3 = PATH_FACTORY.create("{}a\t{}a\t{}a\t{}a\t{}a\t{}b");

    public void testPathPattern() throws RepositoryException {
        int len = PATH1.getLength();
        for (int k = 1; k < len; k++) {
            Pattern pattern = Pattern.path(PATH1.subPath(0, len - k));
            MatchResult result = pattern.match(PATH1);
            assertTrue(errMsg(pattern, PATH1), result.isMatch());
            assertEquals(errMsg(pattern, PATH1), PATH1.subPath(0, len - k), result.getMatch());
            assertEquals(errMsg(pattern, PATH1), PATH1.subPath(len - k, len), result.getRemainder());
            assertEquals(errMsg(pattern, PATH1), PATH1.subPath(len - k, len), Matcher.match(pattern, PATH1));
        }

        Pattern pattern = Pattern.path(PATH1);
        MatchResult result = pattern.match(PATH1);
        assertTrue(errMsg(pattern, PATH1), result.isMatch());
        assertTrue(errMsg(pattern, PATH1), result.isFullMatch());
        assertTrue(errMsg(pattern, PATH1), Matcher.matches(pattern, PATH1));
        assertEquals(errMsg(pattern, PATH1), PATH1, result.getMatch());
        assertEquals(errMsg(pattern, PATH1), null, result.getRemainder());
        assertEquals(errMsg(pattern, PATH1), null, Matcher.match(pattern, PATH1));
    }

    public void testNamePattern() throws RepositoryException {
        Element[] elements = PATH1.getElements();
        Path path = PATH1;
        for (int k = 0; k < elements.length; k++) {
            Pattern pattern = Pattern.name(elements[k].getName());
            MatchResult result = pattern.match(path);
            Path remainder = result.getRemainder();
            assertTrue(errMsg(pattern, path), result.isMatch());
            assertEquals(errMsg(pattern, path), path.subPath(0, 1), result.getMatch());
            if (k + 1 == elements.length) {
                assertTrue(errMsg(pattern, path), result.isFullMatch());
                assertTrue(errMsg(pattern, path), Matcher.matches(pattern, path));
                assertEquals(errMsg(pattern, path), null, remainder);
                assertEquals(errMsg(pattern, path), null, Matcher.match(pattern, path));
            }
            else {
                assertEquals(errMsg(pattern, path), path.subPath(1, path.getLength()), remainder);
                assertEquals(errMsg(pattern, path), path.subPath(1, path.getLength()), Matcher.match(pattern, path));
            }
            path = remainder;
        }
    }

    public void testRegexPattern() {
        Path path = PATH_FACTORY.create("{}goodday");
        Pattern pattern = Pattern.name(".*", "go*d+ay");
        MatchResult result = pattern.match(path);
        assertTrue(errMsg(pattern, path), result.isMatch());
        assertTrue(errMsg(pattern, path), result.isFullMatch());
        assertTrue(errMsg(pattern, path), Matcher.matches(pattern, path));
        assertEquals(errMsg(pattern, path), path, result.getMatch());
        assertEquals(errMsg(pattern, path), null, result.getRemainder());
        assertEquals(errMsg(pattern, path), null, Matcher.match(pattern, path));

        pattern = Pattern.name("", "goodday");
        result = pattern.match(path);
        assertTrue(errMsg(pattern, path), result.isMatch());
        assertTrue(errMsg(pattern, path), result.isFullMatch());
        assertTrue(errMsg(pattern, path), Matcher.matches(pattern, path));
        assertEquals(errMsg(pattern, path), path, result.getMatch());
        assertEquals(errMsg(pattern, path), null, result.getRemainder());
        assertEquals(errMsg(pattern, path), null, Matcher.match(pattern, path));
    }

    public void testAllPattern() {
        Pattern pattern = Pattern.all();
        MatchResult result = pattern.match(PATH1);
        assertTrue(errMsg(pattern, PATH1), result.isMatch());
        assertTrue(errMsg(pattern, PATH1), result.isFullMatch());
        assertTrue(errMsg(pattern, PATH1), Matcher.matches(pattern, PATH1));
        assertEquals(errMsg(pattern, PATH1), PATH1, result.getMatch());
        assertEquals(errMsg(pattern, PATH1), null, result.getRemainder());
        assertEquals(errMsg(pattern, PATH1), null, Matcher.match(pattern, PATH1));
    }

    public void testNothingPattern() {
        Pattern pattern = Pattern.nothing();
        MatchResult result = pattern.match(PATH1);
        assertFalse(errMsg(pattern, PATH1), result.isMatch());
        assertFalse(errMsg(pattern, PATH1), result.isFullMatch());
        assertFalse(errMsg(pattern, PATH1), Matcher.matches(pattern, PATH1));
        assertEquals(errMsg(pattern, PATH1), null, result.getMatch());
        assertEquals(errMsg(pattern, PATH1), PATH1, result.getRemainder());
        assertEquals(errMsg(pattern, PATH1), PATH1, Matcher.match(pattern, PATH1));
    }

    public void testSelectPattern() {
        Pattern pattern = Pattern.selection(
                Pattern.path(PATH2),
                Pattern.path(PATH1));

        MatchResult result = pattern.match(PATH1);
        assertTrue(errMsg(pattern, PATH1), result.isMatch());
        assertTrue(errMsg(pattern, PATH1), result.isFullMatch());
        assertTrue(errMsg(pattern, PATH1), Matcher.matches(pattern, PATH1));
        assertEquals(errMsg(pattern, PATH1), PATH1, result.getMatch());
        assertEquals(errMsg(pattern, PATH1), null, result.getRemainder());
        assertEquals(errMsg(pattern, PATH1), null, Matcher.match(pattern, PATH1));
    }

    public void testOptionalPattern() {
        Pattern pattern = Pattern.selection(
                Pattern.nothing(),
                Pattern.path(PATH1));

        MatchResult result = pattern.match(PATH1);
        assertTrue(errMsg(pattern, PATH1), result.isMatch());
        assertTrue(errMsg(pattern, PATH1), result.isFullMatch());
        assertTrue(errMsg(pattern, PATH1), Matcher.matches(pattern, PATH1));
        assertEquals(errMsg(pattern, PATH1), PATH1, result.getMatch());
        assertEquals(errMsg(pattern, PATH1), null, result.getRemainder());
        assertEquals(errMsg(pattern, PATH1), null, Matcher.match(pattern, PATH1));
    }

    public void testGreedySelection() {
        Path path = PATH_FACTORY.create("{}a\t{}a\t{}a\t{}a\t{}a");
        Path expectedMatch = PATH_FACTORY.create("{}a\t{}a\t{}a");
        Path expectedRemainder = PATH_FACTORY.create("{}a\t{}a");
        Pattern pattern1 = Pattern.path(PATH_FACTORY.create("{}a\t{}a"));
        Pattern pattern2 = Pattern.path(PATH_FACTORY.create("{}a\t{}a\t{}a"));

        Pattern pattern = Pattern.selection(pattern1, pattern2);
        MatchResult result = pattern.match(path);
        assertTrue(errMsg(pattern, path), result.isMatch());
        assertFalse(errMsg(pattern, path), result.isFullMatch());
        assertFalse(errMsg(pattern, path), Matcher.matches(pattern, path));
        assertEquals(errMsg(pattern, path), expectedMatch, result.getMatch());
        assertEquals(errMsg(pattern, path), expectedRemainder, result.getRemainder());
        assertEquals(errMsg(pattern, path), expectedRemainder, Matcher.match(pattern, path));

        pattern = Pattern.selection(pattern2, pattern1);
        result = pattern.match(path);
        assertTrue(errMsg(pattern, path), result.isMatch());
        assertFalse(errMsg(pattern, path), result.isFullMatch());
        assertFalse(errMsg(pattern, path), Matcher.matches(pattern, path));
        assertEquals(errMsg(pattern, path), expectedMatch, result.getMatch());
        assertEquals(errMsg(pattern, path), expectedRemainder, result.getRemainder());
        assertEquals(errMsg(pattern, path), expectedRemainder, Matcher.match(pattern, path));
    }


    public void testSequencePattern() throws RepositoryException {
        int len = PATH1.getLength();
        for (int k = 1; k < len; k++) {
            Pattern pattern = Pattern.sequence(
                    Pattern.path(PATH1.subPath(0, len - k)),
                    Pattern.path(PATH1.subPath(len - k, len)));

            MatchResult result = pattern.match(PATH1);
            assertTrue(errMsg(pattern, PATH1), result.isMatch());
            assertTrue(errMsg(pattern, PATH1), result.isFullMatch());
            assertTrue(errMsg(pattern, PATH1), Matcher.matches(pattern, PATH1));
            assertEquals(errMsg(pattern, PATH1), PATH1, result.getMatch());
            assertEquals(errMsg(pattern, PATH1), null, result.getRemainder());
            assertEquals(errMsg(pattern, PATH1), null, Matcher.match(pattern, PATH1));
        }
    }

    public void testRepeatPattern() throws RepositoryException {
        Pattern pattern = Pattern.repeat(
                Pattern.name(".*", "a"));

        MatchResult result = pattern.match(PATH3);
        assertTrue(errMsg(pattern, PATH3), result.isMatch());
        assertEquals(errMsg(pattern, PATH3), 5, result.getMatchLength());
        assertFalse(errMsg(pattern, PATH3), result.isFullMatch());
        assertEquals(errMsg(pattern, PATH3), PATH3.subPath(0, 5),result.getMatch());
        assertEquals(errMsg(pattern, PATH3), PATH3.subPath(5, PATH3.getLength()), result.getRemainder());
        assertEquals(errMsg(pattern, PATH3), PATH3.subPath(5, PATH3.getLength()), Matcher.match(pattern, PATH3));
    }

    public void testZeroLengthRepeatPattern() throws RepositoryException {
        Pattern pattern = Pattern.sequence(
                Pattern.repeat(
                        Pattern.name("", "any")),
                Pattern.name("", ".*"));

        MatchResult result = pattern.match(PATH3);
        assertTrue(errMsg(pattern, PATH3), result.isMatch());
        assertEquals(errMsg(pattern, PATH3), 1, result.getMatchLength());
        assertFalse(errMsg(pattern, PATH3), result.isFullMatch());
        assertEquals(errMsg(pattern, PATH3), PATH3.subPath(0, 1),result.getMatch());
        assertEquals(errMsg(pattern, PATH3), PATH3.subPath(1, PATH3.getLength()), result.getRemainder());
        assertEquals(errMsg(pattern, PATH3), PATH3.subPath(1, PATH3.getLength()), Matcher.match(pattern, PATH3));
    }

    public void testRepeatPatternWithBounds() {
        for (int i = 0; i <= PATH3.getLength(); i++) {
            for (int j = 0; j <= PATH3.getLength(); j++) {
                Pattern pattern = Pattern.repeat(
                        Pattern.name(".*", "a"), i, j);

                MatchResult result = pattern.match(PATH3);
                if (i <= 5 && 5 <= j) {
                    assertTrue(errMsg(pattern, PATH3), result.isMatch());
                }
                else {
                    assertFalse(errMsg(pattern, PATH3), result.isMatch());
                }
            }
        }
    }

    public void testComplexPattern() {
        Pattern pattern = Pattern.sequence(
                Pattern.selection(
                        Pattern.path(PATH2),
                        Pattern.name("", "")),
                Pattern.selection(
                        Pattern.repeat(Pattern.repeat(Pattern.repeat(
                                Pattern.name(NAME_FACTORY.create("{}www"))))),
                        Pattern.sequence(
                                Pattern.path(PATH_FACTORY.create("{}www\t{}day")),
                                Pattern.all())));

        MatchResult result = pattern.match(PATH1);
        assertTrue(errMsg(pattern, PATH1), result.isMatch());
        assertTrue(errMsg(pattern, PATH1), result.isFullMatch());
        assertTrue(errMsg(pattern, PATH1), Matcher.matches(pattern, PATH1));
        assertEquals(errMsg(pattern, PATH1), PATH1, result.getMatch());
        assertEquals(errMsg(pattern, PATH1), null, result.getRemainder());
        assertEquals(errMsg(pattern, PATH1), null, Matcher.match(pattern, PATH1));
    }

    // -----------------------------------------------------< private >---

    private static String errMsg(Pattern pattern, Path path) {
        return pattern + " matches " + path;
    }

}
