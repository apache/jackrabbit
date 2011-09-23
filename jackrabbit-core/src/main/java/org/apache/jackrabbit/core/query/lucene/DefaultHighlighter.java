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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;

import org.apache.lucene.document.Field;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.apache.lucene.index.Term;
import org.apache.jackrabbit.util.Text;

/**
 * This is an adapted version of the <code>FulltextHighlighter</code> posted in
 * issue: <a href="http://issues.apache.org/jira/browse/LUCENE-644">LUCENE-644</a>.
 * <p/>
 * Important: for this highlighter to function properly, field must be stored
 * with token offsets.<br/> Use Field constructor {@link
 * Field#Field(String,String,Field.Store,Field.Index,Field.TermVector)
 * Field(String, String, Field.Store, Field.Index, Field.TermVector)} where the
 * last argument is either {@link Field.TermVector#WITH_POSITIONS_OFFSETS} or
 * {@link org.apache.lucene.document.Field.TermVector#WITH_OFFSETS}
 *
 * @see org.apache.lucene.index.TermPositionVector
 * @see org.apache.lucene.index.TermFreqVector
 */
public class DefaultHighlighter {

    /**
     * A default value of <tt>3</tt>
     */
    public static final int DEFAULT_MAXFRAGMENTS = 3;

    /**
     * A default value of <tt>75</tt>
     */
    public static final int DEFAULT_SURROUND = 75;

    public static final String START_EXCERPT = "<excerpt>";

    public static final String END_EXCERPT = "</excerpt>";

    public static final String START_FRAGMENT_SEPARATOR = "<fragment>";

    public static final String END_FRAGMENT_SEPARATOR = "</fragment>";

    public static final String START_HIGHLIGHT = "<highlight>";

    public static final String END_HIGHLIGHT = "</highlight>";

    protected DefaultHighlighter() {
    }

    /**
     * @param tvec          the term position vector for this hit
     * @param queryTerms    the query terms.
     * @param text          the original text that was used to create the
     *                      tokens.
     * @param excerptStart  this string is prepended to the excerpt
     * @param excerptEnd    this string is appended to the excerpt
     * @param fragmentStart this string is prepended to every fragment
     * @param fragmentEnd   this string is appended to the end of every
     *                      fragement.
     * @param hlStart       the string used to prepend a highlighted token, for
     *                      example <tt>&quot;&lt;b&gt;&quot;</tt>
     * @param hlEnd         the string used to append a highlighted token, for
     *                      example <tt>&quot;&lt;/b&gt;&quot;</tt>
     * @param maxFragments  the maximum number of fragments
     * @param surround      the maximum number of chars surrounding a
     *                      highlighted token
     * @return a String with text fragments where tokens from the query are
     *         highlighted
     */
    public static String highlight(TermPositionVector tvec,
                                   Set queryTerms,
                                   String text,
                                   String excerptStart,
                                   String excerptEnd,
                                   String fragmentStart,
                                   String fragmentEnd,
                                   String hlStart,
                                   String hlEnd,
                                   int maxFragments,
                                   int surround)
            throws IOException {
        return new DefaultHighlighter().doHighlight(tvec, queryTerms, text,
                excerptStart, excerptEnd, fragmentStart, fragmentEnd, hlStart,
                hlEnd, maxFragments, surround);
    }

    /**
     * @param tvec         the term position vector for this hit
     * @param queryTerms   the query terms.
     * @param text         the original text that was used to create the tokens.
     * @param maxFragments the maximum number of fragments
     * @param surround     the maximum number of chars surrounding a highlighted
     *                     token
     * @return a String with text fragments where tokens from the query are
     *         highlighted
     */
    public static String highlight(TermPositionVector tvec,
                                   Set queryTerms,
                                   String text,
                                   int maxFragments,
                                   int surround)
            throws IOException {
        return highlight(tvec, queryTerms, text, START_EXCERPT, END_EXCERPT,
                START_FRAGMENT_SEPARATOR, END_FRAGMENT_SEPARATOR,
                START_HIGHLIGHT, END_HIGHLIGHT, maxFragments, surround);
    }

