package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.jcr2spi.security.AccessControlConstants;

abstract class AbstractAccessControlList implements JackrabbitAccessControlList, AccessControlConstants {

    private final String jcrPath;
    
    AbstractAccessControlList(String jcrPath) {
        this.jcrPath = jcrPath;
    }

    @Override
    public String getPath() {
        return jcrPath;
    }
    
    // ------------------------------------------------< AccessControlList >---
    @Override
    public boolean addAccessControlEntry(Principal principal, Privilege[] privileges) throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, true, Collections.<String, Value>emptyMap());
    }
    
    @Override
    public AccessControlEntry[] getAccessControlEntries()
            throws RepositoryException {
        return getEntries().toArray(new AccessControlEntry[getEntries().size()]);
    }

    //----------------------------------------------< JackrabbitAccessControlList >---
    @Override
    public boolean isEmpty() {
        return getEntries().isEmpty();
    }

    @Override
    public int size() {
        return getEntries().size();
    }

    @Override
    public boolean addEntry(Principal principal, Privilege[] privileges,
            boolean isAllow) throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, isAllow, Collections.<String, Value>emptyMap());
    }

    @Override
    public boolean addEntry(Principal principal, Privilege[] privileges,
            boolean isAllow, Map<String, Value> restrictions,
            Map<String, Value[]> mvRestrictions) throws AccessControlException,
            RepositoryException {
        if (mvRestrictions != null && !mvRestrictions.isEmpty()) {
            throw new UnsupportedRepositoryOperationException("Jackrabbit 2.x does not support multi-valued restrictions");
        }
        return addEntry(principal, privileges, isAllow, restrictions);
    }

    @Override
    public void orderBefore(AccessControlEntry srcEntry,
            AccessControlEntry destEntry) throws AccessControlException,
            UnsupportedRepositoryOperationException, RepositoryException {
        // TODO access to the list object is needed to perform this operation.
        // make it abstract. or with getEntries()
    }

    
    //-------------------------------------------------< abstract >---
    
    public abstract List<AccessControlEntry> getEntries();

}
