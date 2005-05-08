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

final class PathParser {

    /**
     * Pattern used to validate and parse path elements:<p>
     * <ul>
     * <li>group 1 is .
     * <li>group 2 is ..
     * <li>group 3 is namespace prefix excl. delimiter (colon)
     * <li>group 4 is localName
     * <li>group 5 is index excl. brackets
     * </ul>
     */
    private static final Pattern PATH_ELEMENT_PATTERN = Pattern.compile(
            "(\\.)|(\\.\\.)|"
            + "(?:([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?"
            + "([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)"
            + "(?:\\[([1-9]\\d*)\\])?");

    private final Session session;

    public PathParser(Session session) {
        this.session = session;
    }

    public Path parsePath(String path)
            throws IllegalArgumentException, RepositoryException {
        PathBuilder builder = new PathBuilder();

        int p = path.indexOf('/');
        if (p == 0) {
            builder.addElement(new RootElement());
            path = path.substring(1);
            p = path.indexOf('/');
        }

        while (p != -1) {
            if (p > 0) {
                builder.addElement(parsePathElement(path.substring(0, p)));
            }
            path = path.substring(p + 1);
            p = path.indexOf('/');
        }

        if (path.length() > 0) {
            builder.addElement(parsePathElement(path));
        }

        return builder.getPath();
    }

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
