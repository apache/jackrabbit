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

import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.PriorityQueue;

import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.LinkedList;
import java.io.IOException;

/**
 * <code>WeightedHighlighter</code> implements a highlighter that weights the
 * fragments based on the proximity of the highlighted terms to each other. The
 * returned fragments are not necessarily in sequence as the text occurs in the
 * content.
 */
public class WeightedHighlighter extends DefaultHighlighter {

    /**
     * Punctuation characters that mark the end of a sentence.
     */
    private static final BitSet PUNCTUATION = new BitSet();

    static {
        PUNCTUATION.set('.');
        PUNCTUATION.set('!');
        PUNCTUATION.set(0xa1); // inverted exclamation mark
        PUNCTUATION.set('?');
        PUNCTUATION.set(0xbf); // inverted question mark
        // todo add more
    }

    protected WeightedHighlighter() {
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
     *                      fragment.
     * @param hlStart       the string used to prepend a highlighted token, for
     *                      example {@code &quot;&lt;b&gt;&quot;}
     * @param hlEnd         the string used to append a highlighted token, for
     *                      example {@code &quot;&lt;/b&gt;&quot;}
     * @param maxFragments  the maximum number of fragments
     * @param surround      the maximum number of chars surrounding a
     *                      highlighted token
     * @return a String with text fragments where tokens from the query are
     *         highlighted
     */
    public static String highlight(TermPositionVector tvec,
                                   Set<Term[]> queryTerms,
                                   String text,
                                   String excerptStart,
                                   String excerptEnd,
                                   String fragmentStart,
                                   String fragmentEnd,
                                   String hlStart,
                                   String hlEnd,
                                   int maxFragments,
                                   int surround) throws IOException {
        return new WeightedHighlighter().doHighlight(tvec, queryTerms, text,
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
                                   Set<Term[]> queryTerms,
                                   String text,
                                   int maxFragments,
                                   int surround) throws IOException {
        return highlight(tvec, queryTerms, text, START_EXCERPT, END_EXCERPT,
                START_FRAGMENT_SEPARATOR, END_FRAGMENT_SEPARATOR,
                START_HIGHLIGHT, END_HIGHLIGHT, maxFragments, surround);
    }

    @Override
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

        PriorityQueue<FragmentInfo> bestFragments = new FragmentInfoPriorityQueue(maxFragments);
        for (int i = 0; i < offsets.length; i++) {
            if (offsets[i].getEndOffset() <= text.length()) {
                FragmentInfo fi = new FragmentInfo(offsets[i], surround * 2);
                for (int j = i + 1; j < offsets.length; j++) {
                    if (offsets[j].getEndOffset() > text.length()) {
                        break;
                    }
                    if (!fi.add(offsets[j], text)) {
                        break;
                    }
                }
                bestFragments.insertWithOverflow(fi);
            }
        }

        if (bestFragments.size() == 0) {
            return createDefaultExcerpt(text, excerptStart, excerptEnd,
                    fragmentStart, fragmentEnd, surround * 2);
        }

        // retrieve fragment infos from queue and fill into list, least
        // fragment comes out first
        List<FragmentInfo> infos = new LinkedList<FragmentInfo>();
        while (bestFragments.size() > 0) {
            FragmentInfo fi = (FragmentInfo) bestFragments.pop();
            infos.add(0, fi);
        }

        Map<TermVectorOffsetInfo, Object> offsetInfos = new IdentityHashMap<TermVectorOffsetInfo, Object>();
        // remove overlapping fragment infos
        Iterator<FragmentInfo> it = infos.iterator();
        while (it.hasNext()) {
            FragmentInfo fi = it.next();
            boolean overlap = false;
            Iterator<TermVectorOffsetInfo> fit = fi.iterator();
            while (fit.hasNext() && !overlap) {
                TermVectorOffsetInfo oi = fit.next();
                if (offsetInfos.containsKey(oi)) {
                    overlap = true;
                }
            }
            if (overlap) {
                it.remove();
            } else {
                Iterator<TermVectorOffsetInfo> oit = fi.iterator();
                while (oit.hasNext()) {
                    offsetInfos.put(oit.next(), null);
                }
            }
        }

        // create excerpts
        StringBuffer sb = new StringBuffer(excerptStart);
        it = infos.iterator();
        while (it.hasNext()) {
            FragmentInfo fi = it.next();
            sb.append(fragmentStart);
            int limit = Math.max(0, fi.getStartOffset() / 2 + fi.getEndOffset() / 2 - surround);
            int len = startFragment(sb, text, fi.getStartOffset(), limit);
            TermVectorOffsetInfo lastOffsetInfo = null;
            Iterator<TermVectorOffsetInfo> fIt = fi.iterator();
            while (fIt.hasNext()) {
                TermVectorOffsetInfo oi = fIt.next();
                if (lastOffsetInfo != null) {
                    // fill in text between terms
                    sb.append(escape(text.substring(
                            lastOffsetInfo.getEndOffset(), oi.getStartOffset())));
                }
                sb.append(hlStart);
                sb.append(escape(text.substring(oi.getStartOffset(),
                        oi.getEndOffset())));
                sb.append(hlEnd);
                lastOffsetInfo = oi;
            }
            limit = Math.min(text.length(), fi.getStartOffset() - len
                    + (surround * 2));
            endFragment(sb, text, fi.getEndOffset(), limit);
            sb.append(fragmentEnd);
        }
        sb.append(excerptEnd);
        return sb.toString();
    }

    /**
     * Writes the start of a fragment to the string buffer <code>sb</code>. The
     * first occurrence of a matching term is indicated by the
     * <code>offset</code> into the <code>text</code>.
     *
     * @param sb     where to append the start of the fragment.
     * @param text   the original text.
     * @param offset the start offset of the first matching term in the
     *               fragment.
     * @param limit  do not go back further than <code>limit</code>.
     * @return the length of the start fragment that was appended to
     *         <code>sb</code>.
     */
    private int startFragment(StringBuffer sb, String text, int offset, int limit) {
        if (limit == 0) {
            // append all
            sb.append(escape(text.substring(0, offset)));
            return offset;
        }
        String intro = "... ";
        int start = offset;
        for (int i = offset - 1; i >= limit; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                // potential start
                start = i + 1;
                if (i - 1 >= limit && PUNCTUATION.get(text.charAt(i - 1))) {
                    // start of sentence found
                    intro = "";
                    break;
                }
            }
        }
        sb.append(intro).append(escape(text.substring(start, offset)));
        return offset - start;
    }

