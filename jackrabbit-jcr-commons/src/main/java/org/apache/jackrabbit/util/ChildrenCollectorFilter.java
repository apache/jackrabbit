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
package org.apache.jackrabbit.util;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.jackrabbit.commons.ItemNameMatcher;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.PropertyIteratorAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * <code>ChildrenCollectorFilter</code> is a utility class
 * which can be used to 'collect' child items of a
 * node whose names match a certain pattern. It implements the
 * <code>ItemVisitor</code> interface.
 */
public class ChildrenCollectorFilter extends TraversingItemVisitor.Default {
    static final char WILDCARD_CHAR = '*';
    static final String OR = "|";

    private final Collection<Item> children;
    private final boolean collectNodes;
    private final boolean collectProperties;
    // namePattern and nameGlobs fields are used mutually exclusive
    private final String namePattern;
    private final String[] nameGlobs;

    /**
     * Constructs a <code>ChildrenCollectorFilter</code>
     *
     * @param namePattern       the pattern which should be applied to the names
     *                          of the children
     * @param children          where the matching children should be added
     * @param collectNodes      true, if child nodes should be collected; otherwise false
     * @param collectProperties true, if child properties should be collected; otherwise false
     * @param maxLevel          number of hierarchy levels to traverse
     *                          (e.g. 1 for direct children only, 2 for children and their children, and so on)
     */
    public ChildrenCollectorFilter(
            String namePattern, Collection<Item> children,
            boolean collectNodes, boolean collectProperties, int maxLevel) {
        super(false, maxLevel);
        this.namePattern = namePattern;
        this.nameGlobs = null;
        this.children = children;
        this.collectNodes = collectNodes;
        this.collectProperties = collectProperties;
    }

    /**
     * Constructs a <code>ChildrenCollectorFilter</code>
     *
     * @param nameGlobs         an array of globbing strings which should be
     *                          applied to the names of the children
     * @param children          where the matching children should be added
     * @param collectNodes      true, if child nodes should be collected; otherwise false
     * @param collectProperties true, if child properties should be collected; otherwise false
     * @param maxLevel          number of hierarchy levels to traverse
     *                          (e.g. 1 for direct children only, 2 for children and their children, and so on)
     */
    public ChildrenCollectorFilter(
            String[] nameGlobs, Collection<Item> children,
            boolean collectNodes, boolean collectProperties, int maxLevel) {
        super(false, maxLevel);
        this.nameGlobs = nameGlobs;
        this.namePattern = null;
        this.children = children;
        this.collectNodes = collectNodes;
        this.collectProperties = collectProperties;
    }

    public static NodeIterator collectChildNodes(
            Node node, String namePattern) throws RepositoryException {
        Collection<Item> nodes = new ArrayList<Item>();
        node.accept(new ChildrenCollectorFilter(
                namePattern, nodes, true, false, 1));
        return new NodeIteratorAdapter(nodes);
    }

    public static NodeIterator collectChildNodes(
            Node node, String[] nameGlobs) throws RepositoryException {
        Collection<Item> nodes = new ArrayList<Item>();
        node.accept(new ChildrenCollectorFilter(
                nameGlobs, nodes, true, false, 1));
        return new NodeIteratorAdapter(nodes);
    }

    public static PropertyIterator collectProperties(
            Node node, String namePattern) throws RepositoryException {
        Collection<Item> properties = Collections.emptySet();
        PropertyIterator pit = node.getProperties();
        while (pit.hasNext()) {
            Property p = pit.nextProperty();
            if (matches(p.getName(), namePattern)) {
                properties = addToCollection(properties, p);
            }
        }
        return new PropertyIteratorAdapter(properties);
    }

    public static PropertyIterator collectProperties(Node node, String[] nameGlobs) throws RepositoryException {
        Collection<Item> properties = Collections.emptySet();
        PropertyIterator pit = node.getProperties();
        while (pit.hasNext()) {
            Property p = pit.nextProperty();
            if (matches(p.getName(), nameGlobs)) {
                properties = addToCollection(properties, p);
            }
        }
        return new PropertyIteratorAdapter(properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void entering(Node node, int level)
            throws RepositoryException {
        if (level > 0 && collectNodes) {
            if (namePattern != null) {
                if (matches(node.getName(), namePattern)) {
                    children.add(node);
                }
            } else {
                if (matches(node.getName(), nameGlobs)) {
                    children.add(node);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void entering(Property property, int level)
            throws RepositoryException {
        if (level > 0 && collectProperties) {
            if (namePattern != null) {
                if (matches(property.getName(), namePattern)) {
                    children.add(property);
                }
            } else {
                if (matches(property.getName(), nameGlobs)) {
                    children.add(property);
                }
            }
        }
    }

    /**
     * Same as {@link ItemNameMatcher#matches(String, String)}.
     *
     * @see javax.jcr.Node#getNodes(String)
     */
    public static boolean matches(String name, String pattern) {
        return ItemNameMatcher.matches(name, pattern);
    }

    /**
     * Same as {@link ItemNameMatcher#matches(String, String)}.
     *
     * @see javax.jcr.Node#getNodes(String)
     */
    public static boolean matches(String name, String[] nameGlobs) {
        return ItemNameMatcher.matches(name, nameGlobs);
    }
    
    private static Collection<Item> addToCollection(Collection<Item> c, Item p) {
        Collection<Item> nc = c;
        if (c.isEmpty()) {
            nc = Collections.singleton(p);
        } else if (c.size() == 1) {
            nc = new ArrayList<Item>(c);
            nc.add(p);
        } else {
            nc.add(p);
        }

        return nc;
    }
}
