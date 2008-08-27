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
package org.apache.jackrabbit.api.jsr283.retention;

import javax.jcr.RepositoryException;

/**
 * An <code>RetentionPolicy</code> is an object with a name and an optional
 * description.
 *
 * @see RetentionManager#getRetentionPolicy(String)
 * @see RetentionManager#setRetentionPolicy(String, RetentionPolicy)
 * @see RetentionManager#removeRetentionPolicy(String) 
 * @since JCR 2.0
 */
public interface RetentionPolicy {
    /**
     * Returns the name of the retention policy. A JCR name.
     *
     * @return the name of the access control policy. A JCR name.
     * @throws RepositoryException if an error occurs.
     */
    public String getName() throws RepositoryException;

}
