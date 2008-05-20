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
package org.apache.jackrabbit.api.jsr283.security;

import javax.jcr.RangeIterator;

/**
 * Allows easy iteration through a list of <code>AccessControlPolicy</code>s
 * with <code>nextAccessControlPolicy</code> as well as a <code>skip</code>
 * method inherited from <code>RangeIterator</code>.
 *
 * @since JCR 2.0
 */
public interface AccessControlPolicyIterator extends RangeIterator {
    
    /**
     * Returns the next <code>AccessControlPolicy</code> in the iteration.
     *
     * @return the next <code>AccessControlPolicy</code> in the iteration.
     * @throws java.util.NoSuchElementException if iteration has no more
     *         <code>AccessControlPolicy</code>s.
    */
   public AccessControlPolicy nextAccessControlPolicy();

}
