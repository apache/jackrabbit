package org.apache.jackrabbit.core.query.lucene;

import org.apache.lucene.search.SortComparator;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;

/**
 * Implements a <code>SortComparator</code> which knows how to sort on a lucene
 * field that contains values for multiple properties.
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
    public ScoreDocComparator newComparator(IndexReader reader, String propertyName)
            throws IOException {
        // get the StringIndex for propertyName
        final FieldCache.StringIndex index
                = SharedFieldCache.INSTANCE.getStringIndex(reader, field,
                        propertyName, SharedFieldSortComparator.this);

        return new ScoreDocComparator() {
            public final int compare (final ScoreDoc i, final ScoreDoc j) {
              final int fi = index.order[i.doc];
              final int fj = index.order[j.doc];
              if (fi < fj) {
                  return -1;
              } else if  (fi > fj) {
                  return 1;
              } else {
                  return 0;
              }
            }

            public Comparable sortValue (final ScoreDoc i) {
              return index.lookup[index.order[i.doc]];
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
