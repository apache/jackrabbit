package org.apache.jackrabbit.core.security.jsr283.security;

import javax.jcr.RepositoryException;

/**
 * An <code>AccessControlPolicy</code> is an object with a name and an optional
 * description. Examples of possible <code>AccessControlPolicy</code> 
 * implementations include access control lists or role-responsibility 
 * assignments.
 *
 * @since JCR 2.0
 */
public interface AccessControlPolicy {
    /**
     * Returns the name of the access control policy, which should be unique
     * among the choices applicable to any particular node.
     * It is presented to provide an easily identifiable choice for
     * users choosing a policy to assign to a node.
     *
     * @return the name of the access control policy.
     * @throws RepositoryException if an error occurs.
     */
    public String getName() throws RepositoryException;

    /**
     * Returns a human readable description of the access control policy which
     * should be sufficient for allowing end users to chose between different
     * policies to apply to a node.
     *
     * @return a human readable description of the access control policy.
     * @throws RepositoryException if an error occurs.
     */
    public String getDescription() throws RepositoryException;
}
