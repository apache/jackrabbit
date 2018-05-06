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
package org.apache.jackrabbit.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.BitSet;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

/**
 * <code>WeakIdentityCollection</code> implements a Collection with weak values.
 * Equality of elements is tested using the == operator.
 * <p>
 * This collection does not hide the fact that the garbage collector will remove
 * a mapping at some point in time. Thus, the {@link java.util.Iterator} returned
 * by this collection might return <code>null</code> values. The same applies
 * to the method {@link #toArray()} in both its variants.
 */
public class WeakIdentityCollection implements Collection {

    /**
     * The weak references.
     */
    private transient WeakRef[] elementData;

    /**
     * The current number of elements in {@link #elementData}.
     */
    private int size;

    /**
     * The reference queue to poll for references that point to unreachable
     * objects.
     */
    private final ReferenceQueue refQueue = new ReferenceQueue();

    /**
     * BitSet where a set bit indicates that the slot at this position in
     * {@link #elementData} is empty and can be reused.
     */
    private final BitSet emptySlots = new BitSet();

    /**
     * Creates a new WeakIdentityCollection.
     *
     * @param initialCapacity the initial capacity.
     */
    public WeakIdentityCollection(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " +
                    initialCapacity);
        }
        this.elementData = new WeakRef[initialCapacity];
    }

    /**
     * Returns the current size of this collection.
     *
     * @return the current size of this collection.
     */
    public int size() {
        return size;
    }

    /**
     * Returns <code>true</code> if this collection is empty.
     *
     * @return <code>true</code> if this collection is empty.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Releases all references held by this collection.
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            elementData[i] = null;
        }
        size = 0;
        emptySlots.clear();
    }

    /**
     * Adds object <code>o</code> to this collection.
     *
     * @param o the object to add.
     * @return always <code>true</code> as this collection allows duplicates.
     * @throws NullPointerException if <code>o</code> is <code>null</code>.
     */
    public boolean add(Object o) {
        if (o == null) {
            throw new NullPointerException("Object must not be null");
        }
        // poll refQueue for a slot we can reuse
        WeakRef ref = (WeakRef) refQueue.poll();
        if (ref != null) {
            elementData[ref.index] = new WeakRef(o, ref.index);
            cleanQueue();
        } else if (!emptySlots.isEmpty()) {
            int idx = emptySlots.nextSetBit(0);
            elementData[idx] = new WeakRef(o, idx);
            emptySlots.clear(idx);
        } else {
            ensureCapacity(size + 1);
            elementData[size++] = new WeakRef(o, size - 1);
        }
        return true;
    }

    /**
     * Returns <code>true</code> if this collection contains <code>o</code>.
     *
     * @param o element whose presence in this collection is to be tested.
     * @return <code>true</code> if this collection contains the specified
     *         element
     */
    public boolean contains(Object o) {
        for (int i = 0; i < size; i++) {
            if (elementData[i].get() == o) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the object <code>o</code> from this collection if it is present.
     *
     * @param o the object to remove.
     * @return <code>true</code> if this collection changed as a result of the
     *         call.
     */
    public boolean remove(Object o) {
        for (int i = 0; i < size; i++) {
            if (elementData[i].get() == o) {
                emptySlots.set(i);
                // overwrite entry with dummy ref
                elementData[i] = new WeakRef(null, i);
                return true;
            }
        }
        return false;
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException("addAll");
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public boolean containsAll(Collection c) {
        throw new UnsupportedOperationException("containsAll");
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException("removeAll");
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException("retainAll");
    }

    /**
     * Returns an {@link java.util.Iterator} over the elements of this
     * collection. The returned iterator is not fail-fast. That is, it does
     * not throw a {@link java.util.ConcurrentModificationException} if this
     * collection is modified while iterating over the collection.
     *
     * @return an {@link java.util.Iterator} over the elements of this
     *         collection.
     */
    public Iterator iterator() {
        return new Iter();
    }

    /**
     * Returns an array containing all of the elements in this collection. The
     * returned array may contain <code>null</code> elements!
     *
     * @return an array containing all of the elements in this collection.
     */
    public Object[] toArray() {
        Object[] result = new Object[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = elementData[i].get();
        }
        return result;
    }

    /**
     * The returned array may contain <code>null</code> elements!
     * {@inheritDoc}
     */
    public Object[] toArray(Object a[]) {
        if (a.length < size) {
            a = (Object[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i].get();
        }

        if (a.length > size) {
            a[size] = null;
        }

        return a;
    }

    /**
     * Ensures that the internal {@link #elementData} has
     * <code>minCapacity</code>.
     *
     * @param minCapacity the minimal capacity to ensure.
     */
    private void ensureCapacity(int minCapacity) {
        int oldCapacity = elementData.length;
        if (minCapacity > oldCapacity) {
            Object oldData[] = elementData;
            int newCapacity = (oldCapacity * 3)/2 + 1;
                if (newCapacity < minCapacity)
            newCapacity = minCapacity;
            elementData = new WeakRef[newCapacity];
            System.arraycopy(oldData, 0, elementData, 0, size);
        }
    }

    /**
     * Polls the reference queue until no reference is available anymore.
     */
    private void cleanQueue() {
        WeakRef ref;
        while ((ref = (WeakRef) refQueue.poll()) != null) {
            emptySlots.set(ref.index);
        }
    }

    /**
     * Iterator implementation for this collecation.
     */
    private final class Iter implements Iterator {

        /**
         * current index.
         */
        private int index;

        /**
         * The current element data.
         */
        private Reference[] elements = elementData;

        /**
         * The current size of this collection.
         */
        private int size = WeakIdentityCollection.this.size;

        /**
         * @throws UnsupportedOperationException always.
         */
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        /**
         * @inheritDoc
         */
        public boolean hasNext() {
            return index < size;
        }

        /**
         * @inheritDoc
         */
        public Object next() {
            if (index >= size) {
                throw new NoSuchElementException();
            }
            return elements[index++].get();
        }
    }

    /**
     * Weak reference with index value that points to the slot in {@link
     * WeakIdentityCollection#elementData}.
     */
    private final class WeakRef extends WeakReference {

        /**
         * The index of this weak reference.
         */
        private final int index;

        /**
         * Creates a new WeakRef.
         *
         * @param referent object the new weak reference will refer to.
         * @param index    the index of this weak reference.
         */
        public WeakRef(Object referent, int index) {
            super(referent, refQueue);
            this.index = index;
        }
    }
}
