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
package org.apache.jackrabbit.core.query;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * <code>ExcerptTest</code> checks if HTML excerpts are created correctly. The
 * test cases assume the following implementation details:
 * <ul>
 * <li>An excerpt is enclosed with a &lt;div> element</li>
 * <li>A fragment is enclosed with a &lt;span> element</li>
 * <li>Terms are highlighted with a &lt;strong> element</li>
 * <li>The maximum number of fragment created is three</li>
 * <li>The maximum excerpt length is 150 characters</li>
 * <li>A fragment contains at most 75 characters (excluding '... ') before the first term is highlighted</li>
 * <li>At least the following sentence separators are recognized: '.', '!' and '?'</li>
 * <li>If there is additial text after the fragment end ' ...' is appended to the fragment</li>
 * <li>If the fragment starts within a sentence, then the fragment is prefixed with '... '</li>
 * </ul>
 */
public class ExcerptTest extends AbstractQueryTest {

    private static final String EXCERPT_START = "<div><span>";

    private static final String EXCERPT_END = "</span></div>";

    public void testHightlightFirstWord() throws RepositoryException {
        checkExcerpt("jackrabbit bla bla bla",
                "<strong>jackrabbit</strong> bla bla bla",
                "jackrabbit");
    }

    public void testHightlightLastWord() throws RepositoryException {
        checkExcerpt("bla bla bla jackrabbit",
                "bla bla bla <strong>jackrabbit</strong>",
                "jackrabbit");
    }

    public void testHightlightWordBetween() throws RepositoryException {
        checkExcerpt("bla bla jackrabbit bla bla",
                "bla bla <strong>jackrabbit</strong> bla bla",
                "jackrabbit");
    }

    public void testMoreTextDotsAtEnd() throws RepositoryException {
        checkExcerpt("bla bla jackrabbit bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla",
                "bla bla <strong>jackrabbit</strong> bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla ...",
                "jackrabbit");
    }

    public void testMoreTextDotsAtStart() throws RepositoryException {
        checkExcerpt("bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla jackrabbit bla bla bla bla",
                "... bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla <strong>jackrabbit</strong> bla bla bla bla",
                "jackrabbit");
    }

    public void testMoreTextDotsAtStartAndEnd() throws RepositoryException {
        checkExcerpt("bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla jackrabbit bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla",
                "... bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla <strong>jackrabbit</strong> bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla ...",
                "jackrabbit");
    }

    public void testPunctuationStartsFragment() throws RepositoryException {
        checkExcerpt("bla bla bla bla bla bla bla bla. bla bla bla bla bla bla bla bla bla bla bla bla jackrabbit bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla",
                "bla bla bla bla bla bla bla bla bla bla bla bla <strong>jackrabbit</strong> bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla",
                "jackrabbit");
    }

    public void testPunctuationStartsFragmentEndsWithDots() throws RepositoryException {
        checkExcerpt("bla bla bla bla bla bla bla bla. bla bla bla bla bla bla bla bla bla bla bla bla jackrabbit bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla",
                "bla bla bla bla bla bla bla bla bla bla bla bla <strong>jackrabbit</strong> bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla ...",
                "jackrabbit");
    }

    public void testHighlightMultipleTerms() throws RepositoryException {
        checkExcerpt("bla bla bla apache jackrabbit bla bla bla",
                "bla bla bla <strong>apache</strong> <strong>jackrabbit</strong> bla bla bla",
                "apache jackrabbit");
    }

    public void testPreferPhrase() throws RepositoryException {
        checkExcerpt("bla apache bla jackrabbit bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla apache jackrabbit bla bla bla",
                "... bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla <strong>apache</strong> <strong>jackrabbit</strong> bla bla bla</span><span>bla <strong>apache</strong> bla <strong>jackrabbit</strong> bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla ...",
                "apache jackrabbit");
    }

