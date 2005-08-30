package org.apache.jackrabbit.core.query.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortComparator;
import org.apache.lucene.search.SortField;

import java.io.IOException;

/**
 * Implements a <code>SortComparator</code> which knows how to sort on a lucene
 * field that contains values for multiple properties.
 * <p/>
 * <b>Important:</b> The ScoreDocComparator returned by {@link #newComparator}
 * does not implement the contract for {@link ScoreDocComparator#sortValue(ScoreDoc)}
 * properly. The method will always return an empty String to save memory consumption
 * on large property ranges. Those values are only of relevance when queries
 * are executed with a <code>MultiSearcher</code>, which is currently not the
 * case in Jackrabbit.
 */
class SharedFieldSortComparator extends SortComparator {

    /**
     * A <code>SharedFieldSortComparator</code> that is based on
     * {@link FieldNames#PROPERTIES}.
     */
    static final SortComparator PROPERTIES = new SharedFieldSortComparator(FieldNames.PROPERTIES);

    /**
     * The name of the shared field in the lucene index.
     */
    private final String field;

    /**
     * Creates a new <code>SharedFieldSortComparator</code> for a given shared
     * field.
     * @param fieldname the shared field.
     */
    public SharedFieldSortComparator(String fieldname) {
        this.field = fieldname;
    }

    /**
     * Creates a new <code>ScoreDocComparator</code> for an embedded
     * <code>propertyName</code> and a <code>reader</code>.
     * @param reader the index reader.
     * @param propertyName the name of the property to sort.
     * @return a <code>ScoreDocComparator</code> for the
     * @throws IOException
     */
    public ScoreDocComparator newComparator(final IndexReader reader, String propertyName)
            throws IOException {
        // get the StringIndex for propertyName
        final FieldCache.StringIndex index
                = SharedFieldCache.INSTANCE.getStringIndex(reader, field,
                        propertyName, SharedFieldSortComparator.this);

        return new ScoreDocComparator() {
            public final int compare(final ScoreDoc i, final ScoreDoc j) {
                final int fi = index.order[i.doc];
                final int fj = index.order[j.doc];
                if (fi < fj) {
                    return -1;
                } else if (fi > fj) {
                    return 1;
                } else {
                    return 0;
                }
            }

            /**
             * Always returns an empty String.
             * @param i the score doc.
             * @return an empty String.
             */
            public Comparable sortValue(final ScoreDoc i) {
                // return dummy value
                return "";
            }

            public int sortType() {
                return SortField.CUSTOM;
            }
        };
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    protected Comparable getComparable(String termtext) {
        throw new UnsupportedOperationException();
    }
}
