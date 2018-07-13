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
 *
 */

package org.apache.jackrabbit.api.binary;

import java.net.URI;

import javax.jcr.RepositoryException;

import org.jetbrains.annotations.Nullable;

/**
 * This extension interface provides a mechanism whereby a client can download
 * a {@link javax.jcr.Binary} directly from a storage location.
 */
public interface BinaryDirectDownload {
    /**
     * Get a URI for downloading the binary directly from a storage location.
     * This is probably a signed URI with a short TTL, although the API does
     * not require it to be so.
     *
     * @return A URI for downloading the binary directly, or {@code null} if the
     *         binary cannot be download directly or if the underlying
     *         implementation does not support this capability.
     * @throws {@link RepositoryException} if an error occurs trying to locate
     *         the binary.
     */
    @Nullable
    URI getDownloadURI() throws RepositoryException;
}
