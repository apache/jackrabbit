/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.search.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FilteredTermEnum;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 */
class WildcardTermEnum extends FilteredTermEnum {

    private final Pattern pattern;

    private final String field;

    private boolean endEnum = false;

    public WildcardTermEnum(IndexReader reader, Term term) throws IOException {
        pattern = createRegexp(term.text());
        field = term.field();

        int idx = 0;
        while (idx < term.text().length()
                && Character.isLetterOrDigit(term.text().charAt(idx))) {
            idx++;
        }
        // because IndexReader.terms() starts with the term after the given
        // one start with idx - 1
        if (idx > 0) {
            idx--;
        }
        setEnum(reader.terms(new Term(term.field(), term.text().substring(0, idx))));
    }

    protected boolean termCompare(Term term) {
        if (term.field() == field) {
            return pattern.matcher(term.text()).matches();
        }
        endEnum = true;
        return false;
    }

    protected float difference() {
        return 1.0f;
    }

    protected boolean endEnum() {
        return endEnum;
    }

    //--------------------------< internal >------------------------------------

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
        return Pattern.compile(regexp.toString());
    }
}
