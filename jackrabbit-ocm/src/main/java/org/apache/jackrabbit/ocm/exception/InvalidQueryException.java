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
package org.apache.jackrabbit.ocm.exception;

/**
 * The <code>InvalidQuerySyntaxException</code> is an
 * <code>ObjectContentManagerException</code> thrown if the query of the
 * {@link org.apache.jackrabbit.ocm.manager.ObjectContentManager#getObjectIterator(String, String)}
 * is invalid. This exception actually wraps a standard JCR
 * <code>javax.jcr.InvalidQueryException</code> to make it an unchecked
 * exception. The cause of this exception will always be the original
 * <code>InvalidQueryException</code> and the message will always be the
 * original exception's message.
 */
public class InvalidQueryException extends ObjectContentManagerException {

    /**
     * Create an exception wrapping the given checked JCR exception
     *
     * @param cause The wrapped JCR <code>InvalidQueryException</code>
     */
    public InvalidQueryException(javax.jcr.query.InvalidQueryException cause) {
        super(cause.getMessage(), cause);
    }
}
