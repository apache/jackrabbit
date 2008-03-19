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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry;

/**
 * This is one Entry in an {@link PolicyTemplate} or an
 * {@link org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy}<p>
 * In the previous case the entry must be detached from the effective ac-content.
 */
public interface PolicyEntry extends AccessControlEntry {

    /**
     * @return true if this entry adds <code>Privilege</code>s for the principal;
     * false otherwise.
     */
    boolean isAllow();

    // TODO: eventually add
    // String getNodePath();
    // String getGlob();
    // String[] getRestrictionNames();
    // Value getRestriction(String restrictionName);
}
