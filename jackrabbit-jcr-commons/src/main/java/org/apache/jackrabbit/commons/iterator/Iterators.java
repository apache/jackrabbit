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
package org.apache.jackrabbit.commons.iterator;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

/**
 * A utility class providing static access to <code>Iterator</code> wrappers
 * that are contained in this package.
 * <p>
 * By static importing
 * <code>org.apache.jackrabbit.commons.iterator.Iterators.iterable</code>, you
 * will be able to transform any {@link Iterator} from the JCR API into a
 * corresponding {@link Iterable} for use in a Java 5 foreach loop.
 * <p>
 * An example:
 * <p>
 * <code><pre>
 * import static org.apache.jackrabbit.commons.iterator.Iterators.iterable;
 *
 * // And then:
 * for (Node n : iterable(parent.getNodes())) {
 *   // Do stuff with n
 * }
 * </pre></code>
 *
 * @since Apache Jackrabbit 2.7
 */
public class Iterators {

    /**
     * Transform any type of {@link Iterator} into an {@link Iterable}
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    public static <I> Iterable<I> iterable(final Iterator<I> iterator) {
        return new Iterable<I>() {
            @Override
            public Iterator<I> iterator() {
                return iterator;
            }
        };
    }

    /**
     * Transform an {@link AccessControlPolicyIterator} into an {@link Iterable}
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<AccessControlPolicyIterator> iterable(AccessControlPolicyIterator iterator) {
        return iterable((Iterator<AccessControlPolicyIterator>) iterator);
    }

    /**
     * Transform an {@link EventIterator} into an {@link Iterable}
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<Event> iterable(EventIterator iterator) {
        return iterable((Iterator<Event>) iterator);
    }

    /**
     * Transform an {@link EventListenerIterator} into an {@link Iterable}
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<EventListener> iterable(EventListenerIterator iterator) {
        return iterable((Iterator<EventListener>) iterator);
    }

    /**
     * Transform an {@link NodeIterator} into an {@link Iterable}
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    public static Iterable<Node> iterable(NodeIterator iterator) {
        return new NodeIterable(iterator);
    }

    /**
     * Transform an {@link NodeTypeIterator} into an {@link Iterable}
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<NodeType> iterable(NodeTypeIterator iterator) {
        return iterable((Iterator<NodeType>) iterator);
    }

    /**
     * Transform an {@link PropertyIterator} into an {@link Iterable}
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    public static Iterable<Property> iterable(PropertyIterator iterator) {
        return new PropertyIterable(iterator);
    }

    /**
     * Transform an {@link RowIterator} into an {@link Iterable}
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    public static Iterable<Row> iterable(RowIterator iterator) {
        return new RowIterable(iterator);
    }

    /**
     * Transform an {@link VersionIterator} into an {@link Iterable}
     *
     * @param iterator
     *            The input <code>Iterator</code>
     * @return The wrapping <code>Iterable</code>
     */
    @SuppressWarnings("unchecked")
    public static Iterable<Version> iterable(VersionIterator iterator) {
        return iterable((Iterator<Version>) iterator);
    }

}
