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
package org.apache.jackrabbit.spi;

import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Factory for creating {@link RepositoryService} instances. Implementations must
 * provide a no argument constructor.
 */
public interface RepositoryServiceFactory {

    /**
     * Create a new {@link RepositoryService}. If the factory does not understand the
     * passed <code>parameters</code> it <em>must</em> return <code>null</code>.
     * @param parameters  implementation specific set of parameters
     * @return  a fresh <code>RepositoryService</code> instance or <code>null</code>.
     * @throws RepositoryException  If there was an error creating the
     *     <code>RepositoryService</code> instance
     */
    public RepositoryService createRepositoryService(Map<?, ?> parameters) throws RepositoryException;
}
