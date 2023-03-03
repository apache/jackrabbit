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

import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.Version;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;

import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.io.IOException;
import java.io.Reader;
import java.io.File;
import java.io.StringReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;


/**
 * Generate "more like this" similarity queries.
 * Based on this mail:
 * <pre>
 * Lucene does let you access the document frequency of terms, with IndexReader.docFreq().
 * Term frequencies can be computed by re-tokenizing the text, which, for a single document,
 * is usually fast enough.  But looking up the docFreq() of every term in the document is
 * probably too slow.
 *
 * You can use some heuristics to prune the set of terms, to avoid calling docFreq() too much,
 * or at all.  Since you're trying to maximize a tf*idf score, you're probably most interested
 * in terms with a high tf. Choosing a tf threshold even as low as two or three will radically
 * reduce the number of terms under consideration.  Another heuristic is that terms with a
 * high idf (i.e., a low df) tend to be longer.  So you could threshold the terms by the
 * number of characters, not selecting anything less than, e.g., six or seven characters.
 * With these sorts of heuristics you can usually find small set of, e.g., ten or fewer terms
 * that do a pretty good job of characterizing a document.
 *
 * It all depends on what you're trying to do.  If you're trying to eek out that last percent
 * of precision and recall regardless of computational difficulty so that you can win a TREC
 * competition, then the techniques I mention above are useless.  But if you're trying to
 * provide a "more like this" button on a search results page that does a decent job and has
 * good performance, such techniques might be useful.
 *
 * An efficient, effective "more-like-this" query generator would be a great contribution, if
 * anyone's interested.  I'd imagine that it would take a Reader or a String (the document's
 * text), analyzer Analyzer, and return a set of representative terms using heuristics like those
 * above.  The frequency and length thresholds could be parameters, etc.
 *
 * Doug
 * </pre>
 *
 *
 * <p>
 * <h2>Initial Usage</h2>
 *
 * This class has lots of options to try to make it efficient and flexible.
 *
 * <pre>
 *
 * IndexReader ir = ...
 * IndexSearcher is = ...
 * <b>
 * MoreLikeThis mlt = new MoreLikeThis(ir);
 * Reader target = ... </b><em>// orig source of doc you want to find similarities to</em><b>
 * Query query = mlt.like( target);
 * </b>
 * Hits hits = is.search(query);
 * <em>// now the usual iteration thru 'hits' - the only thing to watch for is to make sure
 * you ignore the doc if it matches your 'target' document, as it should be similar to itself </em>
 *
 * </pre>
 *
 * Thus you:
 * <ol>
 * <li> do your normal, Lucene setup for searching,
 * <li> create a MoreLikeThis,
 * <li> get the text of the doc you want to find similaries to
 * <li> then call one of the like() calls to generate a similarity query
 * <li> call the searcher to find the similar docs
 * </ol>
 *
 * <h2>More Advanced Usage</h2>
 *
 * You may want to use {@link #setFieldNames setFieldNames(...)} so you can examine
 * multiple fields (e.g. body and title) for similarity.
 * <p>
 *
 * Depending on the size of your index and the size and makeup of your documents you
 * may want to call the other set methods to control how the similarity queries are
 * generated:
 * <ul>
 * <li> {@link #setMinTermFreq setMinTermFreq(...)}
 * <li> {@link #setMinDocFreq setMinDocFreq(...)}
 * <li> {@link #setMinWordLen setMinWordLen(...)}
 * <li> {@link #setMaxWordLen setMaxWordLen(...)}
 * <li> {@link #setMaxQueryTerms setMaxQueryTerms(...)}
 * <li> {@link #setMaxNumTokensParsed setMaxNumTokensParsed(...)}
 * <li> {@link #setStopWords setStopWord(...)}
 * </ul>
 *
 * <hr>
 * <pre>
 * Changes: Mark Harwood 29/02/04
 * Some bugfixing, some refactoring, some optimisation.
 *  - bugfix: retrieveTerms(int docNum) was not working for indexes without a termvector -added missing code
 *  - bugfix: No significant terms being created for fields with a termvector - because
 *            was only counting one occurence per term/field pair in calculations(ie not including frequency info from TermVector)
 *  - refactor: moved common code into isNoiseWord()
 *  - optimise: when no termvector support available - used maxNumTermsParsed to limit amount of tokenization
 * </pre>
 *
 */
