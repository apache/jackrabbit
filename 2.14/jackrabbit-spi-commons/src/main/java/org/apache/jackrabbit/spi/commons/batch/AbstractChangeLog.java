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
package org.apache.jackrabbit.spi.commons.batch;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Batch;

/**
 * This base class for {@link ChangeLog} implementations maintains a list of operations
 * of type type <code>T</code>.
 * @param <T>
 */
public abstract class AbstractChangeLog<T extends Operation> implements ChangeLog {

    /**
     * {@link Operation}s kept in this change log.
     */
    protected final List<T> operations = new LinkedList<T>();

    /**
     * Added an operation to the list of {@link #operations}.
     * @param op  {@link Operation} to add
     * @throws RepositoryException
     */
    public void addOperation(T op) throws RepositoryException {
        operations.add(op);
    }

    /**
     * This implementation applies each of the operation maintained by
     * this change log to the passed <code>batch</code>.
     * {@inheritDoc}
     */
    public Batch apply(Batch batch) throws RepositoryException {
        if (batch == null) {
            throw new IllegalArgumentException("Batch must not be null");
        }
        for (Iterator<T> it = operations.iterator(); it.hasNext(); ) {
            Operation op = it.next();
            op.apply(batch);
        }
        return batch;
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        for (Iterator<T> it = operations.iterator(); it.hasNext(); ) {
            b.append(it.next());
            if (it.hasNext()) {
                b.append(", ");
            }
        }
        return b.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (null == other) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (other instanceof AbstractChangeLog<?>) {
            return equals((AbstractChangeLog<?>) other);
        }
        return false;
    }

    public boolean equals(AbstractChangeLog<?> other) {
        return operations.equals(other.operations);
    }

    @Override
    public int hashCode() {
        throw new IllegalArgumentException("Not hashable");
    }

}
