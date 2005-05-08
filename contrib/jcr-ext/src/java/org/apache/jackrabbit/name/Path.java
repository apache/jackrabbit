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

import javax.jcr.Item;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public final class Path {

    private final PathElement[] elements;

    Path(PathElement[] elements) {
        this.elements = elements;
    }

    public Item resolve(Item item)
            throws PathNotFoundException, RepositoryException {
        for (int i = 0; i < elements.length; i++) {
            item = elements[i].resolve(item);
        }
        return item;
    }

    public static Path parse(Session session, String path)
            throws IllegalArgumentException, RepositoryException {
        return new PathParser(session).parsePath(path);
    }

    public static Item resolve(Item item, String path)
            throws IllegalArgumentException, PathNotFoundException,
            RepositoryException {
        return parse(item.getSession(), path).resolve(item);
    }

}