    /**
     * Verifies character encoding on a node property that does not contain any
     * excerpt info
     */
    public void testEncodeIllegalCharsNoHighlights() throws RepositoryException {
        String text = "bla <strong>bla</strong> bla";
        String excerpt = createExcerpt("bla &lt;strong&gt;bla&lt;/strong&gt; bla");
        Node n = testRootNode.addNode(nodeName1);
        n.setProperty("text", text);
        n.setProperty("other", "foo");
        superuser.save();

        String stmt = getStatement("foo");
        QueryResult result = executeQuery(stmt);
        RowIterator rows = result.getRows();
        assertEquals(1, rows.getSize());
        String ex = rows.nextRow().getValue("rep:excerpt(text)").getString();
        assertEquals("Expected " + excerpt + ", but got ", excerpt, ex);
    }

    /**
     * Verifies character encoding on a node property that contains excerpt info
     */
    public void testEncodeIllegalCharsHighlights() throws RepositoryException {
        checkExcerpt("bla <strong>bla</strong> foo",
                "bla &lt;strong&gt;bla&lt;/strong&gt; <strong>foo</strong>",
                "foo");
    }

    /**
     * test for https://issues.apache.org/jira/browse/JCR-3077
     * 
     * when given a quoted phrase, the excerpt should evaluate it whole as a
     * token (not break is down)
     * 
     */
    public void testQuotedPhrase() throws RepositoryException {
        checkExcerpt("one two three four",
                "one <strong>two three</strong> four", "\"two three\"");
    }

    /**
     * Verifies excerpt generation on a node property that does not contain any
     * excerpt info for a quoted phrase
     */
    public void testQuotedPhraseNoMatch() throws RepositoryException {
        String text = "one two three four";
        String excerpt = createExcerpt("one two three four");
        String terms = "\"five six\"";

        Node n = testRootNode.addNode(nodeName1);
        n.setProperty("text", text);
        n.setProperty("other", terms);
        superuser.save();

        String stmt = getStatement(terms);
        QueryResult result = executeQuery(stmt);
        RowIterator rows = result.getRows();
        assertEquals(1, rows.getSize());
        String ex = rows.nextRow().getValue("rep:excerpt(text)").getString();
        assertEquals("Expected " + excerpt + ", but got ", excerpt, ex);
    }

    /**
     * 
     * Verifies excerpt generation on a node property that contains the exact
     * quoted phrase but with scrambled words.
     * 
     * More clearly it actually checks that the order of tokens is respected for
     * a quoted phrase.
     */
    public void testQuotedPhraseNoMatchScrambled() throws RepositoryException {
        String text = "one two three four";
        String excerpt = createExcerpt("one two three four");
        String terms = "\"three two\"";

        Node n = testRootNode.addNode(nodeName1);
        n.setProperty("text", text);
        n.setProperty("other", terms);
        superuser.save();

        String stmt = getStatement(terms);
        QueryResult result = executeQuery(stmt);
        RowIterator rows = result.getRows();
        assertEquals(1, rows.getSize());
        String ex = rows.nextRow().getValue("rep:excerpt(text)").getString();
        assertEquals("Expected " + excerpt + ", but got ", excerpt, ex);
    }
    
    /**
     * Verifies excerpt generation on a node property that does not contain the
     * exact quoted phrase, but contains fragments of it.
     * 
     */
    public void testQuotedPhraseNoMatchGap() throws RepositoryException {
        String text = "one two three four";
        String excerpt = createExcerpt("one two three four");
        String terms = "\"two four\"";

        Node n = testRootNode.addNode(nodeName1);
        n.setProperty("text", text);
        n.setProperty("other", terms);
        superuser.save();

        String stmt = getStatement(terms);
        QueryResult result = executeQuery(stmt);
        RowIterator rows = result.getRows();
        assertEquals(1, rows.getSize());
        String ex = rows.nextRow().getValue("rep:excerpt(text)").getString();
        assertEquals("Expected " + excerpt + ", but got ", excerpt, ex);
    }
    
