/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import javax.jcr.NamespaceException;
import javax.jcr.PathNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The <code>Path</code> utility class provides
 * misc. methods to resolve and nornalize JCR-style item paths.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.38 $, $Date: 2004/08/30 11:13:45 $
 */
public final class Path {

    /**
     * Pattern used to validate and parse path elements:<p>
     * <ul>
     * <li>group 1 is .
     * <li>group 2 is ..
     * <li>group 3 is namespace prefix incl. delimiter (colon)
     * <li>group 4 is namespace prefix excl. delimiter (colon)
     * <li>group 5 is localName
     * <li>group 6 is index incl. brackets
     * <li>group 7 is index excl. brackets
     * </ul>
     */
    private static final Pattern PATH_ELEMENT_PATTERN = Pattern.compile("(\\.)|(\\.\\.)|(([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)(\\[([1-9]\\d*)\\])?");

    private static final PathElement ROOT_ELEMENT = new RootElement();
    // .
    private static final PathElement CURRENT_ELEMENT = new CurrentElement();
    // ..
    private static final PathElement PARENT_ELEMENT = new ParentElement();

    private final PathElement[] elements;

    private int hashCode;
    private String string;

    /**
     * Private constructor
     *
     * @param elements
     */
    private Path(PathElement[] elements) {
	this.elements = elements;
	hashCode = 0;
    }

    //------------------------------------------------------< factory methods >
    /**
     * @param jcrPath
     * @param resolver
     * @param canonicalize
     * @return
     * @throws MalformedPathException
     */
    public static Path create(String jcrPath, NamespaceResolver resolver, boolean canonicalize)
	    throws MalformedPathException {
	PathElement[] elements = parse(jcrPath, null, resolver);
	if (canonicalize) {
	    return new Path(elements).getCanonicalPath();
	} else {
	    return new Path(elements);
	}
    }

    /**
     * @param master
     * @param relJCRPath
     * @param resolver
     * @param canonicalize
     * @return
     * @throws MalformedPathException
     */
    public static Path create(Path master, String relJCRPath, NamespaceResolver resolver, boolean canonicalize)
	    throws MalformedPathException {
	if (relJCRPath.startsWith("/")) {
	    throw new MalformedPathException("'" + relJCRPath + "' is not a relative path");
	}

	PathElement[] elements = parse(relJCRPath, master.getElements(), resolver);
	if (canonicalize) {
	    return new Path(elements).getCanonicalPath();
	} else {
	    return new Path(elements);
	}
    }

    /**
     * @param master
     * @param relPath
     * @param canonicalize
     * @return
     * @throws MalformedPathException
     */
    public static Path create(Path master, Path relPath, boolean canonicalize)
	    throws MalformedPathException {
	if (relPath.isAbsolute()) {
	    throw new MalformedPathException("relPath is not a relative path");
	}

	PathBuilder pb = new PathBuilder(master.getElements());
	pb.addAll(relPath.getElements());

	if (canonicalize) {
	    return pb.getPath().getCanonicalPath();
	} else {
	    return pb.getPath();
	}
    }

    /**
     * @param master
     * @param name
     * @param canonicalize
     * @return
     * @throws MalformedPathException
     */
    public static Path create(Path master, QName name, boolean canonicalize)
	    throws MalformedPathException {
	PathBuilder pb = new PathBuilder(master.getElements());
	pb.addLast(name.getNamespaceURI(), name.getLocalName());

	if (canonicalize) {
	    return pb.getPath().getCanonicalPath();
	} else {
	    return pb.getPath();
	}
    }

    /**
     * @param master
     * @param name
     * @param index
     * @param canonicalize
     * @return
     * @throws MalformedPathException
     */
    public static Path create(Path master, QName name, int index, boolean canonicalize)
	    throws MalformedPathException {
	PathBuilder pb = new PathBuilder(master.getElements());
	pb.addLast(name.getNamespaceURI(), name.getLocalName(), index);

	if (canonicalize) {
	    return pb.getPath().getCanonicalPath();
	} else {
	    return pb.getPath();
	}
    }