    /**
     * Writes the end of a fragment to the string buffer <code>sb</code>. The
     * last occurrence of a matching term is indicated by the
     * <code>offset</code> into the <code>text</code>.
     *
     * @param sb     where to append the start of the fragment.
     * @param text   the original text.
     * @param offset the end offset of the last matching term in the fragment.
     * @param limit  do not go further than <code>limit</code>.
     */
    private void endFragment(StringBuffer sb, String text, int offset, int limit) {
        if (limit == text.length()) {
            // append all
            sb.append(escape(text.substring(offset)));
            return;
        }
        int end = offset;
        for (int i = end; i < limit; i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                // potential end
                end = i;
            }
        }
        sb.append(escape(text.substring(offset, end))).append(" ...");
    }

    private static class FragmentInfo {
        List<TermVectorOffsetInfo> offsetInfosList;
        int startOffset;
        int endOffset;
        int maxFragmentSize;
        int quality;

        public FragmentInfo(TermVectorOffsetInfo offsetinfo, int maxFragmentSize) {
            offsetInfosList = new ArrayList<TermVectorOffsetInfo>();
            offsetInfosList.add(offsetinfo);
            startOffset = offsetinfo.getStartOffset();
            endOffset = offsetinfo.getEndOffset();
            this.maxFragmentSize = maxFragmentSize;
            quality = 0;
        }

        public boolean add(TermVectorOffsetInfo offsetinfo, String text) {
            if (offsetinfo.getEndOffset() > (startOffset + maxFragmentSize)) {
                return false;
            }
            offsetInfosList.add(offsetinfo);
            if (offsetinfo.getStartOffset() - endOffset <= 3) {
                // boost quality when terms are adjacent
                // and only separated by whitespace character
                boolean boost = true;
                for (int i = endOffset; i < offsetinfo.getStartOffset(); i++) {
                    if (!Character.isWhitespace(text.charAt(i))) {
                        boost = false;
                        break;
                    }
                }
                if (boost) {
                    quality += 10;
                } else {
                    quality++;
                }
            } else {
                quality++;
            }
            endOffset = offsetinfo.getEndOffset();
            return true;
        }

        public Iterator<TermVectorOffsetInfo> iterator() {
            return offsetInfosList.iterator();
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public int getQuality() {
            return quality;
        }

    }

    private static class FragmentInfoPriorityQueue extends PriorityQueue<FragmentInfo> {

        public FragmentInfoPriorityQueue(int size) {
            initialize(size);
        }

        /**
         * Checks the quality of two {@link FragmentInfo} objects. The one with
         * the lower quality is considered less than the other. If both
         * fragments have the same quality, the one with the higher start offset
         * is considered the lesser. This will result in a queue that keeps the
         * {@link FragmentInfo} with the best quality.
         */
        @Override
        protected boolean lessThan(FragmentInfo infoA, FragmentInfo infoB) {
            if (infoA.getQuality() == infoB.getQuality()) {
                return infoA.getStartOffset() > infoB.getStartOffset();
            }
            return infoA.getQuality() < infoB.getQuality();
        }
    }
}
