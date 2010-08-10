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
package org.apache.jackrabbit.core.session;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.ItemImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;

/**
 * Session operation for accessing an item at a given path. See the static
 * methods for factories of different kinds of item operations.
 */
public abstract class SessionItemOperation<T> implements SessionOperation<T> {

    private final String method;

    private final String path;

    private SessionItemOperation(String method, String path) {
        this.method = method;
        this.path = path;
    }

    public String toString() {
        return method + "(" + path + ")";
    }

    public T perform(SessionContext context) throws RepositoryException {
        try {
            Path normalized =
                context.getQPath(path).getNormalizedPath();
            if (normalized.isAbsolute()) {
                return perform(context.getItemManager(), normalized);
            } else {
                throw new RepositoryException("Not an absolute path: " + path);
            }
        } catch (AccessDeniedException e) {
            throw new PathNotFoundException(path);
        } catch (NameException e) {
            throw new RepositoryException("Invalid path:" + path, e);
        }
    }

    protected abstract T perform(ItemManager manager, Path path)
            throws RepositoryException;

    public static SessionItemOperation<Boolean> itemExists(String path) {
        return new SessionItemOperation<Boolean>("itemExists", path) {
            @Override @SuppressWarnings("deprecation")
            protected Boolean perform(ItemManager manager, Path path) {
                return manager.itemExists(path);
            }
        };
    }

    public static SessionItemOperation<Boolean> propertyExists(String path) {
        return new SessionItemOperation<Boolean>("propertyExists", path) {
            @Override
            protected Boolean perform(ItemManager manager, Path path) {
                return manager.propertyExists(path);
            }
        };
    }

    public static SessionItemOperation<Boolean> nodeExists(String path) {
        return new SessionItemOperation<Boolean>("nodeExists", path) {
            @Override
            protected Boolean perform(ItemManager manager, Path path) {
                return manager.nodeExists(path);
            }
        };
    }

    public static SessionItemOperation<ItemImpl> getItem(String path) {
        return new SessionItemOperation<ItemImpl>("getItem", path) {
            @Override @SuppressWarnings("deprecation")
            protected ItemImpl perform(ItemManager manager, Path path)
                    throws RepositoryException {
                return manager.getItem(path);
            }
        };
    }

    public static SessionItemOperation<PropertyImpl> getProperty(String path) {
        return new SessionItemOperation<PropertyImpl>("getProperty", path) {
            @Override
            protected PropertyImpl perform(ItemManager manager, Path path)
                    throws RepositoryException {
                return manager.getProperty(path);
            }
        };
    }

    public static SessionItemOperation<NodeImpl> getNode(String path) {
        return new SessionItemOperation<NodeImpl>("getNode", path) {
            @Override
            protected NodeImpl perform(ItemManager manager, Path path)
                    throws RepositoryException {
                return manager.getNode(path);
            }
        };
    }

    public static SessionItemOperation<Object> remove(String path) {
        return new SessionItemOperation<Object>("remove", path) {
            @Override  @SuppressWarnings("deprecation")
            protected Object perform(ItemManager manager, Path path)
                    throws RepositoryException {
                manager.getItem(path).remove();
                return this;
            }
        };
    }

}