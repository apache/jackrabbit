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
package org.apache.jackrabbit.spi.commons.identifier;

import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

/**
 * <code>IdFactoryImpl</code>...
 */
public final class IdFactoryImpl extends AbstractIdFactory {

    private static IdFactory INSTANCE;

    private IdFactoryImpl() {
    }

    public static IdFactory getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new IdFactoryImpl();
        }
        return IdFactoryImpl.INSTANCE;
    }

    /**
     * @see org.apache.jackrabbit.spi.commons.identifier.AbstractIdFactory#getPathFactory() 
     */
    @Override
    protected PathFactory getPathFactory() {
        return PathFactoryImpl.getInstance();
    }
}
