package org.apache.jackrabbit.core.security.principal;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;
import java.util.Enumeration;

import org.apache.jackrabbit.api.security.principal.GroupPrincipal;

/**
 * Helper class to deal with the migration between the 2 types of groups
 *
 */
public final class GroupPrincipals {

    private GroupPrincipals() {
    }

    /**
     * Checks if the provided principal is a group.
     *
     * @param principal
     *            to be checked.
     *
     * @return true if the principal is of type group.
     */
    public static boolean isGroup(Principal principal) {
        return principal instanceof Group || principal instanceof GroupPrincipal;
    }

    /**
     * Returns an enumeration of the members in the group.
     * @param principal the principal whose membership is listed.
     * @return an enumeration of the group members.
     */
    public static Enumeration<? extends Principal> members(Principal principal) {
        if (principal instanceof Group) {
            return ((Group) principal).members();
        }
        if (principal instanceof GroupPrincipal) {
            return ((GroupPrincipal) principal).members();
        }
        return Collections.emptyEnumeration();
    }

    /**
     * Returns true if the passed principal is a member of the group.
     * @param principal the principal whose members are being checked.
     * @param member the principal whose membership is to be checked.
     * @return true if the principal is a member of this group, false otherwise.
     */
    public static boolean isMember(Principal principal, Principal member) {
        if (principal instanceof Group) {
            return ((Group) principal).isMember(member);
        }
        if (principal instanceof GroupPrincipal) {
            return ((GroupPrincipal) principal).isMember(member);
        }
        return false;
    }
}
