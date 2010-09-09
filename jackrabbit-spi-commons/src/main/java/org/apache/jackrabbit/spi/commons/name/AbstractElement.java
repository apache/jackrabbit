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
package org.apache.jackrabbit.spi.commons.name;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Path.Element;

/**
 * Abstract base class for path elements.
 */
abstract class AbstractElement implements Element {

    /** Serial version UID */
    private static final long serialVersionUID = 5259174485452841973L;

    /**
     * Returns <code>false</code>, except when overridden by the
     * {@link RootElement} subclass.
     *
     * @return <code>false</code>
     */
    public boolean denotesRoot() {
        return false;
    }

    /**
     * Returns <code>false</code>, except when overridden by the
     * {@link ParentElement} subclass.
     *
     * @return <code>false</code>
     */
    public boolean denotesParent() {
        return false;
    }

    /**
     * Returns <code>false</code>, except when overridden by the
     * {@link CurrentElement} subclass.
     *
     * @return <code>false</code>
     */
    public boolean denotesCurrent() {
        return false;
    }

    /**
     * Returns <code>false</code>, except when overridden by the
     * {@link NameElement} subclass.
     *
     * @return <code>false</code>
     */
    public boolean denotesName() {
        return false;
    }

    /**
     * Returns <code>false</code>, except when overridden by the
     * {@link IdentifierElement} subclass.
     *
     * @return <code>false</code>
     */
    public boolean denotesIdentifier() {
        return false;
    }

    /**
     * Returns {@link Path#INDEX_UNDEFINED}, except when overridden by the
     * {@link NameElement} subclass.
     *
     * @return {@link Path#INDEX_UNDEFINED}
     */
    public int getIndex() {
        return Path.INDEX_UNDEFINED;
    }

    /**
     * Returns {@link Path#INDEX_DEFAULT}, except when overridden by the
     * {@link NameElement} subclass.
     *
     * @return {@link Path#INDEX_DEFAULT}
     */
    public int getNormalizedIndex() {
        return Path.INDEX_DEFAULT;
    }

    //--------------------------------------------------------------< Object >

    public final String toString() {
        return getString();
    }

}
