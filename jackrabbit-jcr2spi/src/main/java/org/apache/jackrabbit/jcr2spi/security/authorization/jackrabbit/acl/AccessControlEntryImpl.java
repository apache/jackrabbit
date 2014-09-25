package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeBits;
import org.apache.jackrabbit.spi.commons.value.QValueValue;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.value.ValueHelper;

/**
 * Default implementation of the JackrabbitAccessControlEntry interface.
 */
abstract class AccessControlEntryImpl implements JackrabbitAccessControlEntry {

    /*
     * The principal this entry has been created for.
     */
    private final Principal principal;
    
    /*
     * The privileges in this entry.
     */
    private final Privilege[] privileges;
        
    /*
     * Whether this entry is allowed/denied
     */
    private final boolean isAllow;
    
    /*
     * Restrictions that may apply with this entry.
     */
    private final Map<Name, QValue> restrictions;

    /**
     * 
     * @param principal
     * @param privileges
     * @param isAllow
     * @param restrictions
     * @throws RepositoryException 
     */
    AccessControlEntryImpl(Principal principal, Privilege[] privileges, boolean isAllow, Map<String, QValue> restrictions) 
                                    throws RepositoryException {
        if (principal == null || (privileges != null && privileges.length == 0)) {
            throw new AccessControlException("An Entry must not have a NULL principal or empty privileges");
        }
        checkAbstract(privileges);
        
        this.principal = principal;
        this.privileges = privileges;
        this.isAllow = isAllow;
        
        if (restrictions == null || (restrictions.size() == 0)) {
            this.restrictions = Collections.<Name, QValue>emptyMap();
        } else {
            this.restrictions = new HashMap<Name, QValue>(restrictions.size());
            for (String restName : restrictions.keySet()) {
                this.restrictions.put(getNamePathResolver().getQName(restName), restrictions.get(restName));
            }
        }
    }

    /**
     * 
     * @param base
     * @param privileges
     * @param isAllow
     * @throws RepositoryException 
     */
    AccessControlEntryImpl(AccessControlEntryImpl base, Privilege[] newPrivileges, boolean isAllow) throws RepositoryException {
        this(base.principal, newPrivileges, isAllow, (base.restrictions == null) ? null : Collections.<String, QValue>emptyMap());

        // store the base restrictions
        for (Name restName : base.restrictions.keySet()) {
            this.restrictions.put(restName, base.restrictions.get(restName));
        }
    }
    
    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Privilege[] getPrivileges() {
        return privileges;
    }
    
    @Override
    public boolean isAllow() {
        return isAllow;
    }

    /**
     * Returns the restrictions stored in this entry.
     * @return     the restrictions of this entry.
     */
    public Map<Name, QValue> getRestrictions() {
        return Collections.unmodifiableMap(restrictions);
    }
    
    @Override
    public String[] getRestrictionNames() throws RepositoryException {
        List<String> restNames = new ArrayList<String>(restrictions.size());
        for (Name restName : restrictions.keySet()) {
            restNames.add(getNamePathResolver().getJCRName(restName));
        }
        return restNames.toArray(new String[restNames.size()]);
    }

    @Override
    public Value getRestriction(String restrictionName)
            throws ValueFormatException, RepositoryException {
       return getRestriction(getNamePathResolver().getQName(restrictionName));
    }

    private Value getRestriction(Name restName) throws RepositoryException {
        try {
            if (!restrictions.containsKey(restName)) {
                return null;
            }
            return createJcrValue(restrictions.get(restName), getQValueFactory());
        } catch (IllegalStateException e) {
            throw new RepositoryException(e.getMessage());
        }
    }
    
    /*
     * As of Jackrabbit 2.8, this extention has been added to the Jackrabbit API.
     * However, Jackrabbit (before) OAK doesn't support mv. restrictions. Thus simply
     * return an array containing the single restriction value.
     */
    @Override
    public Value[] getRestrictions(String restrictionName)
            throws RepositoryException {
        return new Value[] {getRestriction(restrictionName)};
    }
    
    //-------------------------------------------------------------< private >---
    private void checkAbstract(Privilege[] privileges) throws AccessControlException {
        for (Privilege privilege : privileges) {
            if (privilege.isAbstract()) {
                throw new AccessControlException("An Entry cannot contain abstract privileges.");
            }
        }
    }

    /**
     * Creates a jcr Value from the given qvalue using the specified
     * factory.
     * @return         the jcr value representing the qvalue.
     */
    private Value createJcrValue(QValue qValue, QValueFactory factory) throws RepositoryException {
        
        // build ValueFactory
        ValueFactoryQImpl valueFactory = new ValueFactoryQImpl(getQValueFactory(), getNamePathResolver());

        // build jcr value
        QValueValue jcrValue = new QValueValue(qValue, getNamePathResolver());
        
        return ValueHelper.copy(jcrValue, valueFactory);
    }

    //-------------------------------------------------------------< Object >---

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AccessControlEntryImpl) {
            AccessControlEntryImpl other = (AccessControlEntryImpl) obj;
            List<Privilege> thisPrivileges = Arrays.asList(this.getPrivileges());
            List<Privilege> otherPrivileges = Arrays.asList(other.getPrivileges());
            
            boolean equalName = principal.getName().equals(other.principal.getName());
            boolean equalPrivilege = otherPrivileges.containsAll(thisPrivileges);
            boolean equalRestriction = restrictions.equals(other.restrictions);
            boolean bothAllow = isAllow == other.isAllow;
            
            return  equalName && equalPrivilege && bothAllow && equalRestriction;
        }
        return false;
    }
    
    //----------------------------------< to be implemented by subclasses >---
    protected abstract NamePathResolver getNamePathResolver();
    protected abstract QValueFactory getQValueFactory();
}
