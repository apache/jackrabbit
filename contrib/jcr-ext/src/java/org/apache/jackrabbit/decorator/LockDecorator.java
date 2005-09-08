/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.decorator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

public class LockDecorator implements Lock {

    private final Node node;

    private final Lock lock;

    public LockDecorator(Node node, Lock lock) {
        this.node = node;
        this.lock = lock;
    }

    public Node getNode() {
        return node;
    }

    public String getLockOwner() {
        return lock.getLockOwner();
    }

    public boolean isDeep() {
        return lock.isDeep();
    }

    public String getLockToken() {
        return lock.getLockToken();
    }

    public boolean isLive() throws RepositoryException {
        return lock.isLive();
    }

    public boolean isSessionScoped() {
        return lock.isSessionScoped();
    }

    public void refresh() throws LockException, RepositoryException {
        lock.refresh();
    }

}
