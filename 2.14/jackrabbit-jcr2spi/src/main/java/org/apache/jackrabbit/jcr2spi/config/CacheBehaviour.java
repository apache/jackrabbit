/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.jcr2spi.config;

/**
 * <code>CacheBehaviour</code> defines constants for the various cache
 * maintenance strategies. The effective strategy depends on two factors,
 * whether the repository implementation supports observation and the behaviour
 * provided in the {@link RepositoryConfig}.
 */
public final class CacheBehaviour {

    /**
     * Cache maintenance is done by invalidating affected items of an operation
     * and forcing the jcr2spi implementation to reload the item states when
     * they are accessed next time. No event listener is used for cache
     * maintenance even though the repository implementation might support
     * observation.
     */
    public static final CacheBehaviour INVALIDATE = new CacheBehaviour();

    /**
     * Cache maintenance is done using events from the repository. After an
     * operation has been executed on the RepositoryService events are retrieved
     * from the repository and the cache is updated based on the returned
     * events. This strategy requires that the repository implementation
     * supports observation.
     */
    public static final CacheBehaviour OBSERVATION = new CacheBehaviour();

    private CacheBehaviour() {
    }
}
