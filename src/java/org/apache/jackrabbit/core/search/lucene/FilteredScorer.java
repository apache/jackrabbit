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

import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.index.IndexReader;
import org.apache.jackrabbit.core.search.lucene.PackageFilter;

import java.io.IOException;
import java.util.BitSet;

/**
 * Implements a <code>Scorer</code> which only scores hits,
 * that are not filtered out by the {@link org.apache.jackrabbit.core.search.lucene.PackageFilter}.
 *
 * @version $Revision: 1.1 $, $Date: 2004/01/23 15:55:40 $
 * @author Marcel Reutegger
 * @since gumbear
 */
class FilteredScorer extends Scorer {

    /** The <code>Scorer</code> of the real lucene query */
    private Scorer delegatee;

    /** The {@link org.apache.jackrabbit.core.search.lucene.PackageFilter} for filtering the hits */
    private PackageFilter filter;

    /** The <code>IndexReader</code> for reading from the search index */
    private IndexReader reader;

    /** The <code>HitCollector</code> with filtering facility */
    private FilteredHitCollector collector;

    /**
     * Creates a new <code>FilteredScorer</code> based on a concrete
     * <code>Scorer</code> using a {@link org.apache.jackrabbit.core.search.lucene.PackageFilter}.
     * @param delegatee
     * @param filter
     * @param reader
     */
    FilteredScorer(Scorer delegatee, PackageFilter filter, IndexReader reader) {
	super(Similarity.getDefault());
	this.delegatee = delegatee;
	this.filter = filter;
	this.reader = reader;
    }

    /**
     * @see Scorer#explain
     */
    public Explanation explain(int doc) throws IOException {
	return delegatee.explain(doc);

    }

    /**
     * Returns a {@link FilteredHitCollector} which filters
     * the collected hits with a {@link org.apache.jackrabbit.core.search.lucene.PackageFilter}.
     * @param hc the <code>HitCollector</code> from the underlying
     *   lucene query.
     * @param maxDoc collect hits until <code>maxDoc</code> has reached.
     * @throws IOException if an error occurs while collecting hits.
     *   e.g. while reading from the search index.
     */
    public void score(HitCollector hc, int maxDoc) throws IOException {
	if (collector == null) {
	    collector = new FilteredHitCollector();
	}
	collector.setDelegatee(hc);
	delegatee.score(collector, maxDoc);
    }

    //---------------------< inner class >---------------------------

    private class FilteredHitCollector extends HitCollector {

	/** The unfiltered <code>HitCollector</code>. */
	private HitCollector delegateeCollector;

	/** The filter */
	private BitSet filterBitSet;

	/**
	 * Creates a new <code>FilteredHitCollector</code>.
	 * @throws IOException if an error occurs while
	 *   calculating the filter.
	 */
	FilteredHitCollector() throws IOException {
	    filterBitSet = filter.bits(reader);
	}

	/**
	 * Sets the currently used unfiltered <code>HitCollector</code>.
	 * @param hc the new <code>HitCollector</code>.
	 */
	private void setDelegatee(HitCollector hc) {
	    delegateeCollector = hc;
	}

	/**
	 * Collects the document with number <code>doc</code>.
	 * This hit collector first applies the filter on the
	 * document and only passes the collect call to the
	 * underlying <code>HitCollector</code> if the filter
	 * allows the document.
	 * @param doc the document in question for filtering.
	 * @param score the score for this document.
	 */
	public void collect(int doc, float score) {
	    // only forward the collect call if the filter
	    // allows the document.
	    if (filterBitSet.get(doc)) {
		delegateeCollector.collect(doc, score);
	    }
	}
    }
}
