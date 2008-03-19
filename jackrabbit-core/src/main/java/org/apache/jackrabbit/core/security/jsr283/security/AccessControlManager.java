package org.apache.jackrabbit.core.security.jsr283.security;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.security.Principal;

/**
 * The <code>AccessControlManager</code> object is accessed via
 * {@link javax.jcr.Session#getAccessControlManager()}. It provides methods for:
 * <ul>
 * <li>Access control discovery</li>
 * <li>Assigning access control policies</li>
 * <li>Assigning access control entries</li>
 * </ul>
 *
 * @since JCR 2.0
 */
public interface AccessControlManager {

    /**
     * Returns the privileges supported for absolute path <code>absPath</code>,
     * which must be an existing node.
     * <p/>
     * This method does not return the privileges held by the session. Instead,
     * it returns the privileges that the repository supports.
     *
     * @param absPath an absolute path.
     * @return an array of <code>Privilege</code>s.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to 
     *                               retrieve the node.
     * @throws RepositoryException   if another error occurs.
     */
    public Privilege[] getSupportedPrivileges(String absPath)
            throws PathNotFoundException, RepositoryException;

    /**
     * Returns whether the session has the specified privileges for absolute
     * path <code>absPath</code>, which must be an existing node.
     * <p/>
     * Testing an aggregate privilege is equivalent to testing each nonaggregate
     * privilege among the set returned by calling
     * <code>Privilege.getAggregatePrivileges()</code> for that privilege.
     *
     * @param absPath    an absolute path.
     * @param privileges an array of <code>Privilege</code>s.
     * @return <code>true</code> if the session has the specified privileges;
     *         <code>false</code> otherwise.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to 
     *                               retrieve the node.
     * @throws RepositoryException   if another error occurs.
     */
    public boolean hasPrivileges(String absPath, Privilege[] privileges)
            throws PathNotFoundException, RepositoryException;

    /**
     * Returns the privileges the session has for absolute path absPath, which
     * must be an existing node.
     * <p/>
     * The returned privileges are those for which {@link #hasPrivileges} would
     * return <code>true</code>.
     * 
     * @param absPath an absolute path.
     * @return an array of <code>Privilege</code>s.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to 
     *                               retrieve the node.
     * @throws RepositoryException   if another error occurs.
     */
    public Privilege[] getPrivileges(String absPath)
            throws PathNotFoundException, RepositoryException;

    /**
     * Returns the <code>AccessControlPolicy</code> that has been set to
     * the node at <code>absPath</code> or <code>null</code> if no
     * policy has been set. This method reflects the binding state including
     * transient policy modifications.
     * <p/>
     * Use {@link #getEffectivePolicy(String)} in order to determine the
     * policy that effectively applies at <code>absPath</code>.
     *
     * @param absPath an absolute path.
     * @return an <code>AccessControlPolicy</code> object or <code>null</code>.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to 
     *                               retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>GET_ACCESS_CONTROL</code> privilege 
     *                               for the <code>absPath</code> node.
     * @throws RepositoryException   if another error occurs.
     */
    public AccessControlPolicy getPolicy(String absPath)
            throws PathNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * Returns the <code>AccessControlPolicy</code> that currently is in effect 
     * at the node at <code>absPath</code>. This may be an 
     * <code>AccessControlPolicy</code> set through this API or some 
     * implementation specific (default) policy.
     * </p>
     *
     * @param absPath an absolute path.
     * @return an <code>AccessControlPolicy</code> object.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to 
     *                               retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>READ_ACCESS_CONTROL</code> privilege 
     *                               for the <code>absPath</code> node.
     * @throws RepositoryException   if another error occurs.
     */
    public AccessControlPolicy getEffectivePolicy(String absPath)
            throws PathNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * Returns the access control policies that are capable of being applied to
     * the node at <code>absPath</code>.
     *
     * @param absPath an absolute path.
     * @return an <code>AccessControlPolicyIterator</code> over the applicable
     *         access control policies or an empty iterator if no policies are
     *         applicable.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to 
     *                               retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>READ_ACCESS_CONTROL</code> privilege 
     *                               for the <code>absPath</code> node.
     * @throws RepositoryException   if another error occurs.
     */
    public AccessControlPolicyIterator getApplicablePolicies(String absPath)
            throws PathNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * Binds the <code>policy</code> to the node at <code>absPath</code>.
     * <p/>
     * Only one policy may be bound at a time. If more than one policy per node 
     * is required, the implementation should provide an appropriate aggregate 
     * policy among those returned by <code>getApplicablePolicies(absPath)</code>.
     * The access control policy does not take effect until a <code>save</code> 
     * is performed.
     * <p/>
     * If the node has access control entries that were bound to it through the 
     * JCR API prior to the <code>setPolicy</code> call, then these entries may 
     * be deleted. Any implementation-specific (non-JCR) access control 
     * settings may be changed in response to a successful call to 
     * <code>setPolicy</code>.
     *
     * @param absPath an absolute path.
     * @param policy  the <code>AccessControlPolicy</code> to be applied.
     * @throws PathNotFoundException   if no node at <code>absPath</code> exists
     *                                 or the session does not have privilege to 
     *                                 retrieve the node.
     * @throws AccessControlException  if the policy is not applicable.
     * @throws AccessDeniedException   if the session lacks
     *                                 <code>MODIFY_ACCESS_CONTROL</code> 
     *                                 privilege for the <code>absPath</code> node.
     * @throws RepositoryException     if another error occurs.
     */
    public void setPolicy(String absPath, AccessControlPolicy policy)
            throws PathNotFoundException, AccessControlException,
            AccessDeniedException, RepositoryException;

