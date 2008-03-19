package org.apache.jackrabbit.core.security.jsr283.security;

import java.security.Principal;

/**
 * An <code>AccessControlEntry</code> represents the association of one or more
 * <code>Privilege</code> objects with a specific <code>Principal</code>.
 *
 * @since JCR 2.0
 */
public interface AccessControlEntry {
    /**
     * Returns the principal associated with this access control entry.
     * @return a <code>Principal</code>.
     */
    public Principal getPrincipal();

    /**
     * Returns the privileges associated with this access control entry.
     * @return an array of <code>Privilege</code>s.
     */
    public Privilege[] getPrivileges();
}
