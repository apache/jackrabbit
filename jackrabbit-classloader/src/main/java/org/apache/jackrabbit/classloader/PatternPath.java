/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.classloader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.util.ChildrenCollectorFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>PatternPath</code> implements a list of repository item path
 * patterns providing an iterator on the expanded paths. The list of patterns is
 * immutably configured from an array of strings.
 * <p>
 * While the original list of path patterns may be retrieved for informational
 * purposes by calling the {@link #getPath()} method, the primary contents of
 * instances of this class are the expanded paths accessible by calling the
 * {@link #getExpandedPaths()} method.
 * <p>
 * Please note that though this list is immutable there is intentionally no
 * guarantee that all invocations of the {@link #getExpandedPaths} method
 * return the same contents as the patterns contained in the list may expand to
 * different paths for each invocation of that method.
 * <p>
 * Each entry in the pattern list is a path whose segments conform to the
 * pattern syntax defined for the <code>Node.getNodes(String)</code> method.
 * The pattern may be a full name or a partial name with one or more wildcard
 * characters ("*"), or a disjunction (using the "|" character to represent
 * logical <i>OR</i>) of these. For example,
 * <blockquote><code>"jcr:*|foo:bar"</code></blockquote>
 * would match <code>"foo:bar"</code>, but also <code>"jcr:whatever"</code>.
 * <p>
 * The EBNF for pattern is:
 * <pre>
 * namePattern ::= disjunct {'|' disjunct}
 * disjunct ::= name [':' name]
 * name ::= '*' |
 *          ['*'] fragment {'*' fragment}['*']
 * fragment ::= char {char}
 * char ::= nonspace | ' '
 * nonspace ::= (* Any Unicode character except:
 *               '/', ':', '[', ']', '*',
 *               ''', '"', '|' or any whitespace
 *               character *)
 * </pre>
 *
 * @author Felix Meschberger
 */
/* package */ class PatternPath {

    /** default logger */
    private static final Logger log =
        LoggerFactory.getLogger(PatternPath.class);

    /** The session to access the repository */
    private final Session session;

    /** The list of path patterns */
    private final String[] patterns;

    /**
     * Creates a <code>PatternPath</code> containing the elements of the
     * string array. Each entry in the array which is either empty or
     * <code>null</code> is ignored and not added to the list. If the array
     * is empty or only contains empty or <code>null</code> elements, the
     * resulting list will consequently be empty.
     *
     * @param session The session to access the Repository to expand the paths
     *      and to register as an event listener.
     * @param pathPatterns The array of path patterns to add.
     *
     * @throws NullPointerException if the <code>pathPatterns</code> array or
     *      the <code>session</code> is <code>null</code>.
     */
    /* package */ PatternPath(Session session, String[] pathPatterns) {

        // check session
        if (session == null) {
            throw new NullPointerException("session");
        }

        // prepare the pattern list, excluding null/empty entries
        List patternList = new ArrayList();
        for (int i=0; i < pathPatterns.length; i++) {
            addChecked(patternList, pathPatterns[i]);
        }
        patterns =
            (String[]) patternList.toArray(new String[patternList.size()]);

        this.session = session;
    }

    /**
     * Returns the session from which this instance has been constructed.
     */
    /* package */ Session getSession() {
        return session;
    }

    /**
     * Returns a copy of the list of path patterns from which this instance has
     * been constructed.
     */
    /* package */  String[] getPath() {
        return (String[]) patterns.clone();
    }

    /**
     * Returns the list of expanded paths matching the list of patterns. This
     * list is guaranteed to only return existing items.
     * <p>
     * Each invocation of this method expands the pattern anew and returns a
     * new list instance.
     *
     * @return The list of paths matching the patterns. If the pattern list is
     *      empty or if no real paths match for any entry in the list, the
     *      returned list is empty.
     *
     * @throws RepositoryException if an error occurrs expanding the path
     *      pattern list.
     */
    /* package */ List getExpandedPaths() throws RepositoryException {
        List result = new ArrayList(patterns.length);
        Node root = session.getRootNode();

        for (int i=0; i < patterns.length; i++) {
            String entry = patterns[i];

            if (entry.indexOf('*') >= 0 || entry.indexOf('|') >= 0) {

                scan(root, entry, result);

            } else {
                // add path without pattern characters without further
                // checking. This allows adding paths which do not exist yet.
                result.add(entry);
            }

        }

        return result;
    }

    //---------- Object overwrite ----------------------------------------------

    /**
     * Returns <code>true</code> if this object equals the other object. This
     * implementation only returns true if the other object is the same as this
     * object.
     * <p>
     * The difference to the base class implementation is, that we only accept
     * equality if the other object is the same than this object. This is
     * actually the same implementation as the original <code>Object.equals</code>
     * implementation.
     *
     * @param o The other object to compare to.
     *
     * @return <code>true</code> if the other object is the same as this.
     */
    public boolean equals(Object o) {
        return o == this;
    }

    /**
     * Returns a hashcode for this instance. This is currently the hash code
     * returned by the parent implementation. While it does not violate the
     * contract to not change the <code>hashCode()</code> implementation but
     * to change the implementation of the {@link #equals} method, I think this
     * is ok, because our implementation of the {@link #equals} method is just
     * more specific than the base class implementation, which also allows
     * the other object to be a list with the same contents.
     *
     * @return The hash code returned by the base class implementation.
     */
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns a string representation of this instance. This is actually the
     * result of the string representation of the List, this actually is,
     * prefixed with the name of this class.
     *
     * @return The string representation of this instance.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("PatternPath: [");
        for (int i=0; i < patterns.length; i++) {
            if (i != 0) buf.append(", ");
            buf.append(patterns[i]);
        }
        buf.append("]");
        return buf.toString();
    }

    //---------- internal ------------------------------------------------------

    /**
     * Adds the string to the list of patterns, if neither empty nor
     * <code>null</code>. If the string has one or more trailing slashes
     * (<em>/</em>) they are removed before adding the string.
     */
    private void addChecked(List patternList, String pattern) {
        if (pattern == null || pattern.length() == 0) {
            log.debug("addChecked: Not adding null/empty pattern");
        } else {

            // remove all trailing slashes
            while (pattern.endsWith("/") && pattern.length() > 1) {
                pattern = pattern.substring(0, pattern.length()-1);
            }

            log.debug("addChecked: Adding {}");
            patternList.add(pattern);
        }
    }

    //---------- Path expansion -----------------------------------------------

    /**
     * Finds the paths of all nodes and properties matching the pattern below
     * the <code>root</code> node.
     *
     * @param root The root node of the subtree to match against the path
     *      pattern.
     * @param pathPattern The path pattern to use to find matching nodes.
     * @param gather The list into which the paths of matching child items
     *      are added.
     */
    private static void scan(Node root, String pathPattern, List gather)
            throws RepositoryException {

        // initial list of candidates is the root node
        List candidates = new ArrayList();
        candidates.add(root);

        StringTokenizer patterns = new StringTokenizer(pathPattern, "/");
        boolean moreTokens = patterns.hasMoreTokens();
        while (moreTokens) {
            String pattern = patterns.nextToken();
            moreTokens = patterns.hasMoreTokens();

            // new candidates are the children of the current candidates list
            // matching the current pattern
            List newCandidates = new ArrayList();
            for (Iterator ci=candidates.iterator(); ci.hasNext(); ) {
                Node current = (Node) ci.next();
                for (NodeIterator ni=current.getNodes(pattern); ni.hasNext(); ) {
                    newCandidates.add(ni.nextNode());
                }

                // if pattern is the last, also consider properties
                if (!moreTokens) {
                    PropertyIterator pi = current.getProperties(pattern);
                    while (pi.hasNext()) {
                        newCandidates.add(pi.nextProperty());
                    }
                }
            }

            // drop old candidates and use new for next step
            candidates.clear();
            candidates = newCandidates;
        }

        // add paths of the candidates to the gather list
        for (Iterator ci=candidates.iterator(); ci.hasNext(); ) {
            Item current = (Item) ci.next();
            gather.add(current.getPath());
        }
    }

    //---------- matching support ---------------------------------------------

    /**
     * Applies the list of path patterns to the given path returning
     * <code>true</code> if it matches, <code>false</code> otherwise.
     * <p>
     * <b><em>This method is package protected for testing purposes. This
     * method is not intended to be used by clients. Its specification or
     * implementation may change without notice.</em></b>
     *
     * @param path The path to match with the pattern list.
     *
     * @return <code>true</code> if the path matches any of the patterns.
     */
    /* package */ boolean matchPath(String path) {
        StringTokenizer exploded = new StringTokenizer(path, "/");

        OUTER_LOOP:
        for (int i=0; i < patterns.length; i++) {
            StringTokenizer exEntry = new StringTokenizer(patterns[i], "/");

            // ignore if the number of path elements to not match
            if (exploded.countTokens() != exEntry.countTokens()) {
                continue;
            }

            while (exploded.hasMoreTokens()) {
                if (!ChildrenCollectorFilter.matches(exploded.nextToken(),
                        exEntry.nextToken())) {
                    continue OUTER_LOOP;
                }
            }

            // if I get here, the path matches entry[i]
            return true;
        }

        // if we run out, no match has been found
        return false;
    }
}
