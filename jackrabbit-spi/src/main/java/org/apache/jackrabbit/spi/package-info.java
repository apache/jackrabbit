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

/**
 * Defines the interfaces of the JCR SPI (Service Provider Interface).
 *
 * <p>
 * The SPI cuts the JCR stack into two parts:
 * <ul>
 * <li>Above the SPI an implementation that wishes to expose the JCR API again
 * needs to implement the transient item space, the session local namespace
 * mapping and various conversions from the value representation in the SPI to
 * the resolved values in the JCR API.</li>
 * <li>An implementation of the SPI interfaces has to deal with the persistent
 * view of a JCR repository. This includes almost all aspects of the JSR 170
 * specification, except the previously stated transient space and the session
 * local namespace resolution to prefixes.</li>
 * </ul>
 *
 * <h2>Observation</h2>
 * Because one of the goals of this SPI is to make it easier to implement a
 * remoting layer using various existing protocols, the observation mechanism
 * has been design with this goal in mind. Instead of a listener registration
 * with a callback for each event bundle, the SPI uses a polling mechanism
 * with a timeout: {@link org.apache.jackrabbit.spi.RepositoryService#getEvents
 * RepositoryService.getEvents()}. With every call to this method the
 * repository is advised to return the events that occurred since the last
 * call. As a reference to the last retrieved
 * {@link org.apache.jackrabbit.spi.EventBundle} the
 * {@link org.apache.jackrabbit.spi.SessionInfo} contains a bundle identifier
 * which is automatically updated on each call to
 * <code>RepositoryService.getEvents()</code>. While this design allows for
 * a polling implementation on top of the SPI it is also well suited for a
 * listener based observation implementation on top of the SPI. With only
 * little thread synchronization overhead events can be acquired using a
 * <code>timeout</code> of {@link java.lang.Long#MAX_VALUE}.
 * <p>
 * If an SPI implementation does not support observation, the method
 * <code>RepositoryService.getEvents()</code> will always throw an
 * {@link javax.jcr.UnsupportedRepositoryOperationException}.
 */
@org.osgi.annotation.versioning.Version("3.0.0")
package org.apache.jackrabbit.spi;
