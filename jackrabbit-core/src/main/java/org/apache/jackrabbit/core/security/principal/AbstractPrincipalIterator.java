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
package org.apache.jackrabbit.core.security.principal;

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.NoSuchElementException;

/**
 * Lazy implementation of the <code>PrincipalIterator</code> that allows to
 * avoid retrieving all elements beforehand.
 * NOTE: Subclasses must call {#link seekNext()}
 * upon object construction and assign the value to the 'next' field.
 */
abstract class AbstractPrincipalIterator implements PrincipalIterator {

    private static Logger log = LoggerFactory.getLogger(AbstractPrincipalIterator.class);

    long size;
    long position;
    Principal next;

    AbstractPrincipalIterator() {
        size = -1;
        position = 0;
    }

    /**
     * Subclasses must call {#link seekNext()} upon object construction and
     * assign the value to the 'next' field.
     *
     * @return The principal to be return upon the subsequent {@link #next()}
     * or {@link #nextPrincipal()} call or <code>null</code> if no next principal
     * exists.
     */
    abstract Principal seekNext();

    //--------------------------------------------------< PrincipalIterator >---
    public Principal nextPrincipal() {
        Principal p = next;
        if (p == null) {
            throw new NoSuchElementException();
        }
        next = seekNext();
        position++;
        return p;
    }

    //------------------------------------------------------< RangeIterator >---
    public void skip(long skipNum) {
        while (skipNum-- > 0) {
            next();
        }
    }

    public long getSize() {
        return size;
    }

    public long getPosition() {
        return position;
    }

    //-----------------------------------------------------------< Iterator >---
    public boolean hasNext() {
        return next != null;
    }

    public Object next() {
        return nextPrincipal();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}