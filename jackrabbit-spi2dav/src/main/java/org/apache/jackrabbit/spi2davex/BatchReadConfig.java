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
package org.apache.jackrabbit.spi2davex;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;

/**
 * <code>BatchReadConfig</code> defines if and how deep child item
 * information should be retrieved, when accessing a <code>Node</code>.
 * The configuration is based on path.
 */
public interface BatchReadConfig {

    /**
     * Return the depth for the given path.
     *
     * @param path
     * @param resolver
     * @return -1 if all child infos should be return or any value greater
     * than -1 if only parts of the subtree should be returned. If there is no
     * matching configuration entry some implementation specific default depth
     * will be returned.
     */
    public int getDepth(Path path, PathResolver resolver) throws NamespaceException;

}