    /**
     * @see #highlight(TermPositionVector, Set, String, String, String, String, String, String, String, int, int)
     */
    protected String doHighlight(TermPositionVector tvec,
                                 Set queryTerms,
                                 String text,
                                 String excerptStart,
                                 String excerptEnd,
                                 String fragmentStart,
                                 String fragmentEnd,
                                 String hlStart,
                                 String hlEnd,
                                 int maxFragments,
                                 int surround) throws IOException {
        String[] terms = new String[queryTerms.size()];
        Iterator it = queryTerms.iterator();
        for (int i = 0; it.hasNext(); i++) {
            terms[i] = ((Term) it.next()).text();
        }
        ArrayList list = new ArrayList();
        int[] tvecindexes = tvec.indexesOf(terms, 0, terms.length);
        for (int i = 0; i < tvecindexes.length; i++) {
            TermVectorOffsetInfo[] termoffsets = tvec.getOffsets(tvecindexes[i]);
            list.addAll(Arrays.asList(termoffsets));
        }

        TermVectorOffsetInfo[] offsets = (TermVectorOffsetInfo[]) list.toArray(new TermVectorOffsetInfo[list.size()]);
        // sort offsets
        if (terms.length > 1) {
            Arrays.sort(offsets, new TermVectorOffsetInfoSorter());
        }

        return mergeFragments(offsets, text, excerptStart,
                excerptEnd, fragmentStart, fragmentEnd, hlStart, hlEnd,
                maxFragments, surround);
    }

