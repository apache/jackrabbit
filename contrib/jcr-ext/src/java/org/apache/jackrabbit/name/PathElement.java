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

/**
 * Path element. This interface is used by the {@link Path Path} class
 * to manage the various types of JCR path elements.
 * <p>
 * Each path element knows how to resolve itself in the context of a given
 * content item. The {@link #resolve(Item) resolve(Item)} method is used
 * to access this logic.
 */
interface PathElement {

    /**
     * Resolves this path element within the context of the given content
     * item. Retuns the result of the path element resolution.
     *
     * @param item the context item from which to resolve this path element
     * @return the resolved target item
     * @throws PathNotFoundException if the path element could not be resolved
     * @throws RepositoryException   if another error occurred
     */
    Item resolve(Item item) throws PathNotFoundException, RepositoryException;

}
