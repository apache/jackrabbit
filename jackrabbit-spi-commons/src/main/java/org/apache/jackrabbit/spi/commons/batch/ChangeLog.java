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
package org.apache.jackrabbit.spi.commons.batch;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Batch;


/**
 * A <code>ChangeLog</code> is a specialized {@link Batch} which
 * keeps a list of {@link Operation}s. The {@link #apply(Batch)} method
 * applies these operations to another batch.
 */
public interface ChangeLog extends Batch {

    /**
     * Applies the {@link Operation}s contained in this change log to
     * the passed <code>batch</code>.
     * @param batch
     * @return  The <code>batch</code> passed in as argument with the
     *   operations from this change log applied.
     * @throws RepositoryException
     */
    public Batch apply(Batch batch) throws RepositoryException;
}
