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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FilteredTermEnum;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Implements a wildcard term enum that optionally supports embedded property
 * names in lucene term texts.
 */
class WildcardTermEnum extends FilteredTermEnum {

    /**
     * The pattern matcher.
     */
    private final Matcher pattern;

    /**
     * The lucene field to search.
     */
    private final String field;

    /**
     * The term prefix without wildcards
     */
    private final String prefix;

    /**
     * Flag that indicates the end of the term enum.
     */
    private boolean endEnum = false;

    /**
     * The input for the pattern matcher.
     */
    private final OffsetCharSequence input;

    /**
     * Creates a new <code>WildcardTermEnum</code>.
     *
     * @param reader the index reader.
     * @param field the lucene field to search.
     * @param propName the embedded jcr property name or <code>null</code> if
     *   there is not embedded property name.
     * @param pattern the pattern to match the values.
     * @throws IOException if an error occurs while reading from the index.
     */
    public WildcardTermEnum(IndexReader reader,
                            String field,
                            String propName,
                            String pattern) throws IOException {
        this.field = field;

        int idx = 0;
        while (idx < pattern.length()
                && Character.isLetterOrDigit(pattern.charAt(idx))) {
            idx++;
        }

        if (propName == null) {
            prefix = pattern.substring(0, idx);
        } else {
            prefix = FieldNames.createNamedValue(propName, pattern.substring(0, idx));
        }

        // initialize with prefix as dummy value
        input = new OffsetCharSequence(prefix.length(), prefix);
        this.pattern = createRegexp(pattern.substring(idx)).matcher(input);

        setEnum(reader.terms(new Term(field, prefix)));
    }

    /**
     * @inheritDoc
     */
    protected boolean termCompare(Term term) {
        if (term.field() == field && term.text().startsWith(prefix)) {
            input.setBase(term.text());
            return pattern.reset().matches();
        }
        endEnum = true;
        return false;
    }

    /**
     * @inheritDoc
     */
    protected float difference() {
        return 1.0f;
    }

    /**
     * @inheritDoc
     */
    protected boolean endEnum() {
        return endEnum;
    }

    //--------------------------< internal >------------------------------------

    /**
     * Creates a regexp from <code>likePattern</code>.
     *
     * @param likePattern the pattern.
     * @return the regular expression <code>Pattern</code>.
     */
    private Pattern createRegexp(String likePattern) {
        // - escape all non alphabetic characters
        // - escape constructs like \<alphabetic char> into \\<alphabetic char>
        // - replace non escaped _ % into . and .*
        StringBuffer regexp = new StringBuffer();
        boolean escaped = false;
        for (int i = 0; i < likePattern.length(); i++) {
            if (likePattern.charAt(i) == '\\') {
                if (escaped) {
                    regexp.append("\\\\");
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else {
                if (Character.isLetterOrDigit(likePattern.charAt(i))) {
                    if (escaped) {
                        regexp.append("\\\\").append(likePattern.charAt(i));
                        escaped = false;
                    } else {
                        regexp.append(likePattern.charAt(i));
                    }
                } else {
                    if (escaped) {
                        regexp.append('\\').append(likePattern.charAt(i));
                        escaped = false;
                    } else {
                        switch (likePattern.charAt(i)) {
                            case '_':
                                regexp.append('.');
                                break;
                            case '%':
                                regexp.append(".*");
                                break;
                            default:
                                regexp.append('\\').append(likePattern.charAt(i));
                        }
                    }
                }
            }
        }
        return Pattern.compile(regexp.toString(), Pattern.DOTALL);
    }

    /**
     * CharSequence that applies an offset to a base CharSequence. The base
     * CharSequence can be replaced without creating a new CharSequence.
     */
    private static final class OffsetCharSequence implements CharSequence {

        /**
         * The offset to apply to the base CharSequence
         */
        private final int offset;

        /**
         * The base character sequence
         */
        private CharSequence base;

        /**
         * Creates a new OffsetCharSequence with an <code>offset</code>.
         *
         * @param offset the offset
         * @param base the base CharSequence
         */
        OffsetCharSequence(int offset, CharSequence base) {
            this.offset = offset;
            this.base = base;
        }

        /**
         * Sets a new base sequence.
         *
         * @param base the base character sequence
         */
        public void setBase(CharSequence base) {
            this.base = base;
        }

        /**
         * @inheritDoc
         */
        public int length() {
            return base.length() - offset;
        }

        /**
         * @inheritDoc
         */
        public char charAt(int index) {
            return base.charAt(index + offset);
        }

        /**
         * @inheritDoc
         */
        public CharSequence subSequence(int start, int end) {
            return base.subSequence(start + offset, end + offset);
        }

        /**
         * @inheritDoc
         */
        public String toString() {
            return base.subSequence(offset, base.length()).toString();
        }
    }
}
