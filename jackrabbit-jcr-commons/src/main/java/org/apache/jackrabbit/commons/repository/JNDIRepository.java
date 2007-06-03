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
package org.apache.jackrabbit.commons.repository;

import javax.naming.Context;

/**
 * Proxy for a repository bound in JNDI. The configured repository is
 * looked up from JNDI lazily during each method call. Thus the JNDI entry
 * does not need to exist when this class is instantiated. The JNDI entry
 * can also be replaced with another repository during the lifetime of an
 * instance of this class.
 *
 * @since 1.4
 */
public class JNDIRepository extends ProxyRepository {

    /**
     * Creates a proxy for a repository in the given JNDI location.
     *
     * @param context JNDI context
     * @param name JNDI name of the proxied repository
     */
    public JNDIRepository(Context context, String name) {
        super(new JNDIRepositoryFactory(context, name));
    }

}
