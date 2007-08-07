package org.apache.jackrabbit.api;

import javax.jcr.query.Query;

/**
 * This interface adds some methods that are to be expected in JCR 2.0 aka
 * JSR-283 (https://jsr-283.dev.java.net/issues/show_bug.cgi?id=214). It will
 * be obsolete when Jackrabbit implements JCR 2.0.
 */
public interface JackrabbitQuery extends Query {

    /**
     * Sets the maximum size of the result set.
     * 
     * @param limit new maximum size of the result set
     */
    void setLimit(long limit);

    /**
     * Sets the start offset of the result set.
     * 
     * @param offset new start offset of the result set
     */
    void setOffset(long offset);

}
