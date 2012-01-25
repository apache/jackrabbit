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
package org.apache.jackrabbit.commons.packaging;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.predicate.Predicate;

public class FilterContentPackage implements ContentPackage {

    protected final List<Content> content = new ArrayList<Content>();

    protected boolean includeProperties = false;

    public void addContent(String path, Predicate filterList) {
        this.content.add(new Content(new String[] {path}, filterList));
    }

    public void addContent(String[] paths, Predicate filterList) {
        this.content.add(new Content(paths, filterList));
    }

    /**
     * @see org.apache.jackrabbit.commons.packaging.ContentPackage#getItems(javax.jcr.Session)
     */
    public Iterator<Item> getItems(Session session)
    throws RepositoryException {
        return new FilteringIterator(session, new ArrayList<Content>(this.content), this.includeProperties);
    }

    protected static class Content {
        protected final String[] paths;
        protected final Predicate filterList;

        public Content(String[] paths, Predicate filterList) {
            this.paths = paths;
            this.filterList = filterList;
        }
    }

    public static class FilteringIterator implements Iterator {

        /** The content we will iterate over. */
        protected final List<Content> content;

        /**
         * Filter that defines which items are included
         */
        protected Predicate includeFilter;

        protected int contentIndex, pathIndex;

        protected Item nextItem;

        protected Node lastNode;

        protected final Session session;

        protected final List<NodeIterator> nodeIteratorStack = new ArrayList<NodeIterator>();

        protected final boolean includeProperties;

        protected PropertyIterator propertyIterator;

        /**
         * Creates a new tree walker that uses the given filter as include and
         * traversal filter.
         *
         * @param session The session.
         * @param contentList The list of content objects.
         * @param includeProperties Should properties be included.
         */
        public FilteringIterator(final Session session,
                                 final List<Content> contentList,
                                 final boolean includeProperties) {
            this.content = contentList;
            this.session = session;
            this.includeProperties = includeProperties;
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            if ( this.nextItem != null ) {
                return true;
            }
            try {
                return this.checkForNextNode();
            } catch (RepositoryException e) {
                // if any error occurs, we stop iterating
                return false;
            }
        }

        protected boolean checkForNextNode() throws RepositoryException {
            if ( this.propertyIterator != null ) {
                if ( this.propertyIterator.hasNext() ) {
                    this.nextItem = this.propertyIterator.nextProperty();
                    return true;
                }
                this.propertyIterator = null;
            } else if ( this.includeProperties && this.lastNode != null ) {
                if ( this.lastNode.hasProperties() ) {
                    this.propertyIterator = this.lastNode.getProperties();
                    this.propertyIterator.hasNext();
                    this.nextItem = this.propertyIterator.nextProperty();
                    return true;
                }
            }
            if ( this.lastNode != null ) {

                if ( this.lastNode.hasNodes() ) {
                    final NodeIterator iter = this.lastNode.getNodes();
                    this.nodeIteratorStack.add(iter);
                }
                while ( this.nodeIteratorStack.size() > 0 ) {
                    final NodeIterator iter = (NodeIterator)this.nodeIteratorStack.get(this.nodeIteratorStack.size() - 1);
                    if ( iter.hasNext() ) {
                        do {
                            final Node contextNode = iter.nextNode();
                            if ( this.includeFilter.evaluate(contextNode) ) {
                                this.lastNode = contextNode;
                                this.nextItem = contextNode;
                                return true;
                            }
                        } while ( iter.hasNext() );
                    }
                    this.nodeIteratorStack.remove(iter);
                }
                this.pathIndex++;
                this.lastNode = null;
            }
            while ( this.contentIndex < this.content.size() ) {
                final Content content = (Content)this.content.get(this.contentIndex);
                this.includeFilter = content.filterList;
                while ( this.pathIndex < content.paths.length ) {
                    final String path = content.paths[this.pathIndex];
                    this.pathIndex++;
                    final Node contextNode = (Node)this.session.getItem(path);
                    if ( this.includeFilter.evaluate(contextNode) ) {
                        this.lastNode = contextNode;
                        this.nextItem = contextNode;
                        return true;
                    }
                }
                this.contentIndex++;
                this.pathIndex = 0;
            }

            return false;
        }

        /**
         * @see java.util.Iterator#next()
         */
        public Object next() {
            if ( this.hasNext() ) {
                final Item result = nextItem;
                this.nextItem = null;
                return result;
            }
            throw new NoSuchElementException("No more elements available");
        }

        /**
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported.");
        }
    }

    public boolean isIncludeProperties() {
        return includeProperties;
    }

    public void setIncludeProperties(boolean includeProperties) {
        this.includeProperties = includeProperties;
    }
}
