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
package org.apache.jackrabbit.spi2dav;

import static org.junit.Assert.assertThrows;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.ItemInfoCacheImpl;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.junit.Test;

public class RepositoryServiceImplTest {

    @Test
    public void testWithSystemPropertiesAndIncompatibleConnectionOptions() throws RepositoryException {
        ConnectionOptions connectionOptions = ConnectionOptions.builder().useSystemProperties(true).allowSelfSignedCertificates(true).build();
        assertThrows(RepositoryException.class, ()->getRepositoryService("https://jackrabbit.apache.org/jcr", connectionOptions));
        ConnectionOptions connectionOptions2 = ConnectionOptions.builder().useSystemProperties(true).disableHostnameVerification(true).build();
        assertThrows(RepositoryException.class, ()->getRepositoryService("https://jackrabbit.apache.org/jcr", connectionOptions2));
    }

    static RepositoryServiceImpl getRepositoryService(String uri, ConnectionOptions connectionOptions) throws RepositoryException {
        IdFactory idFactory = IdFactoryImpl.getInstance();
        NameFactory nFactory = NameFactoryImpl.getInstance();
        PathFactory pFactory = PathFactoryImpl.getInstance();
        QValueFactory vFactory = QValueFactoryImpl.getInstance();
        return new RepositoryServiceImpl(uri, idFactory, nFactory, pFactory, vFactory, ItemInfoCacheImpl.DEFAULT_CACHE_SIZE, connectionOptions);
    }
}
