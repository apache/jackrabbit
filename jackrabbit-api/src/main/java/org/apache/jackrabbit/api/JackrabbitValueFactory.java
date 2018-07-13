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

package org.apache.jackrabbit.api;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.api.binary.BinaryDirectUpload;

public interface JackrabbitValueFactory extends ValueFactory {
    /**
     * Initiate a transaction to upload binary data directly to a storage
     * location.  {@link IllegalArgumentException} will be thrown if an upload
     * cannot be supported for the required parameters, or if the parameters are
     * otherwise invalid.  For example, if the value of {@code maxSize} exceeds
     * the size limits for a single binary of the implementation or the service
     * provider, or if the value of {@code maxSize} divided by {@code maxParts}
     * exceeds the size limit for an upload or upload part of the implementation
     * or the service provider, {@link IllegalArgumentException} may be thrown.
     * <p>
     * Each service provider has specific limitations on upload sizes,
     * multi-part upload support, part sizes, etc. which can result in {@link
     * IllegalArgumentException} being thrown.  You should consult the
     * documentation for your underlying implementation and your service
     * provider for details.
     * <p>
     * If this call is successful, a {@link BinaryDirectUpload} is returned
     * which contains the information a client needs to successfully complete
     * a direct upload.
     *
     * @param maxSize The expected maximum size of the binary to be uploaded by
     *         the client.  If the actual size of the binary is known, this
     *         size should be used; otherwise, the client should make a best
     *         guess.  If a client calls {@link
     *         #initiateBinaryUpload(long, int)} with one size and then later
     *         determines that the guess was too small, the transaction should
     *         be restarted by calling {@link #initiateBinaryUpload(long, int)}
     *         again with the correct size.
     * @param maxURIs The maximum number of upload URIs that the client can
     *         accept.  The implementation will ensure that an upload of
     *         {@code maxSize} can be completed by splitting the binary into
     *         a number of parts not to exceed the value of {@code maxURIs}.  If
     *         this is not possible, {@link IllegalArgumentException} will be
     *         thrown.  A client may specify -1 for this value, indicating that
     *         any number of URIs may be returned.
     * @return A {@link BinaryDirectUpload} that can be used by the client to
     *         complete the upload.
     * @throws {@link IllegalArgumentException} if the provided arguments are
     *         invalid or if a valid upload cannot be completed given the
     *         provided arguments, or {@link AccessDeniedException} if it is
     *         determined that insufficient permission exists to perform the
     *         upload.
     */
    BinaryDirectUpload initiateBinaryUpload(long maxSize, int maxURIs)
            throws IllegalArgumentException, AccessDeniedException;

    /**
     * Complete a transaction to upload binary data directly to a storage
     * location.  The client must provide a valid {@code uploadToken} that can
     * only be obtained via a previous call to {@link
     * #initiateBinaryUpload(long, int)}.  If the {@code uploadToken} is
     * unreadable or invalid, {@link IllegalArgumentException} will be thrown.
     * <p>
     * Calling {@link #completeBinaryUpload(String)} does not associate the
     * returned {@link Binary} with any location in the repository.  It is the
     * responsibility of the client to do this if desired.
     * <p>
     * The {@code uploadToken} can be obtained from the {@link
     * BinaryDirectUpload} returned from a prior call to {@link
     * #initiateBinaryUpload(long, int)}.  Clients should treat the {@code
     * uploadToken} as an immutable string, and should expect that
     * implementations will sign the string and verify the signature when {@link
     * #completeBinaryUpload(String)} is called.
     *
     * @param uploadToken A string that is used to identify the direct upload
     *         transaction.
     * @return The {@link Binary} object created as a result of the transaction.
     * @throws {@link IllegalArgumentException} if the {@code uploadToken} is
     *         unreadable or invalid, or {@link RepositoryException} if a
     *         repository access error occurs.
     */
    Binary completeBinaryUpload(String uploadToken)
            throws IllegalArgumentException, RepositoryException;
}
