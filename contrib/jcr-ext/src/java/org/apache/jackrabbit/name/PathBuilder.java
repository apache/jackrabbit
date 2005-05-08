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

import java.util.List;
import java.util.Vector;

/**
 * Content path builder. This utility class uses the Builder design
 * pattern (GoF) to provide a simple mechanism for converting a sequence
 * of path elements into a path instance.
 *
 * @see PathParser
 */
final class PathBuilder {

    /** Path element list. Grows as more elements are added. */
    private final List elements;

    /**
     * Creates a path builder instance.
     */
    public PathBuilder() {
        elements = new Vector();
    }

    /**
     * Adds an element to the path being built.
     *
     * @param element path element
     */
    public void addElement(PathElement element) {
        elements.add(element);
    }

    /**
     * Creates a path instance from the collected sequence of path elements.
     *
     * @return path instance
     */
    public Path getPath() {
        return new Path((PathElement[])
                elements.toArray(new PathElement[elements.size()]));
    }

}
