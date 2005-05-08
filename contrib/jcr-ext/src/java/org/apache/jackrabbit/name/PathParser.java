/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.name;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * JCR content path parser. Instances of this class are used to parse
 * JCR content path strings into resolvable path instances. A path parser
 * instance is always associated with a given session that is used to
 * resolve namespace prefixes within the parsed paths.
 */
public final class PathParser {

    /**
     * Pattern used to validate and parse path elements.
     * <ul>
     * <li>group 1 is . (matches the full string)
     * <li>group 2 is .. (matches the full string)
     * <li>group 3 is the optional namespace prefix (without the colon)
     * <li>group 4 is the local part of the JCR name
     * <li>group 5 is the optional index (without the brackets)
     * </ul>
     */
    private static final Pattern PATH_ELEMENT_PATTERN = Pattern.compile(
            "(\\.)|(\\.\\.)|"
            + "(?:([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?"
            + "([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)"
            + "(?:\\[([1-9]\\d*)\\])?");

    /** Current session. Used to resolve namespace prefixes. */
    private final Session session;

    /**
     * Creates a path parser for the given session.
     *
     * @param session current session
     */
    public PathParser(Session session) {
        this.session = session;
    }

    /**
     * Parses the given JCR content path.
     *
     * @param path JCR content path
     * @return path instance
     * @throws IllegalArgumentException if the given path is invalid
     * @throws RepositoryException      if another error occurs
     */
    public Path parsePath(String path)
            throws IllegalArgumentException, RepositoryException {
        PathBuilder builder = new PathBuilder();
        int p = path.indexOf('/');

        if (p == 0) { // Check for an absolute path with a leading slash
            builder.addElement(new RootElement());
            path = path.substring(1);
            p = path.indexOf('/');
        }

        while (p != -1) {
            if (p > 0) { // Ignore empty path elements
                builder.addElement(parsePathElement(path.substring(0, p)));
            }
            path = path.substring(p + 1);
            p = path.indexOf('/');
        }

        if (path.length() > 0) { // Ignore empty trailing path elements
            builder.addElement(parsePathElement(path));
        }

        return builder.getPath();
    }

    /**
     * Parses the given path element.
     *
     * @param element path element
     * @return path element instance
     * @throws IllegalArgumentException if the given path element is invalid
     * @throws RepositoryException      if another error occurs
     */
    private PathElement parsePathElement(String element)
            throws IllegalArgumentException, RepositoryException {
        Matcher matcher = PATH_ELEMENT_PATTERN.matcher(element);
        if (matcher.matches()) {
            try {
                if (matcher.group(1) != null) {          // .
                    return new ThisElement();
                } else if (matcher.group(2) != null) {   // ..
                    return new ParentElement();
                } else {
                    String prefix = matcher.group(3);
                    String localPart = matcher.group(4);
                    String index = matcher.group(5);

                    String namespaceURI =
                        session.getNamespaceURI(prefix != null ? prefix : "");
                    Name name = new Name(namespaceURI, localPart);
                    if (index == null) {
                        return new NamedElement(name);
                    } else {
                        return new IndexedElement(name, Integer.parseInt(index));
                    }
                }
            } catch (NamespaceException e) {
                throw new IllegalArgumentException(
                        "Invalid path name prefix: " + element);
            }
        } else {
            throw new IllegalArgumentException(
                    "Invalid path name syntax: " + element);
        }
    }


}
