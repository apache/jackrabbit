/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.ocm.exception;

/**
 * The <code>InvalidQuerySyntaxException</code> is an
 * <code>ObjectContentManagerException</code> thrown if the query of the
 * {@link org.apache.jackrabbit.ocm.manager.ObjectContentManager#getObjectIterator(String, String)}
 * is invalid. This exception actually wraps a standard JCR
 * <code>javax.jcr.InvalidQueryException</code> to make it an unchecked
 * exception. The cause of this exception will always be the original
 * <code>InvalidQueryException</code> and the message will always be the
 * original exception's message.
 */
public class InvalidQueryException extends ObjectContentManagerException {

    /**
     * Create an exception wrapping the given checked JCR exception
     * 
     * @param cause The wrapped JCR <code>InvalidQueryException</code>
     */
    public InvalidQueryException(javax.jcr.query.InvalidQueryException cause) {
        super(cause.getMessage(), cause);
    }
}