    protected String mergeFragments(TermVectorOffsetInfo[] offsets,
                                    String text,
                                    String excerptStart,
                                    String excerptEnd,
                                    String fragmentStart,
                                    String fragmentEnd,
                                    String hlStart,
                                    String hlEnd,
                                    int maxFragments,
                                    int surround) throws IOException {
        if (offsets == null || offsets.length == 0) {
            // nothing to highlight
            return createDefaultExcerpt(text, excerptStart, excerptEnd,
                    fragmentStart, fragmentEnd, surround * 2);
        }
        int lastOffset = offsets.length; // Math.min(10, offsets.length); // 10 terms is plenty?
        ArrayList fragmentInfoList = new ArrayList();
        if (offsets[0].getEndOffset() <= text.length()) {
            FragmentInfo fi = new FragmentInfo(offsets[0], surround * 2);
            for (int i = 1; i < lastOffset; i++) {
                if (offsets[i].getEndOffset() > text.length()) {
                    break;
                }
                if (fi.add(offsets[i])) {
                    continue;
                }
                fragmentInfoList.add(fi);
                fi = new FragmentInfo(offsets[i], surround * 2);
            }
            fragmentInfoList.add(fi);
        }

        if (fragmentInfoList.isEmpty()) {
            // nothing to highlight
            return createDefaultExcerpt(text, excerptStart, excerptEnd,
                    fragmentStart, fragmentEnd, surround * 2);
        }

        // sort with score
        Collections.sort(fragmentInfoList, new FragmentInfoScoreSorter());

        // extract best fragments
        ArrayList bestFragmentsList = new ArrayList();
        for (int i = 0; i < Math.min(fragmentInfoList.size(), maxFragments); i++) {
            bestFragmentsList.add(fragmentInfoList.get(i));
        }

        // re-sort with positions
        Collections.sort(bestFragmentsList, new FragmentInfoPositionSorter());

        // merge #maxFragments fragments
        StringReader reader = new StringReader(text);
        StringBuffer sb = new StringBuffer(excerptStart);
        int pos = 0;
        char[] cbuf;
        int skip;
        int nextStart;
        int skippedChars;
        int firstWhitespace;
        for (int i = 0; i < bestFragmentsList.size(); i++) {
            FragmentInfo fi = (FragmentInfo) bestFragmentsList.get(i);
            fi.trim();
            nextStart = fi.getStartOffset();
            skip = nextStart - pos;
            if (skip > surround * 2) {
                skip -= surround;
                if (i > 0) {
                    // end last fragment
                    cbuf = new char[surround];
                    reader.read(cbuf, 0, surround);
                    // find last whitespace
                    skippedChars = 1;
                    for (; skippedChars < surround + 1; skippedChars++) {
                        if (Character.isWhitespace(cbuf[surround - skippedChars])) {
                            break;
                        }
                    }
                    pos += surround;
                    if (skippedChars > surround) {
                        skippedChars = surround;
                    }
                    sb.append(escape(new String(cbuf, 0, surround
                            - skippedChars)));
                    sb.append(fragmentEnd);
                }
            }

            if (skip >= surround) {
                if (i > 0) {
                    skip -= surround;
                }
                // skip
                reader.skip((long) skip);
                pos += skip;
            }
            // start fragment
            cbuf = new char[nextStart - pos];
            skippedChars = Math.max(cbuf.length - 1, 0);
            firstWhitespace = skippedChars;
            reader.read(cbuf, 0, nextStart - pos);
            pos += (nextStart - pos);
            sb.append(fragmentStart);
            // find last period followed by whitespace
            if (cbuf.length > 0) {
                for (; skippedChars >= 0; skippedChars--) {
                    if (Character.isWhitespace(cbuf[skippedChars])) {
                        firstWhitespace = skippedChars;
                        if (skippedChars - 1 >= 0
                                && cbuf[skippedChars - 1] == '.') {
                            skippedChars++;
                            break;
                        }
                    }
                }
            }
            boolean sentenceStart = true;
            if (skippedChars == -1) {
                if (pos == cbuf.length) {
                    // this fragment is the start of the text -> skip none
                    skippedChars = 0;
                } else {
                    sentenceStart = false;
                    skippedChars = firstWhitespace + 1;
                }
            }

            if (!sentenceStart) {
                sb.append("... ");
            }
            sb.append(escape(new String(cbuf, skippedChars, cbuf.length
                    - skippedChars)));

            // iterate terms
            for (Iterator iter = fi.iterator(); iter.hasNext();) {
                TermVectorOffsetInfo ti = (TermVectorOffsetInfo) iter.next();
                nextStart = ti.getStartOffset();
                if (nextStart - pos > 0) {
                    cbuf = new char[nextStart - pos];
                    int charsRead = reader.read(cbuf, 0, nextStart - pos);
                    pos += (nextStart - pos);
                    sb.append(escape(new String(cbuf, 0, charsRead)));
                }
                sb.append(hlStart);
                nextStart = ti.getEndOffset();
                // print term
                cbuf = new char[nextStart - pos];
                reader.read(cbuf, 0, nextStart - pos);
                pos += (nextStart - pos);
                sb.append(escape(new String(cbuf)));
                sb.append(hlEnd);
            }
        }
        if (pos != 0) {
            // end fragment
            if (offsets.length > lastOffset) {
                surround = Math.min(offsets[lastOffset].getStartOffset() - pos, surround);
            }
            cbuf = new char[surround];
            skip = reader.read(cbuf, 0, surround);
            boolean EOF = reader.read() == -1;
            if (skip >= 0) {
                if (!EOF) {
                    skippedChars = 1;
                    for (; skippedChars < surround + 1; skippedChars++) {
                        if (Character.isWhitespace(cbuf[surround - skippedChars])) {
                            break;
                        }
                    }
                    if (skippedChars > surround) {
                        skippedChars = surround;
                    }
                } else {
                    skippedChars = 0;
                }
                sb.append(escape(new String(cbuf, 0, EOF ? skip
                        : (surround - skippedChars))));
                if (!EOF) {
                    char lastChar = sb.charAt(sb.length() - 1);
                    if (lastChar != '.' && lastChar != '!' && lastChar != '?') {
                        sb.append(" ...");
                    }
                }
            }
            sb.append(fragmentEnd);
        }
        sb.append(excerptEnd);
        return sb.toString();
    }

