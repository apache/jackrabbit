/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.lite;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.jackrabbit.base.BaseRepository;

/**
 * Lightweight implementation of the JCR Repository interface.
 * <p>
 * The repository descriptors are managed as a private property
 * collection that subclasses can modify using the protected
 * {@link #setDescriptor(String, String) setDescriptor} method.
 * <p>
 * A subclass needs to also implement the login method to make
 * this class useful.
 */
public class LiteRepository extends BaseRepository {

    /** Repository descriptors. */
    private final Properties descriptors;

    /**
     * Initializes the repository by adding the standard repository
     * descriptors. The constructor is protected to signify that
     * this class needs to be subclassed to be of any real use.
     */
    protected LiteRepository() {
        descriptors = new Properties();

        // JCR specification information
        setDescriptor(SPEC_NAME_DESC,
                "Content Repository API for Java(TM) Technology Specification");
        setDescriptor(SPEC_VERSION_DESC, "0.16.2");

        // addDescriptor(LEVEL_1_SUPPORTED, "true"); // TODO required!
        // addDescriptor(LEVEL_2_SUPPORTED, "true");
        // addDescriptor(OPTION_LOCKING_SUPPORTED, "true");
        // addDescriptor(OPTION_OBSERVATION_SUPPORTED, "true");
        // addDescriptor(OPTION_QUERY_SQL_SUPPORTED, "true");
        // addDescriptor(OPTION_TRANSACTIONS_SUPPORTED, "true");
        // addDescriptor(OPTION_VERSIONING_SUPPORTED, "true");
        // addDescriptor(QUERY_JCRPATH, "true");
        // addDescriptor(QUERY_JCRSCORE, "true");
        // addDescriptor(QUERY_XPATH_DOC_ORDER, "true");
        // addDescriptor(QUERY_XPATH_POS_INDEX, "true");
    }

    /**
     * Adds a repository descriptor.
     *
     * @param key descriptor key
     * @param value descriptor value
     */
    protected void setDescriptor(String key, String value) {
        descriptors.setProperty(key, value);
    }

    /**
     * Returns the value of the identified repository descriptor.
     *
     * @param key descriptor key
     * @return descriptor value
     */
    public String getDescriptor(String key) {
        return descriptors.getProperty(key);
    }

    /**
     * Returns the repository descriptor keys.
     *
     * @return descriptor keys
     */
    public String[] getDescriptorKeys() {
        Enumeration keys = descriptors.propertyNames();
        return (String[]) Collections.list(keys).toArray(new String[0]);
    }

}
