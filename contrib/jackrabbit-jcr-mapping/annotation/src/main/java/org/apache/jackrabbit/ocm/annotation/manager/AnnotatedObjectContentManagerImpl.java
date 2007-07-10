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
package org.apache.jackrabbit.ocm.annotation.manager;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.ocm.annotation.mapper.AnnotatedObjectMapper;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.query.impl.QueryManagerImpl;

/**
 * A ObjectContentManager implementation that uses the annotation mapper to map Java classes to JCR nodes
 * 
 * @author Philip Dodds
 */
public class AnnotatedObjectContentManagerImpl extends ObjectContentManagerImpl {

    private AnnotatedObjectMapper annotatedObjectMapper;

    public AnnotatedObjectContentManagerImpl(Session session, AnnotatedObjectMapper annotatedObjectMapper) {
        super(annotatedObjectMapper, getQueryManager(session, annotatedObjectMapper), session);
        this.annotatedObjectMapper = annotatedObjectMapper;
    }

    public AnnotatedObjectMapper getAnnotatedObjectMapper() {
        return this.annotatedObjectMapper;
    }

    public static QueryManager getQueryManager(Session session, AnnotatedObjectMapper annotatedObjectMapper) {
        DefaultAtomicTypeConverterProvider converterProvider = new DefaultAtomicTypeConverterProvider();
        Map atomicTypeConverters = converterProvider.getAtomicTypeConverters();
        QueryManagerImpl queryManager;

        try {
            queryManager = new QueryManagerImpl(annotatedObjectMapper, atomicTypeConverters, session.getValueFactory());
        } catch (UnsupportedRepositoryOperationException e) {
            throw new RuntimeException("Unable to get query manager for " + AnnotatedObjectContentManagerImpl.class
                    + " , opperation is not supported", e);
        } catch (RepositoryException e) {
            throw new RuntimeException("Unable to get query manager for " + AnnotatedObjectContentManagerImpl.class, e);
        }

        return queryManager;
    }

}
