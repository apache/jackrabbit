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
import org.apache.jackrabbit.core.search.lucene.PackageFilter;
import org.apache.jackrabbit.core.search.lucene.FilteredScorer;

import java.io.IOException;

/**
 * A PathFilterQuery is a wrapper around a native lucene query and
 * adds a {@link org.apache.jackrabbit.core.search.lucene.PackageFilter} around that query.
 *
 * @version $Revision: 1.3 $, $Date: 2004/01/23 15:55:40 $
 * @author Marcel Reutegger
 */
class PathFilterQuery extends Query {

    /** The lucene <code>Query</code> object, containing the real query */
    private Query delegatee;

    /** The {@link PackageFilter} used for filtering */
    private final PackageFilter filter;

    /**
     * Creates a new <code>PathFilterQuery</code> object.
     *
     * @param delegatee the real lucene query.
     * @param filter the <code>PackageFilter</code> for filtering the result.
     */
    PathFilterQuery(Query delegatee, PackageFilter filter) {
	this.delegatee = delegatee;
	this.filter = filter;
    }

    /**
     * Returns the real lucene <code>Query</code> object.
     * @return the real lucene <code>Query</code> object.
     */
    Query getQuery() {
	return delegatee;
    }

    /**
     * Returns a <code>String</code> representation of a field.
     * @param field the field for which to return a <code>String</code>
     *   representaion.
     * @return a <code>String</code> representation of a field.
     */
    public String toString(String field) {
	return delegatee.toString(field);
    }

    /**
     * Returns the <code>Weight</code> object for this
     * <code>HandleFilterQuery</code>.
     * @param searcher the <code>Searcher</code> to create
     *   <code>Weight</code> object.
     * @return the <code>Weight</code> object for this query.
     * @throws IOException if an error occurs while creating the
     *   <code>Weight</code>.
     */
    public Weight weight(Searcher searcher)
	    throws IOException {
	return new FilteredWeight(searcher);
    }

    /**
     * Returns a clone of this query.
     */
    public Object clone() {
	PathFilterQuery clone = (PathFilterQuery)super.clone();
	clone.delegatee = (Query)this.delegatee.clone();
	return clone;
    }

    /**
     * Creates a <code>Weight</code> object for this query.
     * @param searcher the <code>Searcher</code> to create
     *   <code>Weight</code> object.
     * @return a <code>Weight</code> object for this query.
     */
    protected Weight createWeight(Searcher searcher) {
	return new FilteredWeight(searcher);
    }

    public Query rewrite(IndexReader reader) throws IOException {
	delegatee = delegatee.rewrite(reader);
	return this;
    }

    //--------------< inner class >----------

    /**
     * This is the <code>Weight</code> implementation for this query.
     * It simply wraps the <code>Weight</code> object of the real
     * query and creates a {@link org.apache.jackrabbit.core.search.lucene.FilteredScorer} in {@link #scorer}.
     */
    private class FilteredWeight implements Weight {

	/** the <code>Searcher</code> to create a <code>Weight</code> object */
	private Searcher searcher;

	/** The <code>Weight</code> created by the real query */
	private Weight weight;

	/**
	 * Creates a <code>FilteredWeight</code> with a given
	 * <code>Searcher</code>.
	 * @param searcher the <code>Search</code> to create the
	 *   real <code>Weight</code>.
	 */
	FilteredWeight(Searcher searcher) {
	    this.searcher = searcher;
	}

	/**
	 * @see Weight#explain
	 */
	public Explanation explain(IndexReader reader, int doc) throws IOException {
	    if (weight == null) {
		weight = delegatee.weight(searcher);
	    }
	    return weight.explain(reader, doc);
	}

	/**
	 * @see Weight#getQuery
	 */
	public Query getQuery() {
	    return PathFilterQuery.this;
	}

	/**
	 * @see Weight#getValue
	 */
	public float getValue() {
	    return weight.getValue();
	}

	/**
	 * @see Weight#normalize
	 */
	public void normalize(float norm) {
	    weight.normalize(norm);
	}

	/**
	 * Returns a {@link FilteredScorer} instance, which only
	 * scores hits that are not filtered by the {@link org.apache.jackrabbit.core.search.lucene.PackageFilter}.
	 * @param reader <code>IndexReader</code> for reading from the search
	 *   index.
	 * @return a {@link org.apache.jackrabbit.core.search.lucene.FilteredScorer} instance.
	 * @throws IOException if an error occurs while reading from the
	 *   search index.
	 */
	public Scorer scorer(IndexReader reader) throws IOException {
	    if (weight == null) {
		weight = delegatee.weight(searcher);
	    }
	    return new FilteredScorer(weight.scorer(reader), filter, reader);
	}

	/**
	 * @see Weight#sumOfSquaredWeights
	 */
	public float sumOfSquaredWeights() throws IOException {
	    if (weight == null) {
		weight = delegatee.weight(searcher);
	    }
	    return weight.sumOfSquaredWeights();
	}
    }

}