public final class MoreLikeThis {

    /**
     * Default maximum number of tokens to parse in each example doc field that is not stored with TermVector support.
     * @see #getMaxNumTokensParsed
     */
    public static final int DEFAULT_MAX_NUM_TOKENS_PARSED = 5000;

    /**
     * Default analyzer to parse source doc with.
     * @see #getAnalyzer
     */
    public static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer(Version.LUCENE_36);

    /**
     * Ignore terms with less than this frequency in the source doc.
     * @see #getMinTermFreq
     * @see #setMinTermFreq
     */
    public static final int DEFAULT_MIN_TERM_FREQ = 2;

    /**
     * Ignore words which do not occur in at least this many docs.
     * @see #getMinDocFreq
     * @see #setMinDocFreq
     */
    public static final int DEFAULT_MIN_DOC_FREQ = 5;

    /**
     * Boost terms in query based on score.
     * @see #isBoost
     * @see #setBoost
     */
    public static final boolean DEFAULT_BOOST = false;

    /**
     * Default field names. Null is used to specify that the field names should be looked
     * up at runtime from the provided reader.
     */
    public static final String[] DEFAULT_FIELD_NAMES = new String[] { "contents"};

    /**
     * Ignore words less than this length or if 0 then this has no effect.
     * @see #getMinWordLen
     * @see #setMinWordLen
     */
    public static final int DEFAULT_MIN_WORD_LENGTH = 0;

    /**
     * Ignore words greater than this length or if 0 then this has no effect.
     * @see #getMaxWordLen
     * @see #setMaxWordLen
     */
    public static final int DEFAULT_MAX_WORD_LENGTH = 0;

    /**
     * Default set of stopwords.
     * If null means to allow stop words.
     *
     * @see #setStopWords
     * @see #getStopWords
     */
    public static final Set<String> DEFAULT_STOP_WORDS = null;

    /**
     * Current set of stop words.
     */
    private Set<String> stopWords = DEFAULT_STOP_WORDS;

    /**
     * Return a Query with no more than this many terms.
     *
     * @see BooleanQuery#getMaxClauseCount
     * @see #getMaxQueryTerms
     * @see #setMaxQueryTerms
     */
    public static final int DEFAULT_MAX_QUERY_TERMS = 25;

    /**
     * Analyzer that will be used to parse the doc.
     */
    private Analyzer analyzer = DEFAULT_ANALYZER;

    /**
     * Ignore words less freqent that this.
     */
    private int minTermFreq = DEFAULT_MIN_TERM_FREQ;

    /**
     * Ignore words which do not occur in at least this many docs.
     */
    private int minDocFreq = DEFAULT_MIN_DOC_FREQ;

    /**
     * Should we apply a boost to the Query based on the scores?
     */
    private boolean boost = DEFAULT_BOOST;

    /**
     * Field name we'll analyze.
     */
    private String[] fieldNames = DEFAULT_FIELD_NAMES;

    /**
     * The maximum number of tokens to parse in each example doc field that is not stored with TermVector support
     */
    private int maxNumTokensParsed = DEFAULT_MAX_NUM_TOKENS_PARSED;

    /**
     * Ignore words if less than this len.
     */
    private int minWordLen = DEFAULT_MIN_WORD_LENGTH;

    /**
     * Ignore words if greater than this len.
     */
    private int maxWordLen = DEFAULT_MAX_WORD_LENGTH;

    /**
     * Don't return a query longer than this.
     */
    private int maxQueryTerms = DEFAULT_MAX_QUERY_TERMS;

    /**
     * For idf() calculations.
     */
    private Similarity similarity;// = new DefaultSimilarity();

    /**
     * IndexReader to use
     */
    private final IndexReader ir;

    /**
     * Constructor requiring an IndexReader.
     */
    public MoreLikeThis(IndexReader ir) {
        this(ir, new DefaultSimilarity());
    }

    public MoreLikeThis(IndexReader ir, Similarity sim){
      this.ir = ir;
      this.similarity = sim;
    }


  public Similarity getSimilarity() {
    return similarity;
  }

  public void setSimilarity(Similarity similarity) {
    this.similarity = similarity;
  }