    /**
     * Removes the <code>AccessControlPolicy</code> from the node at absPath and
     * returns it.
     * <p/>
     * 
     * An <code>AccessControlPolicy</code> can only be removed if it was
     * bound to the specified node through this API before. The effect of the 
     * removal only takes place upon <code>Session.save()</code>. Whichever 
     * defaults the implementation applies now take effect.
     * Note, that an implementation default or any other effective 
     * <code>AccessControlPolicy</code> that has not been applied to the node
     * before may never be removed using this method.
     *
     * @param absPath an absolute path.
     * @return the removed <code>AccessControlPolicy</code>.
     * @throws PathNotFoundException   if no node at <code>absPath</code> exists
     *                                 or the session does not have privilege to 
     *                                 retrieve the node.
     * @throws AccessControlException  if no policy exists.
     * @throws AccessDeniedException   if the session lacks
     *                                 <code>MODIFY_ACCESS_CONTROL</code> 
     *                                 privilege for the <code>absPath</code> node.
     * @throws RepositoryException     if another error occurs.
     */
    public AccessControlPolicy removePolicy(String absPath)
            throws PathNotFoundException, AccessControlException,
            AccessDeniedException, RepositoryException;

    /**
     * Returns all access control entries assigned to the node at <code>absPath</code>
     * including transient modifications made to the entries at <code>absPath</code>.
     * <p/>
     * This method is only guaranteed to return an <code>AccessControlEntry</code>
     * if that <code>AccessControlEntry</code> has been assigned <i>through this API</i>.
     *
     * @param absPath an absolute path
     * @return all access control entries assigned at to specified node.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to 
     *                               retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>READ_ACCESS_CONTROL</code> privilege 
     *                               for the <code>absPath</code> node.
     * @throws UnsupportedRepositoryOperationException if access control
     *         is not supported.
     * @throws RepositoryException   if another error occurs.
     */
    public AccessControlEntry[] getAccessControlEntries(String absPath)
            throws PathNotFoundException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns the access control entries that are effective at the node at
     * <code>absPath</code>.
     * <p/>
     * This method is intended for information purpose only and should allow
     * the user to determine which entries are currently used for access control
     * evaluation.
     * </p>
     * If an implementation is not able to determine the effective entries
     * present at the given node it returns <code>null</code> in order to indicate
     * that entries exists but the implementation cannot find them. If there
     * are no entries present at the given node an empty array will be returned.
     *
     * @param absPath an absolute path
     * @return the access control entries that are currently effective at the
     *         node at <code>absPath</code> or <code>null</code> if the
     *         implementation is not able to determine the effective entries.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *         or the session does not have privilege to retrieve the node.
     * @throws AccessDeniedException if the session lacks
     *         <code>READ_ACCESS_CONTROL</code> privilege for the
     *         <code>absPath</code> node.
     * @throws UnsupportedRepositoryOperationException if access control
     *         is not supported.
     * @throws RepositoryException if another error occurs.
     */
    public AccessControlEntry[] getEffectiveAccessControlEntries(String absPath)
            throws PathNotFoundException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;


