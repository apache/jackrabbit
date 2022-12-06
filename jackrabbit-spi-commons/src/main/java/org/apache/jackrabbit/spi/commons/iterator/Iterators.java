/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.jackrabbit.spi.commons.iterator;

import org.apache.commons.collections4.iterators.ArrayIterator;
import org.apache.commons.collections4.iterators.EmptyIterator;
import org.apache.commons.collections4.iterators.FilterIterator;
import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.commons.collections4.iterators.SingletonIterator;
import org.apache.commons.collections4.iterators.TransformIterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;

import java.util.Collection;
import java.util.Iterator;

/**
 * Historical utility class containing type safe adapters for some of the iterators of
 * commons-collections.
 */
public final class Iterators {

    private Iterators() {
        super();
    }

    /**
     * Returns an iterator containing the single element <code>element</code> of
     * type <code>T</code>.
     *
     * @param <T>
     * @param element
     * @return
     */
    public static <T> Iterator<T> singleton(T element) {
        return new SingletonIterator<T>(element);
    }

    /**
     * Returns an empty iterator of type <code>T</code>.
     *
     * @param <T>
     * @return
     */
    public static <T> Iterator<T> empty() {
        return EmptyIterator.emptyIterator();
    }

    /**
     * Returns an iterator for the concatenation of <code>iterator1</code> and
     * <code>iterator2</code>.
     *
     * @param <T>
     * @param iterator1
     * @param iterator2
     * @return
     */
    public static <T> Iterator<T> iteratorChain(Iterator<? extends T> iterator1, Iterator<? extends T> iterator2) {
        return new IteratorChain<T>(iterator1, iterator2);
    }

    /**
     * Returns an iterator for the concatenation of all the given <code>iterators</code>.
     *
     * @param <T>
     * @param iterators
     * @return
     */
    public static <T> Iterator<T> iteratorChain(Iterator<? extends T>[] iterators) {
        return new IteratorChain<T>(iterators);
    }

    /**
     * Returns an iterator for the concatenation of all the given <code>iterators</code>.
     *
     * @param <T>
     * @param iterators
     * @return
     */
    public static <T> Iterator<T> iteratorChain(Collection<Iterator<? extends T>> iterators) {
        return new IteratorChain<T>(iterators);
    }

    /**
     * Returns an iterator for elements of an array of <code>values</code>.
     *
     * @param <T>
     * @param values  the array to iterate over.
     * @param from  the index to start iterating at.
     * @param to  the index to finish iterating at.
     * @return
     */
    public static <T> Iterator<T> arrayIterator(T[] values, int from, int to) {
        return new ArrayIterator<T>(values, from, to);
    }

    /**
     * Returns an iterator with elements from an original <code>iterator</code> where the
     * given <code>predicate</code> matches removed.
     *
     * @param <T>
     * @param iterator
     * @param predicate
     * @return
     * @deprecated use {@link #filterIterator(Iterator, java.util.function.Predicate)} instead
     */
    public static <T> Iterator<T> filterIterator(Iterator<? extends T> iterator,
            final Predicate<? super T> predicate) {

        return new FilterIterator<T>(iterator, new org.apache.commons.collections4.Predicate<T>() {
            public boolean evaluate(T object) {
                return predicate.evaluate(object);
            }
        });
    }

    /**
     * Returns an iterator with elements from an original <code>iterator</code> where the
     * given <code>predicate</code> matches removed.
     *
     * @param <T>
     * @param iterator
     * @param predicate
     * @return
     */
    public static <T> Iterator<T> filterIterator(Iterator<? extends T> iterator,
            final java.util.function.Predicate<? super T> predicate) {

        return new FilterIterator<T>(iterator, new org.apache.commons.collections4.Predicate<T>() {
            public boolean evaluate(T object) {
                return predicate.test(object);
            }
        });
    }

    /**
     * Returns an iterator with elements of an original  <code>iterator</code> transformed by
     * a <code>transformer</code>.
     *
     * @param <R>
     * @param <S>
     * @param iterator
     * @param transformer
     * @return
     */
    public static <S, R> Iterator<R> transformIterator(Iterator<S> iterator, final Transformer<S, R> transformer) {

        org.apache.commons.collections4.Transformer<S, R> tf = new org.apache.commons.collections4.Transformer<S, R>() {
            public R transform(S input) {
                return transformer.transform(input);
            }
        };
        return new TransformIterator<S, R>(iterator, tf);
    }

    /**
     * Returns an iterator of {@link Property} from a {@link PropertyIterator}.
     *
     * @param propertyIterator
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Iterator<Property> properties(PropertyIterator propertyIterator) {
        return propertyIterator;
    }

    /**
     * Returns an iterator of {@link Node} from a {@link NodeIterator}.
     * @param nodeIterator
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Iterator<Node> nodes(NodeIterator nodeIterator) {
        return nodeIterator;
    }

}