  /**
     * Returns an analyzer that will be used to parse source doc with. The default analyzer
     * is the {@link #DEFAULT_ANALYZER}.
     *
     * @return the analyzer that will be used to parse source doc with.
     * @see #DEFAULT_ANALYZER
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * Sets the analyzer to use. An analyzer is not required for generating a query with the
     * {@link #like(int)} method, all other 'like' methods require an analyzer.
     *
     * @param analyzer the analyzer to use to tokenize text.
     */
    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Returns the frequency below which terms will be ignored in the source doc. The default
     * frequency is the {@link #DEFAULT_MIN_TERM_FREQ}.
     *
     * @return the frequency below which terms will be ignored in the source doc.
     */
    public int getMinTermFreq() {
        return minTermFreq;
    }

    /**
     * Sets the frequency below which terms will be ignored in the source doc.
     *
     * @param minTermFreq the frequency below which terms will be ignored in the source doc.
     */
    public void setMinTermFreq(int minTermFreq) {
        this.minTermFreq = minTermFreq;
    }

    /**
     * Returns the frequency at which words will be ignored which do not occur in at least this
     * many docs. The default frequency is {@link #DEFAULT_MIN_DOC_FREQ}.
     *
     * @return the frequency at which words will be ignored which do not occur in at least this
     * many docs.
     */
    public int getMinDocFreq() {
        return minDocFreq;
    }

    /**
     * Sets the frequency at which words will be ignored which do not occur in at least this
     * many docs.
     *
     * @param minDocFreq the frequency at which words will be ignored which do not occur in at
     * least this many docs.
     */
    public void setMinDocFreq(int minDocFreq) {
        this.minDocFreq = minDocFreq;
    }

    /**
     * Returns whether to boost terms in query based on "score" or not. The default is
     * {@link #DEFAULT_BOOST}.
     *
     * @return whether to boost terms in query based on "score" or not.
     * @see #setBoost
     */
    public boolean isBoost() {
        return boost;
    }

    /**
     * Sets whether to boost terms in query based on "score" or not.
     *
     * @param boost true to boost terms in query based on "score", false otherwise.
     * @see #isBoost
     */
    public void setBoost(boolean boost) {
        this.boost = boost;
    }

    /**
     * Returns the field names that will be used when generating the 'More Like This' query.
     * The default field names that will be used is {@link #DEFAULT_FIELD_NAMES}.
     *
     * @return the field names that will be used when generating the 'More Like This' query.
     */
    public String[] getFieldNames() {
        return fieldNames;
    }

    /**
     * Sets the field names that will be used when generating the 'More Like This' query.
     * Set this to null for the field names to be determined at runtime from the IndexReader
     * provided in the constructor.
     *
     * @param fieldNames the field names that will be used when generating the 'More Like This'
     * query.
     */
    public void setFieldNames(String[] fieldNames) {
        this.fieldNames = fieldNames;
    }

    /**
     * Returns the minimum word length below which words will be ignored. Set this to 0 for no
     * minimum word length. The default is {@link #DEFAULT_MIN_WORD_LENGTH}.
     *
     * @return the minimum word length below which words will be ignored.
     */
    public int getMinWordLen() {
        return minWordLen;
    }

    /**
     * Sets the minimum word length below which words will be ignored.
     *
     * @param minWordLen the minimum word length below which words will be ignored.
     */
    public void setMinWordLen(int minWordLen) {
        this.minWordLen = minWordLen;
    }

    /**
     * Returns the maximum word length above which words will be ignored. Set this to 0 for no
     * maximum word length. The default is {@link #DEFAULT_MAX_WORD_LENGTH}.
     *
     * @return the maximum word length above which words will be ignored.
     */
    public int getMaxWordLen() {
        return maxWordLen;
    }

    /**
     * Sets the maximum word length above which words will be ignored.
     *
     * @param maxWordLen the maximum word length above which words will be ignored.
     */
    public void setMaxWordLen(int maxWordLen) {
        this.maxWordLen = maxWordLen;
    }

    /**
     * Set the set of stopwords.
     * Any word in this set is considered "uninteresting" and ignored.
     * Even if your Analyzer allows stopwords, you might want to tell the MoreLikeThis code to ignore them, as
     * for the purposes of document similarity it seems reasonable to assume that "a stop word is never interesting".
     *
     * @param stopWords set of stopwords, if null it means to allow stop words
     *
     * @see org.apache.lucene.analysis.StopFilter#makeStopSet StopFilter.makeStopSet()
     * @see #getStopWords
     */
    public void setStopWords(Set<String> stopWords) {
        this.stopWords = stopWords;
    }

    /**
     * Get the current stop words being used.
     * @see #setStopWords
     */
    public Set<String> getStopWords() {
        return stopWords;
    }

    /**
     * Returns the maximum number of query terms that will be included in any generated query.
     * The default is {@link #DEFAULT_MAX_QUERY_TERMS}.
     *
     * @return the maximum number of query terms that will be included in any generated query.
     */
    public int getMaxQueryTerms() {
        return maxQueryTerms;
    }

    /**
     * Sets the maximum number of query terms that will be included in any generated query.
     *
     * @param maxQueryTerms the maximum number of query terms that will be included in any
     * generated query.
     */
    public void setMaxQueryTerms(int maxQueryTerms) {
        this.maxQueryTerms = maxQueryTerms;
    }

    /**
     * @return The maximum number of tokens to parse in each example doc field that is not stored with TermVector support
     * @see #DEFAULT_MAX_NUM_TOKENS_PARSED
     */
    public int getMaxNumTokensParsed() {
        return maxNumTokensParsed;
    }

    /**
     * @param i The maximum number of tokens to parse in each example doc field that is not stored with TermVector support
     */
    public void setMaxNumTokensParsed(int i) {
        maxNumTokensParsed = i;
    }

    /**
     * Return a query that will return docs like the passed lucene document ID.
     *
     * @param docNum the documentID of the lucene doc to generate the 'More Like This" query for.
     * @return a query that will return docs like the passed lucene document ID.
     */
    public Query like(int docNum) throws IOException {
        if (fieldNames == null) {
            // gather list of valid fields from lucene
            Collection<String> fields = ReaderUtil.getIndexedFields(ir);
            fieldNames = fields.toArray(new String[fields.size()]);
        }

        return createQuery(retrieveTerms(docNum));
    }

    /**
     * Return a query that will return docs like the passed file.
     *
     * @return a query that will return docs like the passed file.
     */
    public Query like(File f) throws IOException {
        if (fieldNames == null) {
            // gather list of valid fields from lucene
            Collection<String> fields = ReaderUtil.getIndexedFields(ir);
            fieldNames = fields.toArray(new String[fields.size()]);
        }

        return like(new FileReader(f));
    }

    /**
     * Return a query that will return docs like the passed URL.
     *
     * @return a query that will return docs like the passed URL.
     */
    public Query like(URL u) throws IOException {
        return like(new InputStreamReader(u.openConnection().getInputStream()));
    }

    /**
     * Return a query that will return docs like the passed stream.
     *
     * @return a query that will return docs like the passed stream.
     */
    public Query like(java.io.InputStream is) throws IOException {
        return like(new InputStreamReader(is));
    }

    /**
     * Return a query that will return docs like the passed Reader.
     *
     * @return a query that will return docs like the passed Reader.
     */
    public Query like(Reader r) throws IOException {
        return createQuery(retrieveTerms(r));
    }

    /**
     * Create the More like query from a PriorityQueue
     */
    private Query createQuery(PriorityQueue q) {
        BooleanQuery query = new BooleanQuery();
        Object cur;
        int qterms = 0;
        float bestScore = 0;

        while (((cur = q.pop()) != null)) {
            Object[] ar = (Object[]) cur;
            TermQuery tq = new JackrabbitTermQuery(new Term((String) ar[1], (String) ar[0]));

            if (boost) {
                if (qterms == 0) {
                    bestScore = ((Float) ar[2]).floatValue();
                }
                float myScore = ((Float) ar[2]).floatValue();

                tq.setBoost(myScore / bestScore);
            }

            try {
                query.add(tq, BooleanClause.Occur.SHOULD);
            }
            catch (BooleanQuery.TooManyClauses ignore) {
                break;
            }

            qterms++;
            if (maxQueryTerms > 0 && qterms >= maxQueryTerms) {
                break;
            }
        }

        return query;
    }

    /**
     * Create a PriorityQueue from a word->tf map.
     *
     * @param words a map of words keyed on the word(String) with Int objects as the values.
     */
    private PriorityQueue createQueue(Map<String, Int> words) throws IOException {
        // have collected all words in doc and their freqs
        int numDocs = ir.numDocs();
        FreqQ res = new FreqQ(words.size()); // will order words by score

        Iterator<Map.Entry<String, Int>> it = words.entrySet().iterator();
        while (it.hasNext()) { // for every word
        	Map.Entry<String, Int> entry = it.next();
            String word = entry.getKey();

            int tf = entry.getValue().x; // term freq in the source doc
            if (minTermFreq > 0 && tf < minTermFreq) {
                continue; // filter out words that don't occur enough times in the source
            }

            // go through all the fields and find the largest document frequency
            String topField = fieldNames[0];
            int docFreq = 0;
            for (int i = 0; i < fieldNames.length; i++) {
                int freq = ir.docFreq(new Term(fieldNames[i], word));
                topField = (freq > docFreq) ? fieldNames[i] : topField;
                docFreq = (freq > docFreq) ? freq : docFreq;
            }

            if (minDocFreq > 0 && docFreq < minDocFreq) {
                continue; // filter out words that don't occur in enough docs
            }

            if (docFreq == 0) {
                continue; // index update problem?
            }

            float idf = similarity.idf(docFreq, numDocs);
            float score = tf * idf;

            // only really need 1st 3 entries, other ones are for troubleshooting
            res.insertWithOverflow(new Object[]{word,                   // the word
                                    topField,               // the top field
                                    new Float(score),       // overall score
                                    new Float(idf),         // idf
                                    new Integer(docFreq),   // freq in all docs
                                    new Integer(tf)
            });
        }
        return res;
    }

    /**
     * Describe the parameters that control how the "more like this" query is formed.
     */
    public String describeParams() {
        StringBuffer sb = new StringBuffer();
        sb.append("\tmaxQueryTerms  : ").append(maxQueryTerms).append("\n");
        sb.append("\tminWordLen     : ").append(minWordLen).append("\n");
        sb.append("\tmaxWordLen     : ").append(maxWordLen).append("\n");
        sb.append("\tfieldNames     : ");
        String delim = "";
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            sb.append(delim).append(fieldName);
            delim = ", ";
        }
        sb.append("\n");
        sb.append("\tboost          : ").append(boost).append("\n");
        sb.append("\tminTermFreq    : ").append(minTermFreq).append("\n");
        sb.append("\tminDocFreq     : ").append(minDocFreq).append("\n");
        return sb.toString();
    }

