package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.jcr2spi.SessionImpl;
import org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl.AccessControlListImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.test.NotExecutableException;
import org.junit.Test;

/**
 * Tests the functionality of the JCR AccessControlList API implementation. The
 * purpose is to test the consistency of the access control list by a, adding ,
 * deleting and modifying entries in the list.
 */
public class AccessControlListImplTest extends AbstractAccessControlTest {

    private SessionImpl sImpl;
    private NamePathResolver resolver;
    private QValueFactory vFactory;
    private Principal unknownPrincipal;
    private Principal secondPrincipal;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        sImpl = (SessionImpl) getSession();
        resolver = sImpl.getNamePathResolver();
        vFactory = sImpl.getQValueFactory();
        unknownPrincipal = getUnknownPrincipal();
        secondPrincipal = getAnotherUnknownPrincipal();
    }

    private JackrabbitAccessControlList createAccessControList(String aclPath)
            throws RepositoryException {
        return new AccessControlListImpl(aclPath, resolver, vFactory);
    }

    private Map<String, Value> createEmptyRestriction() {
        return Collections.<String, Value> emptyMap();
    }

    @Test
    public void testAddingDifferentEntries() throws RepositoryException {
        AccessControlEntry allow0, allow1, deny0, deny1 = null;
        JackrabbitAccessControlList acl = createAccessControList(path);

        // allow read to unknownPrincipal
        Privilege[] p = privilegesFromName(Privilege.JCR_READ);
        acl.addAccessControlEntry(unknownPrincipal, p);
        allow0 = acl.getAccessControlEntries()[0];

        // allow addChildNodes to secondPrincipal
        p = privilegesFromName(Privilege.JCR_ADD_CHILD_NODES);
        acl.addAccessControlEntry(secondPrincipal, p);
        allow1 = acl.getAccessControlEntries()[1];

        // deny modifyAccessControl to 'unknown' principal
        p = privilegesFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL);
        acl.addEntry(unknownPrincipal, p, false);
        deny0 = acl.getAccessControlEntries()[2];

        // deny jcr:nodeTypeManagement to secondPrincipal
        p = privilegesFromName(Privilege.JCR_NODE_TYPE_MANAGEMENT);
        acl.addEntry(secondPrincipal, p, false);
        deny1 = acl.getAccessControlEntries()[0];

        // four different entries
        assertEquals(4, acl.size());
        
        // UnknownPrincipal entries
        AccessControlEntry[] pentries = getEntries(acl, unknownPrincipal);
        assertEquals(2, pentries.length);
        
        // secondPrincipal entries
        AccessControlEntry[] sentries = getEntries(acl, secondPrincipal);
        assertEquals(2, sentries.length);
        
    }

    @Test
    public void testMultipleEntryEffect() throws RepositoryException {
        JackrabbitAccessControlList acl = createAccessControList(path);
        Privilege[] privileges = new Privilege[] { getACManager()
                .privilegeFromName(Privilege.JCR_READ) };

        // GRANT 'read' privilege to the Admin user -> list now contains one
        // allow entry
        boolean actual = acl.addEntry(unknownPrincipal, privileges, true,
                createEmptyRestriction());
        assertEquals(true, actual);

        // policy contains a single entry
        assertEquals(1, acl.size());

        AccessControlEntry[] entries = acl.getAccessControlEntries();

        // ... and the entry grants a single privilege
        assertEquals(1, entries[0].getPrivileges().length);

        // and the privilege must be a read privilege
        String n = privileges[0].toString();

        assertEquals("jcr:read", entries[0].getPrivileges()[0].getName());

        // GRANT 'add_child_node' privilege for the admin user -> same entry but
        // with an additional 'add_child_node' privilege.
        privileges = privilegesFromNames(new String[] {Privilege.JCR_ADD_CHILD_NODES, Privilege.JCR_READ });

        actual = acl.addEntry(unknownPrincipal, privileges, true,createEmptyRestriction());
        assertEquals(true, actual);

        // A new Entry wasn't added -> the existing entry was modified ->
        // entries count should still be 1.
        assertEquals(1, acl.size());

        // The single entry should now contain both 'read' and 'add_child_nodes'
        // privileges for the same principal.
        assertEquals(2, acl.getAccessControlEntries()[0].getPrivileges().length);

        // adding a privilege that's already granted for the same principal ->
        // no modifications to the list
        privileges = privilegesFromNames(new String[] { Privilege.JCR_READ });
        actual = acl.addEntry(unknownPrincipal, privileges, true, createEmptyRestriction());
        assertEquals(false, actual);

        // privileges = privilegesFromNames(new String[] {Privilege.JCR_READ});

        // revoke the read privilege
        actual = acl.addEntry(unknownPrincipal, privileges, false, createEmptyRestriction());
        assertEquals("Fail to revoke read privilege", true, actual);

        // should now be two entries -> an allow entry + a deny entry
        assertEquals(2, acl.size());

        // allow entry contains only a single privilege
        assertTrue(acl.getAccessControlEntries()[0].getPrivileges().length == 1);

        // ... and that privilege should not be a 'read' privilege -> was revoked
        String jcrName = acl.getAccessControlEntries()[0].getPrivileges()[0].getName();
        assertNotSame(getJcrName(Privilege.JCR_READ), jcrName);

        // deny entry contains a single privilege -> 'read' privilege
        jcrName = acl.getAccessControlEntries()[1].getPrivileges()[0].getName();
        assertEquals(getJcrName(Privilege.JCR_READ), jcrName);

        // remove the allow entry
        acl.removeAccessControlEntry(acl.getAccessControlEntries()[0]);

        // ... list should now only contain a single entry -> the deny entry
        assertTrue(acl.size() == 1);

        // remove the deny entry
        acl.removeAccessControlEntry(acl.getAccessControlEntries()[0]);

        // ... list must be empty at this point
        assertTrue(acl.isEmpty());

        // GRANT a read privilege
        privileges = privilegesFromNames(new String[] { Privilege.JCR_READ });
        actual = acl.addAccessControlEntry(unknownPrincipal, privileges);
        assertEquals("New Entry -> grants read privilege", true, actual);

        actual = acl.addEntry(unknownPrincipal, privileges, false,
                createEmptyRestriction());
        assertEquals("Fail to revoke the read privilege", true, actual);

        assertEquals(1, acl.size());

        // assertEquals( false, acl.getAccessControlEntries()[0].);
    }

    @Test
    public void testAddAggregatedPrivilegesSeparately() throws Exception {
        Privilege aggregate = null;
        Privilege[] privs = getACManager().getSupportedPrivileges(path);
        for (int i = 0; i < privs.length; i++) {
            if (privs[i].isAggregate()) {
                aggregate = privs[i];
            }
        }
        if (aggregate == null) {
            throw new NotExecutableException("No aggregate privilege supported at "+ path);
        }
        
        AccessControlList acl = createAccessControList(path);
        // ADD aggregate privilege
        acl.addAccessControlEntry(getUnknownPrincipal(), new Privilege[] {aggregate});
        
        Privilege[] aggregates = aggregate.getAggregatePrivileges();
        for (int i = 0; i < aggregates.length; i++) {
            boolean actual = acl.addAccessControlEntry(getUnknownPrincipal(), new Privilege[] {aggregates[i]});
            assertEquals(false, actual);
        }
    }
    @Test
    public void testAddAbstractPrivilege() throws NotExecutableException, RepositoryException {
        Privilege abstractPriv = null;
        Privilege[] allPrivs = getACManager().privilegeFromName(Privilege.JCR_ALL).getAggregatePrivileges();
        for (int i = 0; i < allPrivs.length; i++) {            
            if (allPrivs[i].isAbstract()) {
                abstractPriv = allPrivs[i];
                break;
            }
        }
        
        if (abstractPriv == null) {
            throw new NotExecutableException("No abstract privilege found");
        }
        
        AccessControlList acl = createAccessControList(path);
        try {
            acl.addAccessControlEntry(getUnknownPrincipal(), new Privilege[] {abstractPriv});
            fail("Adding an ACE with an abstract privilege must fail.");
        } catch (AccessControlException e) {
            // success
        }


    }
    
    
    // -------------------------------------------------------< utility methods >---
    private Name getQName(String name) throws RepositoryException {
        return resolver.getQName(name);
    }

    private String getJcrName(String name) throws RepositoryException {
        return resolver.getJCRName(getQName(name));
    }

    private AccessControlEntry[] getEntries(AccessControlList acl, Principal princ) throws RepositoryException {
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        List<AccessControlEntry> entriesPerPrincipal = new ArrayList<AccessControlEntry>(2);
        for (AccessControlEntry entry : entries) {
            if (entry.getPrincipal().getName().equals(princ.getName())) {
                entriesPerPrincipal.add(entry);
            }
        }
        return entriesPerPrincipal.toArray(new AccessControlEntry[entriesPerPrincipal.size()]);
    }
 }
