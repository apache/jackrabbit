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

import org.apache.jackrabbit.spi.Name;

/**
 * Singleton class for the parent path element, i.e. "..".
 */
final class ParentElement extends AbstractElement {

    /** Singleton instance */
    public static final ParentElement INSTANCE = new ParentElement();

    /** Serial version UID */
    private static final long serialVersionUID = -7445693011766172219L;

    /** Name of the parent element */
    private static final Name NAME =
        NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, "..");

    /** Hidden constructor */
    private ParentElement() {
    }

    /**
     * Returns <code>true</code>, as this is the parent path element.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean denotesParent() {
        return true;
    }

    /**
     * Returns the pseudo-name ".." of the parent path element.
     *
     * @return ".."
     */
    public Name getName() {
        return NAME;
    }

    /**
     * Returns the ".." string representation of the parent path element.
     *
     * @return ".."
     */
    public String getString() {
        return "..";
    }

    //--------------------------------------------------------< Serializable >

    /** Returns the singleton instance of this class */
    public Object readResolve() {
        return INSTANCE;
    }

}