    /**
     * Find words for a more-like-this query former.
     *
     * @param docNum the id of the lucene document from which to find terms
     */
    public PriorityQueue retrieveTerms(int docNum) throws IOException {
        Map<String, Int> termFreqMap = new HashMap<String, Int>();
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            TermFreqVector vector = ir.getTermFreqVector(docNum, fieldName);

            // field does not store term vector info
            if (vector == null) {
                Document d = ir.document(docNum);
                String[] text = d.getValues(fieldName);
                if (text != null) {
                    for (int j = 0; j < text.length; j++) {
                        addTermFrequencies(new StringReader(text[j]), termFreqMap, fieldName);
                    }
                }
            }
            else {
                addTermFrequencies(termFreqMap, vector);
            }

        }

        return createQueue(termFreqMap);
    }

    /**
     * Adds terms and frequencies found in vector into the Map termFreqMap
     * @param termFreqMap a Map of terms and their frequencies
     * @param vector List of terms and their frequencies for a doc/field
     */
    private void addTermFrequencies(Map<String, Int> termFreqMap, TermFreqVector vector) {
        String[] terms = vector.getTerms();
        int[] freqs = vector.getTermFrequencies();
        for (int j = 0; j < terms.length; j++) {
            String term = terms[j];

            if (isNoiseWord(term)) {
                continue;
            }
            // increment frequency
            Int cnt = (Int) termFreqMap.get(term);
            if (cnt == null) {
                cnt = new Int();
                termFreqMap.put(term, cnt);
                cnt.x = freqs[j];
            }
            else {
                cnt.x += freqs[j];
            }
        }
    }

    /**
     * Adds term frequencies found by tokenizing text from reader into the Map words
     * @param r a source of text to be tokenized
     * @param termFreqMap a Map of terms and their frequencies
     * @param fieldName Used by analyzer for any special per-field analysis
     */
    private void addTermFrequencies(Reader r, Map<String, Int> termFreqMap, String fieldName)
            throws IOException {
        TokenStream ts = analyzer.tokenStream(fieldName, r);
        int tokenCount = 0;
        // for every token
        while (ts.incrementToken()) {
            TermAttribute term = ts.getAttribute(TermAttribute.class);
            String word =  term.term();
            tokenCount++;
            if (tokenCount > maxNumTokensParsed) {
                break;
            }
            if (isNoiseWord(word)) {
                continue;
            }

            // increment frequency
            Int cnt = termFreqMap.get(word);
            if (cnt == null) {
                termFreqMap.put(word, new Int());
            } else {
                cnt.x++;
            }
        }
        ts.end();
        ts.close();
    }

    /** determines if the passed term is likely to be of interest in "more like" comparisons
     *
     * @param term The word being considered
     * @return true if should be ignored, false if should be used in further analysis
     */
    private boolean isNoiseWord(String term) {
        int len = term.length();
        if (minWordLen > 0 && len < minWordLen) {
            return true;
        }
        if (maxWordLen > 0 && len > maxWordLen) {
            return true;
        }
        if (stopWords != null && stopWords.contains( term)) {
            return true;
        }
        return false;
    }


    /**
     * Find words for a more-like-this query former.
     * The result is a priority queue of arrays with one entry for <b>every word</b> in the document.
     * Each array has 6 elements.
     * The elements are:
     * <ol>
     * <li> The word (String)
     * <li> The top field that this word comes from (String)
     * <li> The score for this word (Float)
     * <li> The IDF value (Float)
     * <li> The frequency of this word in the index (Integer)
     * <li> The frequency of this word in the source document (Integer)
     * </ol>
     * This is a somewhat "advanced" routine, and in general only the 1st entry in the array is of interest.
     * This method is exposed so that you can identify the "interesting words" in a document.
     * For an easier method to call see {@link #retrieveInterestingTerms retrieveInterestingTerms()}.
     *
     * @param r the reader that has the content of the document
     * @return the most interesting words in the document ordered by score, with the highest scoring, or best entry, first
     *
     * @see #retrieveInterestingTerms
     */
    public PriorityQueue retrieveTerms(Reader r) throws IOException {
        Map<String, Int> words = new HashMap<String, Int>();
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            addTermFrequencies(r, words, fieldName);
        }
        return createQueue(words);
    }

    /**
     * @see #retrieveInterestingTerms(java.io.Reader)
     */
    public String[] retrieveInterestingTerms(int docNum) throws IOException {
        List<String> al = new ArrayList<String>(maxQueryTerms);
        PriorityQueue pq = retrieveTerms(docNum);
        Object cur;
        int lim = maxQueryTerms; // have to be careful, retrieveTerms returns all words but that's probably not useful to our caller...
        // we just want to return the top words
        while (((cur = pq.pop()) != null) && lim-- > 0) {
            Object[] ar = (Object[]) cur;
            al.add((String) ar[0]); // the 1st entry is the interesting word
        }
        return al.toArray(new String[al.size()]);
    }

    /**
     * Convenience routine to make it easy to return the most interesting words in a document.
     * More advanced users will call {@link #retrieveTerms(java.io.Reader) retrieveTerms()} directly.
     * @param r the source document
     * @return the most interesting words in the document
     *
     * @see #retrieveTerms(java.io.Reader)
     * @see #setMaxQueryTerms
     */
    public String[] retrieveInterestingTerms(Reader r) throws IOException {
        List<String> al = new ArrayList<String>(maxQueryTerms);
        PriorityQueue pq = retrieveTerms(r);
        Object cur;
        int lim = maxQueryTerms; // have to be careful, retrieveTerms returns all words but that's probably not useful to our caller...
        // we just want to return the top words
        while (((cur = pq.pop()) != null) && lim-- > 0) {
            Object[] ar = (Object[]) cur;
            al.add((String) ar[0]); // the 1st entry is the interesting word
        }
        return al.toArray(new String[al.size()]);
    }

    /**
     * PriorityQueue that orders words by score.
     */
    private static class FreqQ extends PriorityQueue {
        FreqQ (int s) {
            initialize(s);
        }

        protected boolean lessThan(Object a, Object b) {
            Object[] aa = (Object[]) a;
            Object[] bb = (Object[]) b;
            Float fa = (Float) aa[2];
            Float fb = (Float) bb[2];
            return fa.floatValue() > fb.floatValue();
        }
    }

    /**
     * Use for frequencies and to avoid renewing Integers.
     */
    private static class Int {
        int x;

        Int() {
            x = 1;
        }
    }


}
