package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.principal.JackrabbitPrincipal;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessControlListImpl extends AbstractAccessControlList {
    
    private static final Logger log = LoggerFactory.getLogger(AccessControlListImpl.class);
    
    private final List<AccessControlEntry> entries;
    private final QValueFactory qValueFactory;
    private final NamePathResolver resolver;
    
    /**
     * Namespace sensitive name of the REP_GLOB property in standard JCR form.
     */
    private final String jcrRepGlob;
    
    public AccessControlListImpl(String jcrPath, NamePathResolver resolver, QValueFactory qValueFactory) throws RepositoryException {
        super(jcrPath);
        this.entries = new ArrayList<AccessControlEntry>();
        this.qValueFactory = qValueFactory;
        this.resolver = resolver;
        
        try {
            jcrRepGlob = resolver.getJCRName(P_GLOB);
        } catch (NamespaceException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    AccessControlListImpl(NodeState aclNode, String aclPath, NamePathResolver resolver, QValueFactory factory, AccessControlManager acm) throws RepositoryException {
        this(aclPath, resolver, factory);

        NodeEntry entry = (NodeEntry) aclNode.getHierarchyEntry();
        Iterator<NodeEntry> it = entry.getNodeEntries();
        
        while(it.hasNext()) {
            NodeState aceNode = it.next().getNodeState();
            Principal principal = null;
            Privilege[] privileges = null;
            try {

                PropertyState ps = aceNode.getPropertyState(N_REP_PRINCIPAL_NAME);
                
                // rep:principal property
                String principalName = ps.getValue().getString();
                principal = new PrincipalImpl(principalName);
                
                // rep:privileges property
                ps = aceNode.getPropertyState(N_REP_PRIVILEGES);
                
                QValue[] values = ps.getValues();
                privileges = new Privilege[values.length];
                for (int i = 0; i < values.length; i++) {
                    privileges[i] = acm.privilegeFromName(values[i].getString());
                }                
               
                // rep:glob property -> restrictions
                Map<String, QValue> restrictions = null;
                if (aceNode.hasPropertyName(P_GLOB)) {
                    ps = aceNode.getPropertyState(P_GLOB);
                    restrictions = Collections.singletonMap(jcrRepGlob, ps.getValue());
                }
                
                // the isAllow flag
                boolean isAllow = NT_REP_GRANT_ACE.equals(aceNode.getNodeTypeName());
                // build the entry
                AccessControlEntry ace = new JackrabbitEntry(principal, privileges, isAllow, restrictions);
                entries.add(ace);
            } catch (RepositoryException e) {
                log.debug("Fail to create Entry for "+ aceNode.getName().toString());
            }
        }
    }
    @Override
    public List<AccessControlEntry> getEntries() {
        return entries;
    }
    
    //-------------------------------------------------< AccessControlList >---
    @Override
    public synchronized void removeAccessControlEntry(AccessControlEntry ace)
            throws AccessControlException, RepositoryException {
        if (entries.contains(ace)) {
            entries.remove(ace);
        } else {
            throw new AccessControlException("Entry not present in this list");
        }
    }

    //----------------------------------------------------< JackrabbitAccessControlList >---
    @Override
    public boolean addEntry(Principal principal, Privilege[] privileges,
            boolean isAllow, Map<String, Value> restrictions)
            throws AccessControlException, RepositoryException {
        
        // create entry to be added
        Map<String, QValue> rs = createRestrictions(restrictions);
        JackrabbitEntry entry = createEntry(principal, privileges, isAllow, rs);

        return internalAddEntry(entry);
    }
    
    @Override
    public String[] getRestrictionNames() throws RepositoryException {
        // TODO
        return new String[] {jcrRepGlob};
    }

    @Override
    public int getRestrictionType(String restrictionName) throws RepositoryException {
        if (!jcrRepGlob.equals(restrictionName)) {
            // JR2 feature
            return PropertyType.UNDEFINED;
        }
        return PropertyType.STRING;
    }

    //------------------------------------------------------------< private >---
    private JackrabbitEntry createEntry(Principal principal, Privilege[] privileges, boolean isAllow, Map<String, QValue> restrictions) throws RepositoryException {
        return new JackrabbitEntry(principal, privileges, isAllow, restrictions);
    }

    private boolean equalRestriction(JackrabbitEntry e1, JackrabbitEntry e2) throws RepositoryException {
        Value v1 = e1.getRestriction(jcrRepGlob);
        Value v2 = e2.getRestriction(jcrRepGlob);

        return (v1 == null) ? v2 == null : v1.equals(v2);
    }
    
    private Map<String, QValue> createRestrictions(Map<String, Value> restrictions) throws RepositoryException {
        Map<String, QValue> rs = new HashMap<String, QValue>(restrictions.size());
        for (String restName : restrictions.keySet()) {
            QValue restValue = qValueFactory.create(restrictions.get(restName).getString(), PropertyType.STRING);
            rs.put(restName, restValue);
        }
        return rs;
    }
    /**
     * Returns a list of entries that applies to the principal.
     * @param principal
     * @return
     */
    private List<JackrabbitEntry> internalGetEntries(Principal principal) {
        String principalName = principal.getName();
        List<JackrabbitEntry> entriesPerPrincipal = new ArrayList<JackrabbitEntry>(2);
        for (AccessControlEntry entry : entries) {
            if (principalName.equals(entry.getPrincipal().getName())) {
                entriesPerPrincipal.add((JackrabbitEntry) entry);
            }
        }
        return entriesPerPrincipal;
    }
    
    private synchronized boolean internalAddEntry(JackrabbitEntry entry) throws RepositoryException {
        Principal principal = entry.getPrincipal();
        List<JackrabbitEntry> entriesPerPrincipal = internalGetEntries(principal);
        if (entriesPerPrincipal.isEmpty()) {
            // simple case: NEW entry for the principal.
            entries.add(entry);
            return true;
        } else { 
            if (entriesPerPrincipal.contains(entry)) {
                // the same entry is already contained -> no modification
                return false;
            }

            // check if need to adjust existing entries
            int updateIndex = -1;
            JackrabbitEntry complementEntry = null;

            for (JackrabbitEntry e : entriesPerPrincipal) {
                if (equalRestriction(entry, e)) {
                    if (entry.isAllow() == e.isAllow()) {
                        // Check privilege inclusion is deferred to cr implementation.
                        // need to update an existing entry
                        List<Privilege> existingPrivileges = new LinkedList<Privilege>(Arrays.asList(e.getPrivileges()));
                        List<Privilege> diff = new LinkedList<Privilege>();
                        List<Privilege> newPrivileges = new LinkedList<Privilege>(Arrays.asList(entry.getPrivileges()));
                        
                        if (existingPrivileges.containsAll(newPrivileges)) {
                            // this part will be taken care in the equals() method
                            return false;
                        }

                        // TODO: rework logic
                        newPrivileges.removeAll(existingPrivileges);
                        diff.addAll(newPrivileges);
                        existingPrivileges.addAll(diff);

                        // remember the index of the existing entry to be updated later on.
                        updateIndex = entries.indexOf(e);

                        // remove the existing entry and create a new one that
                        // includes the merge privileges.
                        entries.remove(e);
                        entry = new JackrabbitEntry(entry, existingPrivileges.toArray(new Privilege[existingPrivileges.size()]), entry.isAllow());
                    } else {
                        complementEntry = e;
                    }
                }
            }

            // make sure, that the complement entry (if existing) does not
            // grant/deny the same privileges -> remove privileges that are now
            // denied/granted.
            if (complementEntry != null) {

                List<Privilege> complPrivs = new LinkedList<Privilege>(Arrays.asList(complementEntry.getPrivileges()));
                List<Privilege> otherPrivs = new LinkedList<Privilege>(Arrays.asList(entry.getPrivileges()));
                complPrivs.removeAll(otherPrivs);
                List<Privilege> diff = new LinkedList<Privilege>();
                diff.addAll(complPrivs);
                
                if (diff.isEmpty()) {
                    // remove the complement entry as the new entry covers
                    // all privileges.
                    entries.remove(complementEntry);
                    updateIndex--;

                } else {
                    // replace the existing entry having the privileges adjusted
                    int index = entries.indexOf(complementEntry);
                    entries.remove(complementEntry);

                    JackrabbitEntry tmpl = new JackrabbitEntry(entry, diff.toArray(new Privilege[diff.size()]), !entry.isAllow());
                    entries.add(index, tmpl);
                } /* else: does not need to be modified.*/            
            }
            // finally update the existing entry or add the new entry passed
            // to this method at the end.
            if (updateIndex < 0) {
                entries.add(entry);
            } else {
                entries.add(updateIndex, entry);
            }
            return true;
        }
    }

    /**
     *
     */
    private final class JackrabbitEntry extends AccessControlEntryImpl {

        protected JackrabbitEntry(Principal principal, Privilege[] privileges,
                boolean isAllow, Map<String, QValue> restrictions)
                throws RepositoryException {
            super(principal, privileges, isAllow, restrictions);
        }

        private JackrabbitEntry(JackrabbitEntry baseEntry, Privilege[] newPrivileges, boolean isAllow) throws RepositoryException {
            super(baseEntry, newPrivileges, isAllow);
        }
        
        @Override
        protected NamePathResolver getNamePathResolver() {
            return resolver;
        }

        @Override
        protected QValueFactory getQValueFactory() {
            return qValueFactory;
        }
        
    }

    // Copied from Jackrabbit-core -> move to spi?
    /**
     * Base class for implementations of <code>JackrabbitPrincipal</code>.
     */
    public class PrincipalImpl implements JackrabbitPrincipal, Serializable {

        /** the serial number */
        private static final long serialVersionUID = 384040549033267804L;

        /**
         * the name of this principal
         */
        private final String name;


        /**
         * Creates a new principal with the given name.
         *
         * @param name the name of this principal
         */
        public PrincipalImpl(String name) {
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("Principal name can neither be null nor empty String.");
            }
            this.name = name;
        }

        //----------------------------------------------------------< Principal >---
        /**
         * {@inheritDoc}
         */
        public String getName() {
            return name;
        }

        //-------------------------------------------------------------< Object >---
        /**
         * Two principals are equal, if their names are.
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof JackrabbitPrincipal) {
                return name.equals(((Principal) obj).getName());
            }
            return false;
        }

        /**
         * @return the hash code of the principals name.
         * @see Object#hashCode()
         */
        @Override
        public int hashCode() {
            return name.hashCode();
        }

        /**
         * @see Object#toString()
         */
        @Override
        public String toString() {
            return getClass().getName() + ":" + name;
        }
    }

}
