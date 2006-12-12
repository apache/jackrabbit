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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.UnknownPrefixException;

/**
 * The class <code>NamespaceMappings</code> holds a namespace mapping that is
 * used internally in the search index. Storing paths with the full uri of a
 * namespace would require too much space in the search index.
 */
public interface NamespaceMappings extends NamespaceResolver {

    /**
     * Translates a property name from a session local namespace mapping into a
     * search index private namespace mapping.
     *
     * @param name     the property name to translate
     * @param resolver the <code>NamespaceResolver</code> of the local session.
     * @return the translated property name
     */
    public String translatePropertyName(String name,
                                        NamespaceResolver resolver)
            throws IllegalNameException, UnknownPrefixException;
}
