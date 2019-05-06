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
package org.apache.jackrabbit.core.value;

import java.io.InputStream;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RefCountingBLOBFileValue</code> implements a reference counting BLOB
 * file value on top of an existing {@link BLOBFileValue}. Whenever a
 * {@link #copy()} is created from this BLOB file value, a new light weight
 * {@link RefCountBinary} is created and the reference count {@link #refCount}
 * is incremented. The underlying value is discarded once this
 * {@link RefCountingBLOBFileValue} and all its light weight
 * {@link RefCountBinary} instances have been discarded.
 */
public class RefCountingBLOBFileValue extends BLOBFileValue {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(RefCountingBLOBFileValue.class);

    /**
     * The actual value.
     */
    private final BLOBFileValue value;

    /**
     * The current ref count. Initially set to one.
     */
    private int refCount = 1;

    /**
     * Whether this instance has been discarded and cannot be used anymore.
     */
    private boolean disposed = false;

    /**
     * Creates a new reference counting blob file value based on the given
     * <code>value</code>.
     *
     * @param value the underlying value.
     */
    public RefCountingBLOBFileValue(BLOBFileValue value) {
        this.value = value;
    }

    //----------------------------< BLOBFileValue >-----------------------------

    /**
     * Discards the underlying value if the reference count drops to zero.
     */
    public synchronized void dispose() {
        if (refCount > 0) {
            if (--refCount == 0) {
                log.debug("{}@refCount={}, discarding value...",
                        System.identityHashCode(this), refCount);
                value.dispose();
                disposed = true;
            } else {
                log.debug("{}@refCount={}",
                        System.identityHashCode(this), refCount);
            }
        }
    }

    /**
     * Forwards the call to the underlying value.
     *
     * @param pruneEmptyParentDirs if <code>true</code>, empty parent
     *                             directories will automatically be deleted
     */
    @Override
    void delete(boolean pruneEmptyParentDirs) {
        value.delete(pruneEmptyParentDirs);
    }

    /**
     * Returns a light weight copy of this BLOB file value.
     *
     * @return a copy of this value.
     * @throws RepositoryException if an error occurs while creating the copy or
     *                             if this value has been disposed already.
     */
    @Override
    synchronized BLOBFileValue copy() throws RepositoryException {
        if (refCount <= 0) {
            throw new RepositoryException("this BLOBFileValue has been disposed");
        }
        BLOBFileValue bin = new RefCountBinary();
        refCount++;
        log.debug("{}@refCount={}", System.identityHashCode(this), refCount);
        return bin;
    }

    public boolean equals(Object obj) {
        if (obj instanceof RefCountingBLOBFileValue) {
            RefCountingBLOBFileValue val = (RefCountingBLOBFileValue) obj;
            return value.equals(val.value);
        }
        return false;
    }

    public String toString() {
        return value.toString();
    }

    public int hashCode() {
        return 0;
    }

    //-----------------------------------------------------< javax.jcr.Binary >

    public long getSize() throws RepositoryException {
        return value.getSize();
    }

    public InputStream getStream() throws RepositoryException {
        return value.getStream();
    }

    @Override
    protected void finalize() throws Throwable {
        if (!disposed) {
            dispose();
        }
        super.finalize();
    }

    //------------------------< RefCountBinary >--------------------------------

    private final class RefCountBinary extends BLOBFileValue {

        private boolean disposed;

        public InputStream getStream() throws RepositoryException {
            checkDisposed();
            return getInternalValue().getStream();
        }

        public long getSize() throws RepositoryException {
            checkDisposed();
            return getInternalValue().getSize();
        }

        public void dispose() {
            if (!disposed) {
                disposed = true;
                getInternalValue().dispose();
            }
        }

        @Override
        void delete(boolean pruneEmptyParentDirs) {
            getInternalValue().delete(pruneEmptyParentDirs);
        }

        @Override
        BLOBFileValue copy() throws RepositoryException {
            checkDisposed();
            return getInternalValue().copy();
        }

        public boolean equals(Object obj) {
            if (obj instanceof RefCountBinary) {
                RefCountBinary other = (RefCountBinary) obj;
                return getInternalValue().equals(other.getInternalValue());
            }
            return false;
        }

        public String toString() {
            return getInternalValue().toString();
        }

        public int hashCode() {
            return 0;
        }

        @Override
        protected void finalize() throws Throwable {
            dispose();
            super.finalize();
        }

        private BLOBFileValue getInternalValue() {
            return RefCountingBLOBFileValue.this;
        }

        private void checkDisposed() throws RepositoryException {
            if (disposed) {
                throw new RepositoryException("this BLOBFileValue is disposed");
            }
        }
    }
}
