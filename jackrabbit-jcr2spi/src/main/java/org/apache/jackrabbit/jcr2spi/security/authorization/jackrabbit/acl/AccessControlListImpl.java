package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.AccessControlConstants;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AccessControlListImpl implements JackrabbitAccessControlList, AccessControlConstants {

    private static final Logger log = LoggerFactory.getLogger(AccessControlListImpl.class);

    private final String jcrPath;

    private final List<AccessControlEntry> entries;
    private final QValueFactory qValueFactory;
    private final NamePathResolver resolver;

    /**
     * Namespace sensitive name of the REP_GLOB property in standard JCR form.
     */
    private final String jcrRepGlob;

    public AccessControlListImpl(String jcrPath, NamePathResolver resolver, QValueFactory qValueFactory) throws RepositoryException {
        this.jcrPath = jcrPath;
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
            try {

                PropertyState ps = aceNode.getPropertyState(N_REP_PRINCIPAL_NAME);

                // rep:principal property
                String principalName = ps.getValue().getString();
                Principal principal = createPrincipal(principalName);

                // rep:privileges property
                ps = aceNode.getPropertyState(N_REP_PRIVILEGES);

                QValue[] values = ps.getValues();
                Privilege[] privileges = new Privilege[values.length];
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
                AccessControlEntry ace = new AccessControlEntryImpl(principal, privileges, isAllow, restrictions, resolver, qValueFactory);
                entries.add(ace);
            } catch (RepositoryException e) {
                log.debug("Fail to create Entry for "+ aceNode.getName().toString());
            }
        }
    }

    //--------------------------------------------------< AccessControlList >---
    @Override
    public AccessControlEntry[] getAccessControlEntries()
            throws RepositoryException {
        return entries.toArray(new AccessControlEntry[entries.size()]);
    }

    @Override
    public boolean addAccessControlEntry(Principal principal, Privilege[] privileges) throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, true, Collections.<String, Value>emptyMap());
    }

    @Override
    public void removeAccessControlEntry(AccessControlEntry ace)
            throws AccessControlException, RepositoryException {
        if (entries.contains(ace)) {
            entries.remove(ace);
        } else {
            throw new AccessControlException("Entry not present in this list");
        }
    }

    //----------------------------------------< JackrabbitAccessControlList >---
    @Override
    public String getPath() {
        return jcrPath;
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public int size() {
        return entries.size();
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
    public boolean addEntry(Principal principal, Privilege[] privileges,
                            boolean isAllow, Map<String, Value> restrictions)
            throws AccessControlException, RepositoryException {

        // create entry to be added
        Map<String, QValue> rs = createRestrictions(restrictions);
        AccessControlEntry entry = createEntry(principal, privileges, isAllow, rs);

        return entries.add(entry);
    }

    @Override
    public String[] getRestrictionNames() throws RepositoryException {
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

    @Override
    public void orderBefore(AccessControlEntry srcEntry,
                            AccessControlEntry destEntry) throws AccessControlException,
            UnsupportedRepositoryOperationException, RepositoryException {
        // TODO
        throw new UnsupportedRepositoryOperationException("not yet implemented");
    }

    //------------------------------------------------------------< private >---
    private AccessControlEntry createEntry(Principal principal, Privilege[] privileges, boolean isAllow, Map<String, QValue> restrictions) throws RepositoryException {
        return new AccessControlEntryImpl(principal, privileges, isAllow, restrictions, resolver, qValueFactory);
    }

    private Map<String, QValue> createRestrictions(Map<String, Value> restrictions) throws RepositoryException {
        Map<String, QValue> rs = new HashMap<String, QValue>(restrictions.size());
        for (String restName : restrictions.keySet()) {
            QValue restValue = qValueFactory.create(restrictions.get(restName).getString(), PropertyType.STRING);
            rs.put(restName, restValue);
        }
        return rs;
    }

    private static Principal createPrincipal(final String name) {
        return new Principal() {
            @Override
            public String getName() {
                return name;
            }
        };
    }
}