    /**
     * Creates a default excerpt with the given text.
     *
     * @param text the text.
     * @param excerptStart the excerpt start.
     * @param excerptEnd the excerpt end.
     * @param fragmentStart the fragment start.
     * @param fragmentEnd the fragment end.
     * @param maxLength the maximum length of the fragment.
     * @return a default excerpt.
     * @throws IOException if an error occurs while reading from the text.
     */
    protected String createDefaultExcerpt(String text,
                                          String excerptStart,
                                          String excerptEnd,
                                          String fragmentStart,
                                          String fragmentEnd,
                                          int maxLength) throws IOException {
        StringReader reader = new StringReader(text);
        StringBuffer excerpt = new StringBuffer(excerptStart);
        excerpt.append(fragmentStart);
        int min = excerpt.length();
        char[] buf = new char[maxLength];
        int len = reader.read(buf);
        StringBuffer tmp = new StringBuffer();
        tmp.append(buf, 0, len);
        if (len == buf.length) {
            for (int i = tmp.length() - 1; i > min; i--) {
                if (Character.isWhitespace(tmp.charAt(i))) {
                    tmp.delete(i, tmp.length());
                    tmp.append(" ...");
                    break;
                }
            }
        }
        excerpt.append(escape(tmp.toString()));
        excerpt.append(fragmentEnd).append(excerptEnd);
        return excerpt.toString();
    }
    
    
    /**
     * Escapes input text suitable for the output format.
     * <p>
     * By default does XML-escaping. Can be overridden for
     * other formats.
     * 
     * @param input raw text.
     * @return text suitably escaped.
     */
    protected String escape(String input) {
        return Text.encodeIllegalXMLCharacters(input);
    }

    private static class FragmentInfo {
        ArrayList offsetInfosList;
        int startOffset;
        int endOffset;
        int mergeGap;
        int numTerms;

        public FragmentInfo(TermVectorOffsetInfo offsetinfo, int mergeGap) {
            offsetInfosList = new ArrayList();
            offsetInfosList.add(offsetinfo);
            startOffset = offsetinfo.getStartOffset();
            endOffset = offsetinfo.getEndOffset();
            this.mergeGap = mergeGap;
            numTerms = 1;
        }

        public boolean add(TermVectorOffsetInfo offsetinfo) {
            if (offsetinfo.getStartOffset() > (endOffset + mergeGap)) {
                return false;
            }
            offsetInfosList.add(offsetinfo);
            numTerms++;
            endOffset = offsetinfo.getEndOffset();
            return true;
        }

        public Iterator iterator() {
            return offsetInfosList.iterator();
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public int numTerms() {
            return numTerms;
        }

        public void trim() {
            int end = startOffset + (mergeGap / 2);
            Iterator it = offsetInfosList.iterator();
            while (it.hasNext()) {
                TermVectorOffsetInfo tvoi = (TermVectorOffsetInfo) it.next();
                if (tvoi.getStartOffset() > end) {
                    it.remove();
                }
            }
        }
    }

    private static class FragmentInfoScoreSorter
            implements java.util.Comparator {
        public int compare(Object o1, Object o2) {
            int s1 = ((FragmentInfo) o1).numTerms();
            int s2 = ((FragmentInfo) o2).numTerms();
            if (s1 == s2) {
                return ((FragmentInfo) o1).getStartOffset() < ((FragmentInfo) o2).getStartOffset() ? -1 : 1;
            }
            return s1 > s2 ? -1 : 1;
        }

        public boolean equals(Object obj) {
            return false;
        }
    }

    private static class FragmentInfoPositionSorter
            implements java.util.Comparator {
        public int compare(Object o1, Object o2) {
            int s1 = ((FragmentInfo) o1).getStartOffset();
            int s2 = ((FragmentInfo) o2).getStartOffset();
            if (s1 == s2) {
                return 0;
            }
            return s1 < s2 ? -1 : 1;
        }

        public boolean equals(Object obj) {
            return false;
        }
    }

    private static class TermVectorOffsetInfoSorter
            implements java.util.Comparator {
        public int compare(Object o1, Object o2) {
            int s1 = ((TermVectorOffsetInfo) o1).getStartOffset();
            int s2 = ((TermVectorOffsetInfo) o2).getStartOffset();
            if (s1 == s2) {
                return 0;
            }
            return s1 < s2 ? -1 : 1;
        }

        public boolean equals(Object obj) {
            return false;
        }
    }

}
