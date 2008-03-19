package org.apache.jackrabbit.core.security.jsr283.security;

import javax.jcr.RangeIterator;

/**
 * Allows easy iteration through a list of <code>AccessControlPolicy</code>s
 * with <code>nextAccessControlPolicy</code> as well as a <code>skip</code>
 * method inherited from <code>RangeIterator</code>.
 *
 * @since JCR 2.0
 */
public interface AccessControlPolicyIterator extends RangeIterator {
    
    /**
     * Returns the next <code>AccessControlPolicy</code> in the iteration.
     *
     * @return the next <code>AccessControlPolicy</code> in the iteration.
     * @throws java.util.NoSuchElementException if iteration has no more
     *         <code>AccessControlPolicy</code>s.
    */
   public AccessControlPolicy nextAccessControlPolicy();

}
