/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.util;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;
import java.util.Collection;

/**
 * <code>ChildrenCollector</code> is a utility class
 * which can be used to 'collect' child elements of a
 * node. It implements the <code>ItemVisitor</code>
 * interface.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.12 $, $Date: 2004/08/02 16:19:52 $
 */
public class ChildrenCollector extends TraversingItemVisitor.Default {

    private final Collection children;
    private final boolean collectNodes;
    private final boolean collectProperties;

    /**
     * Constructs a <code>ChildrenCollector</code>
     *
     * @param children          where the matching children should be added
     * @param collectNodes      true, if child nodes should be collected; otherwise false
     * @param collectProperties true, if child properties should be collected; otherwise false
     * @param maxLevel          umber of hierarchy levels to traverse
     *                          (e.g. 1 for direct children only, 2 for children and their children, and so on)
     */
    public ChildrenCollector(Collection children, boolean collectNodes, boolean collectProperties, int maxLevel) {
	super(false, maxLevel);
	this.children = children;
	this.collectNodes = collectNodes;
	this.collectProperties = collectProperties;
    }

    /**
     * @see TraversingItemVisitor#entering(Node, int)
     */
    protected void entering(Node node, int level)
	    throws RepositoryException {
	if (level > 0 && collectNodes) {
	    children.add(node);
	}
    }

    /**
     * @see TraversingItemVisitor#entering(Property, int)
     */
    protected void entering(Property property, int level)
	    throws RepositoryException {
	if (level > 0 && collectProperties) {
	    children.add(property);
	}
    }
}
