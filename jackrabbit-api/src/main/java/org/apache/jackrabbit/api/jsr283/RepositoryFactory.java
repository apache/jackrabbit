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
package org.apache.jackrabbit.api.jsr283;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Repository;

/**
 * <code>RepositoryFactory</code> is a factory class for <code>Repository</code>
 * implementations.
 * <p/>
 * A concrete implementation of this class must have a zero-argument public
 * constructor. Repository factories may be installed in an instance of the Java
 * platform as extensions, that is, jar files placed into any of the usual
 * extension directories. Factories may also be made available by adding them to
 * the applet or application class path or by some other platform-specific
 * means.
 * <p/>
 * A repository factory implementation should support the Java Standard Edition
 * Service Provider mechanism, that is, an implementation should include the
 * file <code>META-INF/services/javax.jcr.RepositoryFactory</code>. This file
 * contains the fully qualified name of the class that implements
 * <code>RepositoryFactory</code>.
 * <p/>
 * <b>Examples how to obtain repository instances</b>
 * <p/>
 * Explicitly specifying the repository factory implementation:
 * <code>
 * <pre>
 *   Map parameters = new HashMap();
 *   parameters.put("com.vendor.address", "vendor://localhost:9999/repo");
 *   RepositoryFactory factory = (RepositoryFactory) Class.forName("com.vendor.RepositoryFactoryImpl");
 *   Repository repo = factory.getRepository(parameters);
 * </pre>
 * </code>
 * <p/>
 * Using ServiceLoader from Java SE 6:
 * <code>
 * <pre>
 *   Map parameters = new HashMap();
 *   parameters.put("com.vendor.address", "vendor://localhost:9999/repo");
 *   Repository repo = null;
 *   for (RepositoryFactory factory : ServiceLoader.load(RepositoryFactory.class)) {
 *     repo = factory.getRepository(parameters);
 *     if (repo != null) {
 *       // factory accepted parameters
 *       break;
 *     }
 *   }
 * </pre>
 * <b>Note:</b> on Java SE prior to version 6, one may use the
 * {@link javax.imageio.spi.ServiceRegistry ServiceRegistry} class to look up
 * the available <code>RepositoryFactory</code> implementations.
 *
 * @since JCR 2.0
 */
public interface RepositoryFactory {

    /**
     * Attempts to establish a connection to a repository using the given
     * <code>parameters</code>.
     * <p>
     * Parameters are passed in a <code>Map</code> of <code>String</code>
     * key/value pairs. The keys are not specified by JCR and are implementation
     * specific. However, vendors should use keys that are namespace qualified
     * in the Java package style to distinguish their key names. For example
     * an address parameter might be <code>com.vendor.address</code>.
     * </p>
     * <p>
     * The implementation must return <code>null</code> if it does not
     * understand the given parameters. The implementation may also return
     * <code>null</code> if a default repository instance is requested
     * (indicated by <code>null</code> <code>parameters</code>) and this factory
     * is not able to identify a default repository. An implementation should
     * throw an <code>RepositoryException</code> if it is the right factory but
     * has trouble connecting to the repository.
     * </p>
     * <p>
     * An implementation of this method must be thread-safe.
     * </p>
     *
     * @param parameters map of arbitrary string key/value pairs as repository
     *                   arguments or <code>null</code> if none are provided and
     *                   a client wishes to connect to a default repository.
     * @return a repository instance or <code>null</code> if this implementation
     *         does not understand the passed <code>parameters</code>.
     * @throws RepositoryException if getRepository fails or if no suitable
     *                             repository is found.
     */
    public Repository getRepository(Map parameters) throws RepositoryException;
}