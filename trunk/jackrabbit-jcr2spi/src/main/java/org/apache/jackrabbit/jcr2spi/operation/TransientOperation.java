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
package org.apache.jackrabbit.jcr2spi.operation;

import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;

/**
 * <code>TransientOperation</code>...
 */
public abstract class TransientOperation extends AbstractOperation {

    static final int NO_OPTIONS = ItemStateValidator.CHECK_NONE;
    static final int DEFAULT_OPTIONS =
            ItemStateValidator.CHECK_LOCK |
            ItemStateValidator.CHECK_COLLISION |
            ItemStateValidator.CHECK_VERSIONING |
            ItemStateValidator.CHECK_CONSTRAINTS;

    private final int options;

    TransientOperation(int options) {
        this.options = options;
    }

    /**
     * Return the set of options that should be used to validate the transient
     * modification. Valid options are a combination of any of the following
     * options:
     *
     * <ul>
     * <li>{@link org.apache.jackrabbit.jcr2spi.state.ItemStateValidator#CHECK_NONE CHECK_NONE} if no validation check is required,</li>
     * <li>{@link org.apache.jackrabbit.jcr2spi.state.ItemStateValidator#CHECK_ACCESS CHECK_ACCESS},</li>
     * <li>{@link org.apache.jackrabbit.jcr2spi.state.ItemStateValidator#CHECK_COLLISION CHECK_COLLISION},</li>
     * <li>{@link org.apache.jackrabbit.jcr2spi.state.ItemStateValidator#CHECK_CONSTRAINTS CHECK_CONSTRAINTS},</li>
     * <li>{@link org.apache.jackrabbit.jcr2spi.state.ItemStateValidator#CHECK_LOCK CHECK_LOCK},</li>
     * <li>{@link org.apache.jackrabbit.jcr2spi.state.ItemStateValidator#CHECK_VERSIONING CHECK_VERSIONING},</li>
     * <li>{@link org.apache.jackrabbit.jcr2spi.state.ItemStateValidator#CHECK_ALL CHECK_ALL} as shortcut for all options.</li>
     * </ul>
     *
     * @return The set of options used to validate the transient modification.
     * @see org.apache.jackrabbit.jcr2spi.state.ItemStateValidator
     */
    public int getOptions() {
        return options;
    }
}
