/*
 * Copyright 2004 The Apache Software Foundation.
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

import org.apache.lucene.search.*;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.jackrabbit.core.search.PathQueryNode;

import java.io.IOException;

/**
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
class PathQuery extends Query {

    /** The path to query */
    private final String path;

    private final int type;

    private final int index;

    /**
     * Creates a <code>PathQuery</code> for a <code>path</code> and a path
     * <code>type</code>. The query does not care about a specific index.
     * <p>
     * The path <code>type</code> must be one of:
     * <ul>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_EXACT}</li>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_CHILDREN}</li>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_DESCENDANT}</li>
     * </ul>
     *
     * @param path the base path
     * @param type the path type.
     * @throws NullPointerException     if path is null.
     * @throws IllegalArgumentException if type is not one of the defined types
     *                                  in {@link org.apache.jackrabbit.core.search.PathQueryNode}.
     */
    PathQuery(String path, int type) {
	if (path == null) {
	    throw new NullPointerException("path");
	}
	if (type < PathQueryNode.TYPE_EXACT || type > PathQueryNode.TYPE_DESCENDANT) {
	    throw new IllegalArgumentException("type: " + type);
	}
	this.path = path;
	this.type = type;
	index = -1;
    }

    /**
     * Creates a <code>PathQuery</code> for a <code>path</code>, a path
     * <code>type</code> and a position index for the last location step.
     * <p>
     * The path <code>type</code> must be one of:
     * <ul>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_EXACT}</li>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_CHILDREN}</li>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_DESCENDANT}</li>
     * </ul>
     *
     * @param path the base path
     * @param type the path type.
     * @param index position index of the last location step.
     *
     * @throws NullPointerException if path is null.
     * @throws IllegalArgumentException if type is not one of the defined types
     *   in {@link org.apache.jackrabbit.core.search.PathQueryNode}. Or if
     *   <code>index</code> &lt; 1.
     */
    PathQuery(String path, int type, int index) {
	if (path == null) {
	    throw new NullPointerException("path");
	}
	if (type < PathQueryNode.TYPE_EXACT || type > PathQueryNode.TYPE_DESCENDANT) {
	    throw new IllegalArgumentException("type: " + type);
	}
	if (index < 1) {
	    throw new IllegalArgumentException("index: " + index);
	}
	this.path = path;
	this.type = type;
	this.index = index;
    }

    /**
     * Creates a new
     * @param searcher
     * @return
     */
    protected Weight createWeight(Searcher searcher) {
	return new PathQueryWeight(searcher);
    }

    public String toString(String field) {
	return "";
    }

    private class PathQueryWeight implements Weight {

	private final Searcher searcher;
	private float value;
	private float idf;
	private float queryNorm;
	private float queryWeight;


	public PathQueryWeight(Searcher searcher) {
	    this.searcher = searcher;
	}

	public Query getQuery() {
	    return PathQuery.this;
	}

	public float getValue() {
	    return value;
	}

	public float sumOfSquaredWeights() throws IOException {
	    idf = searcher.getSimilarity().idf(searcher.maxDoc(), searcher.maxDoc()); // compute idf
	    queryWeight = idf * getBoost();             // compute query weight
	    return queryWeight * queryWeight;           // square it
	}

	public void normalize(float norm) {
	    this.queryNorm = norm;
	    queryWeight *= queryNorm;                   // normalize query weight
	    value = queryWeight * idf;                  // idf for document
	}

	public Scorer scorer(IndexReader reader) throws IOException {
	    return new PathQueryScorer(this, reader, searcher.getSimilarity());
	}

	public Explanation explain(IndexReader reader, int doc) throws IOException {
	    throw new UnsupportedOperationException();
	}
    }

    private class PathQueryScorer extends Scorer {

	private final Weight weight;

	private final IndexReader reader;

	private final float score;

	protected PathQueryScorer(Weight weight,
				  IndexReader reader,
				  Similarity similarity) {
	    super(similarity);
	    this.weight = weight;
	    this.reader = reader;
	    score = similarity.tf(1) * weight.getValue();
	}

	public void score(HitCollector hc, int maxDoc) throws IOException {
	    TermDocs docs = reader.termDocs();
	    //hc.collect();
	}

	public Explanation explain(int doc) throws IOException {
	    throw new UnsupportedOperationException();
	}
    }
}
