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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * TODO
 */
public class Path {

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
    private static final Pattern PATH_ELEMENT_PATTERN = Pattern.compile(
            "(\\.?)" + "|" + "(\\.\\.)" + "|" + "(([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)(\\[([1-9]\\d*)\\])?");

    public static Path parseJCRPath(Session session, String path)
            throws IllegalArgumentException, RepositoryException {
        Vector elements = new Vector();

        int p = path.indexOf('/');
        if (p == 0) {
            elements.add(RootElement.getInstance());
            path = path.substring(1);
            p = path.indexOf('/');
        }

        while (p != -1) {
            elements.add(parseJCRPathElement(session, path.substring(0, p)));
            path = path.substring(p + 1);
            p = path.indexOf('/');
        }

        elements.add(parseJCRPathElement(session, path));

        return new Path(elements); 
    }
    
    private static PathElement parseJCRPathElement(
            Session session, String element)
            throws IllegalArgumentException, RepositoryException {
        Matcher matcher = PATH_ELEMENT_PATTERN.matcher(element);
        if (matcher.matches()) {
            try {
                if (matcher.group(1) != null) {
                    return ThisElement.getInstance();
                } else if (matcher.group(2) != null) {
                    return ParentElement.getInstance();
                } else if (matcher.group(3) != null) {
                    return new NamedElement(
                            Name.parseJCRName(session, element));
                } else {
                    return new IndexedElement(
                            Name.parseJCRName(session, matcher.group(5)),
                            Integer.parseInt(matcher.group(6)));
                }
            } catch (NamespaceException e) {
                throw new IllegalArgumentException(
                        "Invalid path element " + element);
            }
        } else {
            throw new IllegalArgumentException(
                    "Invalid path element " + element);
        }
    }

    private final List elements;

    private Path(List elements) {
        this.elements = elements;
    }

    public Item walk(Item item)
            throws ItemNotFoundException, RepositoryException {
        Iterator iterator = elements.iterator();
        while (iterator.hasNext()) {
            PathElement element = (PathElement) iterator.next();
            item = element.step(item);
        }
        return item;
    }

}
