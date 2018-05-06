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
package org.apache.jackrabbit.core.query.lucene.constraint;

import java.util.Arrays;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;

/**
 * <code>SelectorBasedConstraint</code> implements a constraint that is based
 * on a named selector.
 */
public abstract class SelectorBasedConstraint implements Constraint {

    /**
     * The selector this constrained is based on.
     */
    private final SelectorImpl selector;

    /**
     * Cached selector index. Initially set to <code>-1</code>.
     */
    private int selectorIndex = -1;

    /**
     * Creates a new constraint based on the given <code>selector</code>.
     *
     * @param selector the selector this constraint is based on.
     */
    public SelectorBasedConstraint(SelectorImpl selector) {
        this.selector = selector;
    }

    /**
     * Returns the selector index of this constraint.
     *
     * @param names the selector names.
     * @return the selector index.
     */
    protected int getSelectorIndex(Name[] names) {
        if (selectorIndex == -1) {
            selectorIndex = Arrays.asList(names).indexOf(
                    selector.getSelectorQName());
        }
        return selectorIndex;
    }

    /**
     * @return the selector of this constraint.
     */
    protected SelectorImpl getSelector() {
        return selector;
    }
}
