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

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Node;
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
        assertEquals(excerpt, rows.nextRow().getValue("rep:excerpt(text)").getString());
    }

    public void testEncodeIllegalCharsHighlights() throws RepositoryException {
        String text = "bla <strong>bla</strong> foo";
        String excerpt = createExcerpt("bla &lt;strong&gt;bla&lt;/strong&gt; <strong>foo</strong>");
        Node n = testRootNode.addNode(nodeName1);
        n.setProperty("text", text);
        superuser.save();

        String stmt = getStatement("foo");
        QueryResult result = executeQuery(stmt);
        RowIterator rows = result.getRows();
        assertEquals(1, rows.getSize());
        assertEquals(excerpt, rows.nextRow().getValue("rep:excerpt(text)").getString());
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
        testRootNode.save();
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
}
