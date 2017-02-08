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
import java.util.List;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.util.Version;

/**
 * <code>JackrabbitQueryParser</code> extends the standard lucene query parser
 * and adds JCR specific customizations.
 */
public class JackrabbitQueryParser extends QueryParser {

    /**
     * The Jackrabbit synonym provider or <code>null</code> if there is none.
     */
    private final SynonymProvider synonymProvider;

    private final PerQueryCache cache;

    /**
     * Creates a new query parser instance.
     *
     * @param fieldName       the field name.
     * @param analyzer        the analyzer.
     * @param synonymProvider the synonym provider or <code>null</code> if none
     *                        is available.
     */
    public JackrabbitQueryParser(String fieldName,
                                 Analyzer analyzer,
                                 SynonymProvider synonymProvider,
                                 PerQueryCache cache) {
        super(Version.LUCENE_36, fieldName, analyzer);
        this.synonymProvider = synonymProvider;
        this.cache = cache;
        setAllowLeadingWildcard(true);
        // see JCR-3075
        setAutoGeneratePhraseQueries(true);
        setDefaultOperator(Operator.AND);
    }

    /**
     * {@inheritDoc}
     */
    public Query parse(String textsearch) throws ParseException {
        // replace escaped ' with just '
        StringBuffer rewritten = new StringBuffer();
        // the default lucene query parser recognizes 'AND' and 'NOT' as
        // keywords.
        textsearch = textsearch.replaceAll("AND", "and");
        textsearch = textsearch.replaceAll("NOT", "not");
        boolean escaped = false;
        for (int i = 0; i < textsearch.length(); i++) {
            if (textsearch.charAt(i) == '\\') {
                if (escaped) {
                    rewritten.append("\\\\");
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else if (textsearch.charAt(i) == '\'') {
                if (escaped) {
                    escaped = false;
                }
                rewritten.append(textsearch.charAt(i));
            } else if (textsearch.charAt(i) == '~') {
                if (i == 0 || Character.isWhitespace(textsearch.charAt(i - 1))) {
                    // escape tilde so we can use it for similarity query
                    rewritten.append("\\");
                }
                rewritten.append('~');
            } else if (textsearch.charAt(i) == ':') {
                // fields as known in lucene are not supported
                rewritten.append("\\:");
            } else {
                if (escaped) {
                    rewritten.append('\\');
                    escaped = false;
                }
                rewritten.append(textsearch.charAt(i));
            }
        }
        return super.parse(rewritten.toString());
    }

    /**
     * Factory method for generating a synonym query.
     * Called when parser parses an input term token that has the synonym
     * prefix (~term) prepended.
     *
     * @param field Name of the field query will use.
     * @param termStr Term token to use for building term for the query
     *
     * @return Resulting {@link Query} built for the term
     * @exception ParseException throw in overridden method to disallow
     */
    protected Query getSynonymQuery(String field, String termStr)
            throws ParseException {
        List<BooleanClause> synonyms = new ArrayList<BooleanClause>();
        synonyms.add(new BooleanClause(getFieldQuery(field, termStr),
                BooleanClause.Occur.SHOULD));
        if (synonymProvider != null) {
            for (String term : synonymProvider.getSynonyms(termStr)) {
                synonyms.add(new BooleanClause(getFieldQuery(field, term), BooleanClause.Occur.SHOULD));
            }
        }
        if (synonyms.size() == 1) {
            return synonyms.get(0).getQuery();
        } else {
            return getBooleanQuery(synonyms);
        }
    }


    /**
     * {@inheritDoc}
     */
    protected Query getFieldQuery(String field, String queryText)
            throws ParseException {
        return getFieldQuery(field, queryText, true);
    }

    /**
     * {@inheritDoc}
     */
    protected Query getFieldQuery(String field, String queryText, boolean quoted)
            throws ParseException {
        if (queryText.startsWith("~")) {
            // synonym query
            return getSynonymQuery(field, queryText.substring(1));
        } else {
            return super.getFieldQuery(field, queryText, quoted);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected Query getPrefixQuery(String field, String termStr)
            throws ParseException {
        // only create a prefix query when the term is a single word / token
        Analyzer a = getAnalyzer();
        TokenStream ts = a.tokenStream(field, new StringReader(termStr));
        int count = 0;
        boolean isCJ = false;
        try {
            TypeAttribute t = ts.addAttribute(TypeAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                count++;
                isCJ = StandardTokenizer.TOKEN_TYPES[StandardTokenizer.CJ].equals(t.type());
            }
            ts.end();
        } catch (IOException e) {
            throw new ParseException(e.getMessage());
        } finally {
            try {
                ts.close();
            } catch (IOException e) {
                // ignore
            }
        }
        if (count > 1 && isCJ) {
            return getFieldQuery(field, termStr);
        } else {
            return getWildcardQuery(field, termStr + "*");
        }
    }

    /**
     * {@inheritDoc}
     */
    protected Query getWildcardQuery(String field, String termStr)
            throws ParseException {
        if (getLowercaseExpandedTerms()) {
            termStr = termStr.toLowerCase();
        }
        return new WildcardQuery(field, null, translateWildcards(termStr), cache);
    }

    /**
     * Translates unescaped wildcards '*' and '?' into '%' and '_'.
     *
     * @param input the input String.
     * @return the translated String.
     */
    private String translateWildcards(String input) {
        StringBuffer translated = new StringBuffer(input.length());
        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '\\') {
                if (escaped) {
                    translated.append("\\\\");
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else if (input.charAt(i) == '*') {
                if (escaped) {
                    translated.append('*');
                    escaped = false;
                } else {
                    translated.append('%');
                }
            } else if (input.charAt(i) == '?') {
                if (escaped) {
                    translated.append('?');
                    escaped = false;
                } else {
                    translated.append('_');
                }
            } else if (input.charAt(i) == '%' || input.charAt(i) == '_') {
                // escape every occurrence of '%' and '_'
                escaped = false;
                translated.append('\\').append(input.charAt(i));
            } else {
                if (escaped) {
                    translated.append('\\');
                    escaped = false;
                }
                translated.append(input.charAt(i));
            }
        }
        return translated.toString();
    }
}
