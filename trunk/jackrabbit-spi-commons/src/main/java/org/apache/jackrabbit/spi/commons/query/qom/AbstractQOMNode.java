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
package org.apache.jackrabbit.spi.commons.query.qom;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

import javax.jcr.NamespaceException;

/**
 * <code>AbstractQOMNode</code>...
 */
public abstract class AbstractQOMNode {

    protected final NamePathResolver resolver;

    public AbstractQOMNode(NamePathResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Accepts a <code>visitor</code> and calls the appropriate visit method
     * depending on the type of this QOM node.
     *
     * @param visitor the visitor.
     * @param data    user defined data, which is passed to the visit method.
     */
    public abstract Object accept(QOMTreeVisitor visitor, Object data) throws Exception;

    //---------------------------< internal >-----------------------------------

    /**
     * Returns the JCR name string for the given <code>Name</code> or
     * <code>null</code> if <code>name</code> is <code>null</code>.
     *
     * @param name the <code>Name</code>.
     * @return the prefixed JCR name or <code>name.toString()</code> if an
     *         unknown namespace URI is encountered.
     */
    protected String getJCRName(Name name) {
        if (name == null) {
            return null;
        }
        try {
            return resolver.getJCRName(name);
        } catch (NamespaceException e) {
            return name.toString();
        }
    }

    /**
     * Returns the JCR path String for the given <code>Path</code> object or
     * <code>null</code> if <code>path</code> is <code>null</code>.
     *
     * @param path A <code>Path</code> object.
     * @return JCR path in the standard form or <code>path.toString()</code>
     * if an unknown namespace URI is encountered.
     */
    protected String getJCRPath(Path path) {
        if (path == null) {
            return null;
        }
        try {
            return resolver.getJCRPath(path);
        } catch (NamespaceException e) {
            return path.toString();
        }
    }

    protected String quote(Name name) {
        String str = getJCRName(name);
        if (str.indexOf(':') != -1) {
            return "[" + str + "]";
        } else {
            return str;
        }
    }

    protected String quote(Path path) {
        String str = getJCRPath(path);
        if (str.indexOf(':') != -1 || str.indexOf('/') != -1) {
            return "[" + str + "]";
        } else {
            return str;
        }
    }

    protected String protect(Object expression) {
        String str = expression.toString();
        if (str.indexOf(" ") != -1) {
            return "(" + str + ")";
        } else {
            return str;
        }
    }

}
