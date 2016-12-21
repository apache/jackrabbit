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
package org.apache.jackrabbit.spi.commons.nodetype.compact;

import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.SessionNamespaceResolver;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;

import javax.jcr.NamespaceException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

/**
 * Prints node type defs in a compact notation
 * Print Format:
 * &lt;ex = "http://apache.org/jackrabbit/example"&gt;
 * [ex:NodeType] &gt; ex:ParentType1, ex:ParentType2
 * orderable mixin
 *   - ex:property (STRING) = 'default1', 'default2'
 *     primary mandatory autocreated protected multiple VERSION
 *     &lt; 'constraint1', 'constraint2'
 *   + ex:node (ex:reqType1, ex:reqType2) = ex:defaultType
 *     mandatory autocreated protected multiple VERSION
 */
public class CompactNodeTypeDefWriter extends org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefWriter {

    /**
     * the current name/path resolver
     */
    private final NamePathResolver npResolver;

    /**
     * Creates a new nodetype writer based on a session
     *
     * @param out the underlying writer
     * @param s repository session
     * @param includeNS if <code>true</code> all used namespace declarations
     *                  are also written to the writer
     */
    public CompactNodeTypeDefWriter(Writer out, Session s, boolean includeNS) {
        this(out, new SessionNamespaceResolver(s), new DefaultNamePathResolver(s), includeNS);
    }

    /**
     * Creates a new nodetype writer based on a namespace resolver
     *
     * @param out the underlying writer
     * @param r the namespace resolver
     * @param includeNS if <code>true</code> all used namespace decl. are also
     *                  written to the writer
     */
    public CompactNodeTypeDefWriter(Writer out, NamespaceResolver r, boolean includeNS) {
        this(out, r, new DefaultNamePathResolver(r), includeNS);
    }

    /**
     * Creates a new nodetype writer that does not include namespaces.
     *
     * @param out the underlying writer
     * @param r the namespace resolver
     * @param npResolver name-path resolver
     */
    public CompactNodeTypeDefWriter(Writer out,
                                    NamespaceResolver r,
                                    NamePathResolver npResolver) {
        this(out, r, npResolver, false);
    }

    /**
     * Creates a new nodetype writer
     *
     * @param out the underlying writer
     * @param r the namespace resolver
     * @param npResolver name-path resolver
     * @param includeNS if <code>true</code> all used namespace decl. are also
     *                  written to the writer
     */
    public CompactNodeTypeDefWriter(Writer out,
                                    final NamespaceResolver r,
                                    NamePathResolver npResolver,
                                    boolean includeNS) {
        super(out, createNsMapping(r), includeNS);
        this.npResolver = npResolver;
    }

    /**
     * Writes the given list of QNodeTypeDefinition to the output writer including the
     * used namespaces.
     *
     * @param defs collection of definitions
     * @param r namespace resolver
     * @param npResolver name-path resolver
     * @param out output writer
     * @throws IOException if an I/O error occurs
     */
    public static void write(Collection<? extends QNodeTypeDefinition> defs,
                             NamespaceResolver r,
                             NamePathResolver npResolver,
                             Writer out)
            throws IOException {
        CompactNodeTypeDefWriter w = new CompactNodeTypeDefWriter(out, r, npResolver, true);
        for (QNodeTypeDefinition def : defs) {
            w.write(def);
        }
        w.close();
    }

    /**
     * Write one QNodeTypeDefinition to this writer
     *
     * @param ntd node type definition
     * @throws IOException if an I/O error occurs
     */
    public void write(QNodeTypeDefinition ntd) throws IOException {
        NodeTypeDefinition def = new NodeTypeDefinitionImpl(ntd, npResolver, new ValueFactoryQImpl(QValueFactoryImpl.getInstance(), npResolver));
        super.write(def);
    }

    /**
     * Write a collection of QNodeTypeDefinitions to this writer
     *
     * @param defs node type definitions
     * @throws IOException if an I/O error occurs
     */
    public void write(Collection<? extends QNodeTypeDefinition> defs) throws IOException {
        for (QNodeTypeDefinition def : defs) {
            write(def);
        }
    }

    private static NamespaceMapping createNsMapping(final NamespaceResolver namespaceResolver) {
        return new org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefWriter.NamespaceMapping() {
            public String getNamespaceURI(String prefix) {
                try {
                    return namespaceResolver.getURI(prefix);
                } catch (NamespaceException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