    //------------------------------------------------------< utility methods >
    /**
     * Checks if <code>jcrPath</code> is a valid JCR-style absolute or relative
     * path.
     *
     * @param jcrPath the path to be checked
     * @throws MalformedPathException If <code>jcrPath</code> is not a valid
     *                                JCR-style path.
     */
    public static void checkFormat(String jcrPath) throws MalformedPathException {
	if (jcrPath == null || jcrPath.length() == 0) {
	    throw new MalformedPathException("empty path");
	}
	// shortcut
	if (jcrPath.equals("/")) {
	    return;
	}

	// split path into path elements
	String[] elems = jcrPath.split("/", -1);
	for (int i = jcrPath.startsWith("/") ? 1 : 0; i < elems.length; i++) {
	    // validate path element
	    String elem = elems[i];
	    Matcher matcher = PATH_ELEMENT_PATTERN.matcher(elem);
	    if (!matcher.matches()) {
		// illegal syntax for path element
		throw new MalformedPathException("'" + jcrPath + "' is not a valid path: '" + elem + "' is not a legal path element");
	    }
	}
    }

    //-------------------------------------------------------< public methods >
    /**
     * Tests whether this path represents the root path, i.e. "/".
     *
     * @return true if this path represents the root path; false otherwise.
     */
    public boolean denotesRoot() {
	return elements.length == 1 && elements[0].denotesRoot();
    }

    /**
     * Tests whether this path is absolute, i.e. whether it starts with "/".
     *
     * @return true if this path is absolute; false otherwise.
     */
    public boolean isAbsolute() {
	return elements.length > 0 && elements[0].denotesRoot();
    }

    /**
     * Tests whether this path is canonical, i.e. whether it is absolute and
     * does not contain redundant names such as "." and "..".
     *
     * @return true if this path is canonical; false otherwise.
     * @see #isAbsolute()
     */
    public boolean isCanonical() {
	if (!isAbsolute()) {
	    return false;
	}
	// check path for "." and ".." names
	for (int i = 0; i < elements.length; i++) {
	    if (elements[i].equals(CURRENT_ELEMENT)
		    || elements[i].equals(PARENT_ELEMENT)) {
		return false;
	    }
	}
	return true;
    }

    /**
     * Returns the canonical path representation of this path. This typically
     * involves removing/resolving redundant names such as "." and ".." from
     * the path.
     *
     * @return a canonical path representation of this path
     * @throws MalformedPathException if this path can not be canonicalized
     *                                (e.g. if it is relative)
     */
    public Path getCanonicalPath() throws MalformedPathException {
	if (isCanonical()) {
	    return this;
	}
	if (!isAbsolute()) {
	    throw new MalformedPathException("only an absolute path can be canonicalized.");
	}

	LinkedList queue = new LinkedList();
	for (int i = 0; i < elements.length; i++) {
	    PathElement elem = elements[i];

	    if (elem.equals(CURRENT_ELEMENT)) {
		continue;
	    } else if (elem.equals(PARENT_ELEMENT)) {
		if (queue.size() <= 1) {
		    // the first element is the root element;
		    // ".." would refer to the parent of root
		    throw new MalformedPathException("path can not be canonicalized: unresolvable '..' element");
		}
		queue.removeLast();
	    } else {
		queue.add(elem);
	    }
	}
	return new Path((PathElement[]) queue.toArray(new PathElement[queue.size()]));
    }