    /**
     * Adds the access control entry consisting of the specified
     * <code>principal</code> and the specified <code>privileges</code> to the
     * node at <code>absPath</code>.
     * <p/>
     * This method returns the <code>AccessControlEntry</code> object constructed from the
     * specified <code>principal</code> and contains at least the given <code>privileges</code>.
     * An implementation may return a resulting ACE that combines the given <code>privileges</code>
     * with those added by a previous call to <code>addAccessControlEntry</code> for the same
     * <code>Principal</code>. However, a call to <code>addAccessControlEntry</code> for a given
     * <code>Principal</code> can never remove a <code>Privilege</code> added by a previous call
     * to <code>addAccessControlEntry</code>.
     * <p/>
     * The access control entry does not take effect until a <code>save</code>
     * is performed.
     * <p/>
     * This method is guaranteed to affect only the privileges of the specified
     * <code>principal</code>.
     * <p/>
     * This method <i>may</i> affect the privileges granted to that principal with
     * respect to nodes other than that specified. However, if it does, it is
     * guaranteed to only affect the privileges of those other nodes in the
     * same way as it affects the privileges of the specified node.
     *
     * @param absPath    an absolute path.
     * @param principal  a <code>Principal</code>.
     * @param privileges an array of <code>Privilege</code>s.
     * @return the <code>AccessControlEntry</code> object constructed from the
     *         specified <code>principal</code> and <code>privileges</code>.
     * @throws PathNotFoundException      if no node at <code>absPath</code> exists
     *                                    or the session does not have privilege to retrieve the node.
     * @throws AccessControlException     if the specified principal does not exist,
     *                                    if any of the specified privileges is not supported at
     *                                    <code>absPath</code> or if some other access control related
     *                                    exception occurs.
     * @throws AccessDeniedException      if the session lacks
     *                                    <code>MODIFY_ACCESS_CONTROL</code> privilege for the
     *                                    <code>absPath</code> node.
     * @throws UnsupportedRepositoryOperationException if access control
     *         is not supported.
     * @throws RepositoryException        if another error occurs.
     */
    public AccessControlEntry addAccessControlEntry(String absPath,
                                                    Principal principal,
                                                    Privilege[] privileges)
            throws PathNotFoundException, AccessControlException,
            AccessDeniedException, UnsupportedRepositoryOperationException,
            RepositoryException;

    /**
     * Removes the specified <code>AccessControlEntry</code> from the node at
     * <code>absPath</code>.
     * <p/>
     * This method is guaranteed to affect only the privileges of the principal
     * defined within the passed <code>AccessControlEntry</code>.
     * <p/>
     * This method <i>may</i> affect the privileges granted to that principal
     * with respect to nodes other than that specified. However, if it does,
     * it is guaranteed to only affect the privileges of those other nodes in
     * the same way as it affects the privileges of the specified node.
     * <p/>
     * Only exactly those entries obtained through 
     * <code>getAccessControlEntries<code> can be removed. The effect of the 
     * removal only takes effect upon <code>Session.save()</code>.
     *
     * @param absPath an absolute path.
     * @param ace     the access control entry to be removed.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     *                               or the session does not have privilege to retrieve the node.
     * @throws AccessControlException
     *                               if the specified entry is not
     *                               present on the specified node.
     * @throws AccessDeniedException if the session lacks
     *                               <code>MODIFY_ACCESS_CONTROL</code> privilege for the
     *                               <code>absPath</code> node.
     * @throws UnsupportedRepositoryOperationException if access control
     *         is not supported.
     * @throws RepositoryException   if another error occurs.
     */
    public void removeAccessControlEntry(String absPath, AccessControlEntry ace)
            throws PathNotFoundException, AccessControlException,
            AccessDeniedException, UnsupportedRepositoryOperationException,
            RepositoryException;
}
