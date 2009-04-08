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
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;

import javax.jcr.RepositoryException;

/**
 * <code>AbstractRepositoryService</code> provides an abstract base class for
 * repository service implementations. This class provides default
 * implementations for the following methods:
 * <ul>
 * <li>{@link #getIdFactory()}</li>
 * <li>{@link #getNameFactory()}</li>
 * <li>{@link #getPathFactory()}</li>
 * <li>{@link #getQValueFactory()}</li>
 * </ul>
 */
public abstract class AbstractRepositoryService implements RepositoryService {

    /**
     * @return {@link IdFactoryImpl#getInstance()}.
     * @throws RepositoryException if an error occurs.
     */
    public IdFactory getIdFactory() throws RepositoryException {
        return IdFactoryImpl.getInstance();
    }

    /**
     * @return {@link NameFactoryImpl#getInstance()}.
     * @throws RepositoryException if an error occurs.
     */
    public NameFactory getNameFactory() throws RepositoryException {
        return NameFactoryImpl.getInstance();
    }

    /**
     * @return {@link PathFactoryImpl#getInstance()}.
     * @throws RepositoryException if an error occurs.
     */
    public PathFactory getPathFactory() throws RepositoryException {
        return PathFactoryImpl.getInstance();
    }

    /**
     * @return {@link QValueFactoryImpl#getInstance()}.
     * @throws RepositoryException if an error occurs.
     */
    public QValueFactory getQValueFactory() throws RepositoryException {
        return QValueFactoryImpl.getInstance();
    }
}