    /**
     * Returns the ancestor path of the specified relative degree.
     * <p/>
     * An ancestor of relative degree <i>x</i> is the path that is <i>x</i>
     * levels up along the path.
     * <ul>
     * <li><i>degree</i> = 0 returns this path.
     * <li><i>degree</i> = 1 returns the parent of this path.
     * <li><i>degree</i> = 2 returns the grandparent of this path.
     * <li>And so on to <i>degree</i> = <i>n</i>, where <i>n</i> is the depth
     * of this path, which returns the root path.
     * </ul>
     *
     * @param degree the relative degree of the requested ancestor.
     * @return the ancestor path of the specified degree.
     * @throws PathNotFoundException    if there is no ancestor of the specified
     *                                  degree
     * @throws IllegalArgumentException if <code>degree</code> is negative
     */
    public Path getAncestor(int degree) throws PathNotFoundException {
	if (degree < 0) {
	    throw new IllegalArgumentException("degree must be >= 0");
	} else if (degree == 0) {
	    return this;
	}
	int length = elements.length - degree;
	if (length < 1) {
	    throw new PathNotFoundException("no such ancestor path of degree " + degree);
	}
	PathElement[] elements = new PathElement[length];
	for (int i = 0; i < length; i++) {
	    elements[i] = this.elements[i];
	}
	return new Path(elements);
    }

    /**
     * Returns the number of ancestors of this path.
     *
     * @return the number of ancestors of this path
     */
    public int getAncestorCount() {
	return elements.length - 1;
    }

    /**
     * Determines if <i>this</i> path is an ancestor of the specified path.
     *
     * @return <code>true</code> if <code>other</code> is a descendant;
     *         otherwise <code>false</code>
     * @throws MalformedPathException if either the specified path or this path
     *                                is a relative path.
     */
    public boolean isAncestorOf(Path other) throws MalformedPathException {
	if (equals(other)) {
	    return false;
	}
	if (other == null) {
	    throw new IllegalArgumentException("null argument");
	}
	// make sure we're comparing canonical paths
	Path p0 = getCanonicalPath();
	Path p1 = other.getCanonicalPath();
	if (p0.elements.length >= p1.elements.length) {
	    return false;
	}
	for (int i = 0; i < p0.elements.length; i++) {
	    if (!p0.elements[i].equals(p1.elements[i])) {
		 return false;
	    }
	}
	return true;
    }

    /**
     * Determines if <i>this</i> path is a descendant of the specified path.
     *
     * @return <code>true</code> if <code>other</code> is an ancestor;
     *         otherwise <code>false</code>
     * @throws MalformedPathException if either the specified path or this path
     *                                is a relative path.
     */
    public boolean isDescendantOf(Path other) throws MalformedPathException {
	if (equals(other)) {
	    return false;
	}
	if (other == null) {
	    throw new IllegalArgumentException("null argument");
	}
	return other.isAncestorOf(this);
    }

    /**
     * Returns the name element (i.e. the last element) of this path.
     *
     * @return the name element of this path
     */
    public PathElement getNameElement() {
	return elements[elements.length - 1];
    }

    /**
     * Returns the elements of this path.
     *
     * @return the elements of this path.
     */
    public PathElement[] getElements() {
	return elements;
    }