    /**
     * test for https://issues.apache.org/jira/browse/JCR-3077
     * 
     * JA search acts as a PhraseQuery, thanks to LUCENE-2458. so it should be
     * covered by the QuotedTest search.
     * 
     */
    public void testHighlightJa() throws RepositoryException {

        // http://translate.google.com/#auto|en|%E3%82%B3%E3%83%B3%E3%83%86%E3%83%B3%E3%83%88
        String jContent = "\u30b3\u30fe\u30c6\u30f3\u30c8";
        // http://translate.google.com/#auto|en|%E3%83%86%E3%82%B9%E3%83%88
        String jTest = "\u30c6\u30b9\u30c8";

        String content = "some text with japanese: " + jContent + " (content)"
                + " and " + jTest + " (test).";

        // expected excerpt; note this may change if excerpt providers change
        String expectedExcerpt = "some text with japanese: " + jContent
                + " (content) and <strong>" + jTest + "</strong> (test).";
        checkExcerpt(content, expectedExcerpt, jTest);
    }

    /**
     * test for https://issues.apache.org/jira/browse/JCR-3428
     * 
     * when given an incomplete fulltext search token, the excerpt should
     * highlight the entire matching token
     * 
     */
    public void testEagerMatch() throws RepositoryException {
        checkExcerpt("lorem ipsum dolor sit amet",
                "lorem <strong>ipsum</strong> dolor sit amet", "ipsu*");
    }

    /**
     * @see #testEagerMatch()
     */
    public void testEagerMatch2() throws RepositoryException {
        checkExcerpt("lorem ipsum dolor sit amet",
                "<strong>lorem</strong> <strong>ipsum</strong> dolor sit amet",
                "lorem ipsu*");
    }

    /**
     * @see #testEagerMatch()
     */
    public void testEagerMatch3() throws RepositoryException {
        checkExcerpt("lorem ipsum dolor sit amet",
                "lorem <strong>ipsum</strong> <strong>dolor</strong> sit amet", "ipsu* dolor");
    }

    private void checkExcerpt(String text, String fragmentText, String terms)
            throws RepositoryException {
        String excerpt = createExcerpt(fragmentText);
        createTestData(text);
        String stmt = getStatement(terms);
        QueryResult result = executeQuery(stmt);
        RowIterator rows = result.getRows();
        assertEquals(1, rows.getSize());
        assertEquals(excerpt, getExcerpt(rows.nextRow()));
    }

    private String getStatement(String terms) {
        return testPath + "/*[jcr:contains(., '"+ terms + "')]/rep:excerpt(.)";
    }

    private void createTestData(String text) throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1);
        n.setProperty("text", text);
        superuser.save();
    }

    private String getExcerpt(Row row) throws RepositoryException {
        Value v = row.getValue("rep:excerpt(.)");
        if (v != null) {
            return v.getString();
        } else {
            return null;
        }
    }

    private String createExcerpt(String fragments) {
        return EXCERPT_START + fragments + EXCERPT_END;
    }

    /**
     * <p>
     * Test when there are multiple tokens that match the fulltext search (which
     * will generate multiple lucene terms for the highlighter to use) in the
     * repository but not all of them are present in the current property.
     * </p>
     * 
     */
    public void testMatchMultipleNonMatchingTokens() throws RepositoryException {
        String text = "lorem ipsum";
        String fragmentText = "lorem <strong>ipsum</strong>";
        String terms = "ipsu*";

        String excerpt = createExcerpt(fragmentText);

        // here we'll add more matching garbage data so we have more tokens
        // passed to the highlighter
        Node parent = testRootNode.addNode(nodeName1);
        Node n = parent.addNode("test");
        n.setProperty("text", text);
        testRootNode.addNode(nodeName2).setProperty("foo", "ipsuFoo");
        testRootNode.addNode(nodeName3).setProperty("bar", "ipsuBar");
        superuser.save();
        // --
        String stmt = testPath + "/" + nodeName1 + "//*[jcr:contains(., '"
                + terms + "')]/rep:excerpt(.)";
        QueryResult result = executeQuery(stmt);
        RowIterator rows = result.getRows();
        assertEquals(1, rows.getSize());
        assertEquals(excerpt, getExcerpt(rows.nextRow()));
    }
}
