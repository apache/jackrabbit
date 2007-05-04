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
class DefaultHighlighter {

    /**
     * A default value of <tt>3</tt>
     */
    public static final int DEFAULT_MAXFRAGMENTS = 3;

    /**
     * A default value of <tt>80</tt>
     */
    public static final int DEFAULT_SURROUND = 80;

    public static final String START_EXCERPT = "<excerpt>";

    public static final String END_EXCERPT = "</excerpt>";

    public static final String START_FRAGMENT_SEPARATOR = "<fragment>";

    public static final String END_FRAGMENT_SEPARATOR = "</fragment>";

    public static final String EMPTY_EXCERPT = "<excerpt/>";

    private DefaultHighlighter() {
    }

    /**
     * @param tvec       the term position vector for this hit
     * @param queryTerms the query terms.
     * @param text       the original text that was used to create the tokens.
     * @param prepend    the string used to prepend a highlighted token, for
     *                   example <tt>&quot;&lt;b&gt;&quot;</tt>
     * @param append     the string used to append a highlighted token, for
     *                   example <tt>&quot;&lt;/b&gt;&quot;</tt>
     * @return a String with text fragments where tokens from the query are
     *         highlighted
     */
    public static String highlight(TermPositionVector tvec,
                                   Set queryTerms,
                                   String text,
                                   String prepend,
                                   String append)
            throws IOException {
        return highlight(tvec, queryTerms, text, prepend, append,
                DEFAULT_MAXFRAGMENTS, DEFAULT_SURROUND);
    }

    /**
     * @param tvec         the term position vector for this hit
     * @param queryTerms   the query terms.
     * @param text         the original text that was used to create the tokens.
     * @param prepend      the string used to prepend a highlighted token, for
     *                     example <tt>&quot;&lt;b&gt;&quot;</tt>
     * @param append       the string used to append a highlighted token, for
     *                     example <tt>&quot;&lt;/b&gt;&quot;</tt>
     * @param maxFragments the maximum number of fragments
     * @param surround     the maximum number of chars surrounding a highlighted
     *                     token
     * @return a String with text fragments where tokens from the query are
     *         highlighted
     */
    public static String highlight(TermPositionVector tvec,
                                   Set queryTerms,
                                   String text,
                                   String prepend,
                                   String append,
                                   int maxFragments,
                                   int surround)
            throws IOException {
        String[] terms = new String[queryTerms.size()];
        Iterator it = queryTerms.iterator();
        for (int i = 0; it.hasNext(); i++) {
            terms[i] = ((Term) it.next()).text();
        }
        ArrayList list = new ArrayList();
        int[] tvecindexes = tvec.indexesOf(terms, 0, terms.length);
        for (int i = 0; i < tvecindexes.length; i++) {
            TermVectorOffsetInfo[] termoffsets = tvec.getOffsets(tvecindexes[i]);
            for (int ii = 0; ii < termoffsets.length; ii++) {
                list.add(termoffsets[ii]);
            }
        }

        TermVectorOffsetInfo[] offsets = (TermVectorOffsetInfo[]) list.toArray(new TermVectorOffsetInfo[0]);
        // sort offsets
        if (terms.length > 1) {
            java.util.Arrays.sort(offsets, new TermVectorOffsetInfoSorter());
        }

        return mergeFragments(offsets, new StringReader(text), prepend,
                append, maxFragments, surround);
    }

    private static String mergeFragments(TermVectorOffsetInfo[] offsets,
                                         StringReader reader,
                                         String prefix,
                                         String suffix,
                                         int maxFragments,
                                         int surround)
            throws IOException {
        if (offsets == null || offsets.length == 0) {
            // nothing to highlight
            return EMPTY_EXCERPT;
        }
        int lastOffset = offsets.length; // Math.min(10, offsets.length); // 10 terms is plenty?
        ArrayList fragmentInfoList = new ArrayList();
        FragmentInfo fi = new FragmentInfo(offsets[0], surround * 2);
        for (int i = 1; i < lastOffset; i++) {
            if (fi.add(offsets[i])) {
                continue;
            }
            fragmentInfoList.add(fi);
            fi = new FragmentInfo(offsets[i], surround * 2);
        }
        fragmentInfoList.add(fi);

        // sort with score
        java.util.Collections.sort(fragmentInfoList, new FragmentInfoScoreSorter());

        // extract best fragments
        ArrayList bestFragmentsList = new ArrayList();
        for (int i = 0; i < Math.min(fragmentInfoList.size(), maxFragments); i++) {
            bestFragmentsList.add(fragmentInfoList.get(i));
        }

        // re-sort with positions
        java.util.Collections.sort(bestFragmentsList, new FragmentInfoPositionSorter());

        // merge #maxFragments fragments
        StringBuffer sb = new StringBuffer(START_EXCERPT);
        int pos = 0;
        char[] cbuf;
        int skip;
        int nextStart;
        int skippedChars;
        for (int i = 0; i < bestFragmentsList.size(); i++) {
            fi = (FragmentInfo) bestFragmentsList.get(i);
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
                    sb.append(Text.encodeIllegalXMLCharacters(
                            new String(cbuf, 0, surround - skippedChars)));
                    sb.append(END_FRAGMENT_SEPARATOR);
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
            skippedChars = 0;
            cbuf = new char[nextStart - pos];
            reader.read(cbuf, 0, nextStart - pos);
            pos += (nextStart - pos);
            sb.append(START_FRAGMENT_SEPARATOR);
            // find first whitespace
            for (; skippedChars < cbuf.length; skippedChars++) {
                if (Character.isWhitespace(cbuf[skippedChars])) {
                    skippedChars += 1;
                    break;
                }
            }

            sb.append(Text.encodeIllegalXMLCharacters(
                    new String(cbuf, skippedChars, cbuf.length - skippedChars)));

            // iterate terms
            for (Iterator iter = fi.iterator(); iter.hasNext();) {
                TermVectorOffsetInfo ti = (TermVectorOffsetInfo) iter.next();
                nextStart = ti.getStartOffset();
                if (nextStart - pos > 0) {
                    cbuf = new char[nextStart - pos];
                    int charsRead = reader.read(cbuf, 0, nextStart - pos);
                    pos += (nextStart - pos);
                    sb.append(cbuf, 0, charsRead);
                }
                sb.append(prefix);
                nextStart = ti.getEndOffset();
                // print term
                cbuf = new char[nextStart - pos];
                reader.read(cbuf, 0, nextStart - pos);
                pos += (nextStart - pos);
                sb.append(cbuf);
                sb.append(suffix);
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
                sb.append(Text.encodeIllegalXMLCharacters(
                        new String(cbuf, 0, EOF ? skip : (surround - skippedChars))));
                sb.append(END_FRAGMENT_SEPARATOR);
            }
        }
        sb.append(END_EXCERPT);
        return sb.toString();
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