    /**
     * @param resolver
     * @return
     * @throws NoPrefixDeclaredException
     */
    public String toJCRPath(NamespaceResolver resolver) throws NoPrefixDeclaredException {
	if (denotesRoot()) {
	    // shortcut
	    return "/";
	}
	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < elements.length; i++) {
	    if (i > 0) {
		sb.append('/');
	    }
	    PathElement element = elements[i];
	    // name
	    sb.append(element.toJCRName(resolver));
	}
	return sb.toString();
    }

    /**
     * Returns the internal string representation of this <code>Path</code>.
     * <p/>
     * Note that the returned string is not a valid JCR path, i.e. the
     * namespace URI's of the individual path elements are not replaced with
     * their mapped prefixes. Call
     * <code>{@link #toJCRPath(NamespaceResolver)}</code>
     * for a JCR path representation.
     *
     * @return the internal string representation of this <code>Path</code>.
     */
    public String toString() {
	// Path is immutable, we can store the string representation
	if (string == null) {
	    StringBuffer sb = new StringBuffer();
	    for (int i = 0; i < elements.length; i++) {
		if (i > 0) {
		    // @todo find safe path separator char that does not conflict with chars in serialized QName
		    sb.append('\t');
		}
		PathElement element = elements[i];
		String elem = element.toString();
		sb.append(elem);
	    }
	    string = sb.toString();
	}
	return string;
    }

    /**
     * Returns a <code>Path</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>Path.toString()</code> method.
     *
     * @param s a <code>String</code> containing the <code>Path</code>
     *          representation to be parsed.
     * @return the <code>Path</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as a <code>Path</code>.
     * @see #toString()
     */
    public static Path valueOf(String s) {
	if ("".equals(s) || s == null) {
	    throw new IllegalArgumentException("invalid Path literal");
	}

	// split into path elements

	// @todo find safe path separator char that does not conflict with chars in serialized QName
	String[] elements = s.split("\t", -1);
	ArrayList list = new ArrayList();
	for (int i = 0; i < elements.length; i++) {
	    String elem = elements[i];
	    list.add(PathElement.fromString(elem));
	}

	return new Path((PathElement[]) list.toArray(new PathElement[list.size()]));
    }

    /**
     * Returns a hash code value for this path.
     *
     * @return a hash code value for this path.
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
	if (hashCode == 0) {
	    hashCode = 1;
	    for (int i = 0; i < elements.length; i++) {
		hashCode = 43 * hashCode + elements[i].hashCode();
	    }
	}
	return hashCode;
    }

    /**
     * Compares the specified object with this path for equality.
     *
     * @param obj the object to be compared for equality with this path.
     * @return <tt>true</tt> if the specified object is equal to this path.
     */
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof Path) {
	    Path other = (Path) obj;
	    return Arrays.equals(elements, other.elements);
	}
	return false;
    }

    //--------------------------------------------------------< inner classes >
    public static final class PathBuilder implements Cloneable {
	private final LinkedList queue;

	public PathBuilder() {
	    queue = new LinkedList();
	}

	public PathBuilder(PathElement[] elements) {
	    this();
	    addAll(elements);
	}

	public void addRoot() {
	    queue.addFirst(ROOT_ELEMENT);
	}

	public void addAll(PathElement[] elements) {
	    for (int i = 0; i < elements.length; i++) {
		queue.add(elements[i]);
	    }
	}

	public void addFirst(String nameSpaceURI, String localName) {
	    queue.addFirst(new PathElement(nameSpaceURI, localName));
	}

	public void addFirst(String nameSpaceURI, String localName, int index) {
	    queue.addFirst(new PathElement(nameSpaceURI, localName, index));
	}

	public void addFirst(QName name) {
	    queue.addFirst(new PathElement(name));
	}

	public void addFirst(QName name, int index) {
	    queue.addFirst(new PathElement(name, index));
	}

	public void addLast(String nameSpaceURI, String localName) {
	    queue.addLast(new PathElement(nameSpaceURI, localName));
	}

	public void addLast(String nameSpaceURI, String localName, int index) {
	    queue.addLast(new PathElement(nameSpaceURI, localName, index));
	}

	public void addLast(QName name) {
	    queue.addLast(new PathElement(name));
	}

	public void addLast(QName name, int index) {
	    queue.addLast(new PathElement(name, index));
	}

	public Path getPath() throws MalformedPathException {
	    PathElement[] elements = (PathElement[]) queue.toArray(new PathElement[queue.size()]);
	    // validate path
	    if (elements.length == 0) {
		throw new MalformedPathException("empty path");
	    }
	    for (int i = 1; i < elements.length; i++) {
		if (elements[i].denotesRoot()) {
		    throw new MalformedPathException("path contains invalid root element(s)");
		}
		String localName = elements[i].getName().getLocalName();
		Matcher matcher = PATH_ELEMENT_PATTERN.matcher(localName);
		if (!matcher.matches()) {
		    // illegal syntax for path element
		    throw new MalformedPathException(localName + "' is not a legal path element");
		}
	    }
	    return new Path(elements);
	}

	public Object clone() {
	    PathBuilder clone = new PathBuilder();
	    clone.queue.addAll(queue);
	    return clone;
	}
    }

    public static class RootElement extends PathElement {
	// use a literal that is an illegal name character to avoid collisions
	static final String LITERAL = "*";

	private RootElement() {
	    super(NamespaceRegistryImpl.NS_DEFAULT_URI, "");
	}

	// PathElement override
	public boolean denotesRoot() {
	    return true;
	}

	// PathElement override
	public String toJCRName(NamespaceResolver resolver) throws NoPrefixDeclaredException {
	    return "";
	}

	// Object override
	public String toString() {
	    return LITERAL;
	}
    }

    public static class CurrentElement extends PathElement {
	static final String LITERAL = ".";

	private CurrentElement() {
	    super(NamespaceRegistryImpl.NS_DEFAULT_URI, LITERAL);
	}

	// PathElement override
	public String toJCRName(NamespaceResolver resolver) throws NoPrefixDeclaredException {
	    return LITERAL;
	}

	// Object override
	public String toString() {
	    return LITERAL;
	}
    }

    public static class ParentElement extends PathElement {
	static final String LITERAL = "..";

	private ParentElement() {
	    super(NamespaceRegistryImpl.NS_DEFAULT_URI, LITERAL);
	}

	// PathElement override
	public String toJCRName(NamespaceResolver resolver) throws NoPrefixDeclaredException {
	    return LITERAL;
	}

	// Object override
	public String toString() {
	    return LITERAL;
	}
    }

    public static class PathElement {

	private final QName name;

	/**
	 * 1-based index; 0 if not explicitly specified (which is equivalent to
	 * specifying 1)
	 */
	private final int index;

	private PathElement(String namespaceURI, String localName) {
	    this(new QName(namespaceURI, localName));
	}

	private PathElement(String namespaceURI, String localName, int index) {
	    this(new QName(namespaceURI, localName), index);
	}

	private PathElement(QName name) {
	    if (name == null) {
		throw new IllegalArgumentException("name must not be null");
	    }
	    this.name = name;
	    this.index = 0;
	}

	private PathElement(QName name, int index) {
	    if (name == null) {
		throw new IllegalArgumentException("name must not be null");
	    }
	    if (index < 1) {
		throw new IllegalArgumentException("index is 1-based");
	    }
	    this.index = index;
	    this.name = name;
	}

	/**
	 * Returns the name of this path element.
	 *
	 * @return the name
	 */
	public QName getName() {
	    return name;
	}

	/**
	 * Returns the 1-based index or 0 if no index was specified (which is
	 * equivalent to specifying 1).
	 *
	 * @return Returns the 1-based index or 0 if no index was specified.
	 */
	public int getIndex() {
	    return index;
	}

	public boolean denotesRoot() {
	    return false;
	}

	public String toJCRName(NamespaceResolver resolver) throws NoPrefixDeclaredException {
	    StringBuffer sb = new StringBuffer();
	    // name
	    sb.append(name.toJCRName(resolver));
	    // index
	    int index = getIndex();
	    /** FIXME the [1] subscript should only be suppressed if the item
	     * in question can't have same-name siblings.
	     */
	    //if (index > 0) {
	    if (index > 1) {
		sb.append('[');
		sb.append(index);
		sb.append(']');
	    }
	    return sb.toString();
	}

	public String toString() {
	    StringBuffer sb = new StringBuffer();
	    // name
	    sb.append(name.toString());
	    // index
	    int index = getIndex();
	    if (index > 0) {
		sb.append('[');
		sb.append(index);
		sb.append(']');
	    }
	    return sb.toString();
	}

	public static PathElement fromString(String s) {
	    if (s == null) {
		throw new IllegalArgumentException("null PathElement literal");
	    }
	    if (s.equals(RootElement.LITERAL)) {
		return ROOT_ELEMENT;
	    } else if (s.equals(CurrentElement.LITERAL)) {
		return CURRENT_ELEMENT;
	    } else if (s.equals(ParentElement.LITERAL)) {
		return PARENT_ELEMENT;
	    }

	    int pos = s.indexOf('[');
	    if (pos == -1) {
		QName name = QName.valueOf(s);
		return new PathElement(name.getNamespaceURI(), name.getLocalName());
	    }
	    QName name = QName.valueOf(s.substring(0, pos));
	    int pos1 = s.indexOf(']');
	    if (pos1 == -1) {
		throw new IllegalArgumentException("invalid PathElement literal: " + s + " (missing ']')");
	    }
	    try {
		int index = Integer.valueOf(s.substring(pos + 1, pos1)).intValue();
		if (index < 1) {
		    throw new IllegalArgumentException("invalid PathElement literal: " + s + " (index is 1-based)");
		}
		return new PathElement(name.getNamespaceURI(), name.getLocalName(), index);
	    } catch (Throwable t) {
		throw new IllegalArgumentException("invalid PathElement literal: " + s + " (" + t.getMessage() + ")");
	    }
	}

	public int hashCode() {
	    // @todo treat index==0 as index==1?
	    return 73 * index + name.hashCode();
	}

	public boolean equals(Object obj) {
	    if (this == obj) {
		return true;
	    }
	    if (obj instanceof PathElement) {
		PathElement other = (PathElement) obj;
		return name.equals(other.name)
			// @todo treat index==0 as index==1?
			&& index == other.index;
	    }
	    return false;
	}
    }

    //-------------------------------------------------------< implementation >
    private static PathElement[] parse(String jcrPath, PathElement[] master, NamespaceResolver resolver)
	    throws MalformedPathException {
	// shortcut
	if (jcrPath.equals("/")) {
	    return new PathElement[]{ROOT_ELEMENT};
	}

	// split path into path elements
	String[] elems = jcrPath.split("/", -1);
	if (elems.length == 0) {
	    throw new MalformedPathException("empty path");
	}

	ArrayList list = new ArrayList();
	if (master != null) {
	    // a master path was specified; the 'path' argument is assumed
	    // to be a relative path
	    for (int i = 0; i < master.length; i++) {
		list.add(master[i]);
	    }
	}

	for (int i = 0; i < elems.length; i++) {
	    // validate & parse path element
	    String prefix;
	    String localName;
	    int index;

	    String elem = elems[i];
	    if (i == 0 && elem.length() == 0) {
		// path is absolute, i.e. the first element is the root element
		if (!list.isEmpty()) {
		    throw new MalformedPathException("'" + jcrPath + "' is not a relative path");
		}
		list.add(ROOT_ELEMENT);
		continue;
	    }
	    Matcher matcher = PATH_ELEMENT_PATTERN.matcher(elem);
	    if (matcher.matches()) {
		if (matcher.group(1) != null) {
		    // group 1 is .
		    list.add(CURRENT_ELEMENT);
		} else if (matcher.group(2) != null) {
		    // group 2 is ..
		    list.add(PARENT_ELEMENT);
		} else {
		    // element is a name

		    // check for prefix (group 3)
		    if (matcher.group(3) != null) {
			// prefix specified
			// group 4 is namespace prefix excl. delimiter (colon)
			prefix = matcher.group(4);
		    } else {
			// no prefix specified
			prefix = "";
		    }

		    // group 5 is localName
		    localName = matcher.group(5);

		    // check for index (group 6)
		    if (matcher.group(6) != null) {
			// index specified
			// group 7 is index excl. brackets
			index = Integer.parseInt(matcher.group(7));
		    } else {
			// no index specified
			index = 0;
		    }

		    String nsURI;
		    try {
			nsURI = resolver.getURI(prefix);
		    } catch (NamespaceException nse) {
			// unknown prefix
			throw new MalformedPathException("'" + jcrPath + "' is not a valid path: '" + elem + "' specifies an unmapped namespace prefix");
		    }

		    PathElement element;
		    if (index == 0) {
			element = new PathElement(nsURI, localName);
		    } else {
			element = new PathElement(nsURI, localName, index);
		    }
		    list.add(element);
		}
	    } else {
		// illegal syntax for path element
		throw new MalformedPathException("'" + jcrPath + "' is not a valid path: '" + elem + "' is not a legal path element");
	    }
	}
	return (PathElement[]) list.toArray(new PathElement[list.size()]);
    }
}
