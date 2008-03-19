package org.apache.jackrabbit.core.security.jsr283.security;

import javax.jcr.RepositoryException;

/**
 * Exception thrown by access control related methods of
 * <code>AccessControlManager</code>.
 *
 * @since JCR 2.0
 */
public class AccessControlException extends RepositoryException {
    
    /**
     * Constructs a new instance of this class with <code>null</code> as its
     * detail message.
     */
    public AccessControlException() {
        super();
    }

    /**
     * Constructs a new instance of this class with the specified detail
     * message.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public AccessControlException(String message) {
        super(message);
    }

    /**
     * Constructs a new instance of this class with the specified detail
     * message and root cause.
     *
     * @param message   the detail message. The detail message is saved for
     *                  later retrieval by the {@link #getMessage()} method.
     * @param rootCause root failure cause
     */
    public AccessControlException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    /**
     * Constructs a new instance of this class with the specified root cause.
     *
     * @param rootCause root failure cause
     */
    public AccessControlException(Throwable rootCause) {
        super(rootCause);
    }
}
