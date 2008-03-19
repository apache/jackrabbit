package org.apache.jackrabbit.core.security.jsr283.security;

/**
 * A privilege represents the capability of performing a particular set
 * of operations on items in the JCR repository. Each privilege is identified
 * by a NAME that is unique across the set of privileges supported by a
 * repository. JCR defines a set of standard privileges in the <code>jcr</code>
 * namespace. Implementations may add additional privileges in namespaces other
 * than <code>jcr</code>.
 * <p/>
 * A privilege may be an aggregate privilege. Aggregate privileges are sets of
 * other privileges. Granting, denying, or testing an aggregate privilege is
 * equivalent to individually granting, denying, or testing each privilege it
 * contains. The privileges contained by an aggregate privilege may themselves
 * be aggregate privileges if the resulting privilege graph is acyclic.
 * <p/>
 * A privilege may be an abstract privilege. Abstract privileges cannot
 * themselves be granted or denied, but can be composed into aggregate privileges
 * which are granted or denied.
 * <p/>
 * A privilege can be both aggregate and abstract.
 *
 * @since JCR 2.0
 */
public interface Privilege {

    /**
     * A constant representing <code>READ</code>, the privilege to retrieve
     * a node and get its properties and their values.
     */
    public static final String READ = "javax.jcr.security.Privilege.READ";

    /**
     * A constant representing <code>MODIFY_PROPERTIES</code>, the privilege
     * to create, modify and remove the properties of a node.
     */
    public static final String MODIFY_PROPERTIES = "javax.jcr.security.Privilege.MODIFY_PROPERTIES";

    /**
     * A constant representing <code>ADD_CHILD_NODES</code>, the privilege
     * to create child nodes of a node.
     */
    public static final String ADD_CHILD_NODES = "javax.jcr.security.Privilege.ADD_CHILD_NODES";

    /**
     * A constant representing <code>REMOVE_CHILD_NODES</code>, the privilege
     * to remove child nodes of a node.
     */
    public static final String REMOVE_CHILD_NODES = "javax.jcr.security.Privilege.REMOVE_CHILD_NODES";

    /**
     * A constant representing <code>WRITE</code>, an aggregate privilege that contains:
     *<ul>
     *  <li>MODIFY_PROPERTIES</li>
     *  <li>ADD_CHILD_NODES</li>
     *  <li>REMOVE_CHILD_NODES</li>
     * </ul>
     */
    public static final String WRITE = "javax.jcr.security.Privilege.WRITE";

    /**
     * A constant representing <code>READ_ACCESS_CONTROL</code>, the privilege
     * to get the access control policy of a node.
     */
    public static final String READ_ACCESS_CONTROL = "javax.jcr.security.Privilege.READ_ACCESS_CONTROL";

    /**
     * A constant representing <code>MODIFY_ACCESS_CONTROL</code>, the privilege
     * to modify the access control policies of a node.
     */
    public static final String MODIFY_ACCESS_CONTROL = "javax.jcr.security.Privilege.MODIFY_ACCESS_CONTROL";

    /**
     * A constant representing <code>ALL</code>, an aggregate privilege that contains
     * all predefined privileges:
     * <ul>
     *   <li>READ</li>
     *   <li>WRITE</li>
     *   <li>READ_ACCESS_CONTROL</li>
     *   <li>MODIFY_ACCESS_CONTROL</li>
     * </ul>
     * It should in addition include all implementation-defined privileges.
     */
    public static final String ALL = "javax.jcr.security.Privilege.ALL";

    /**
     * Returns the name of this privilege.
     *
     * @return the name of this privilege.
     */
    public String getName();

    /**
     * Returns a description of this privilege.
     *
     * @return a description of this privilege.
     */
    public String getDescription();

    /**
     * Returns whether this privilege is an abstract privilege.
     * @return <code>true</code> if this privilege is an abstract privilege;
     *         <code>false</code> otherwise.
     */
    public boolean isAbstract();

    /**
     * Returns whether this privilege is an aggregate privilege.
     * @return <code>true</code> if this privilege is an aggregate privilege;
     *         <code>false</code> otherwise.
     */
    public boolean isAggregate();

    /**
     * If this privilege is an aggregate privilege, returns the privileges directly
     * contained by the aggregate privilege. Otherwise returns an empty array.
     *
     * @return an array of <code>Privilege</code>s
     */
    public Privilege[] getDeclaredAggregatePrivileges();

    /**
     * If this privilege is an aggregate privilege, returns the privileges it
     * contains, the privileges contained by any aggregate privileges among
     * those, and so on (the transitive closure of privileges contained by this
     * privilege). Otherwise returns an empty array.
     *
     * @return an array of <code>Privilege</code>s
     */
    public Privilege[] getAggregatePrivileges();
}
