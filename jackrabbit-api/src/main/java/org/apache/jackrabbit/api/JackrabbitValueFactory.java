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
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.api.binary.BinaryUpload;
import org.apache.jackrabbit.api.binary.BinaryDownload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines optional functionality that a {@link ValueFactory} may choose to
 * provide.  A {@link ValueFactory} may also implement this interface without
 * supporting all of the capabilities in this interface.  Each method of the
 * interface describes the behavior of that method if the underlying capability
 * is not available.
 * <p>
 * Currently this interface defines the following optional features:
 * <ul>
 *     <li>Direct Binary Access - enable a client to upload or download binaries
 *         directly to/from a storage location
 * </ul>
 * <p>
 * The features are described in more detail below.
 *
 * <h2>Direct Binary Access</h2>
 * <p>
 * The Direct Binary Access feature provides the capability for a client to
 * upload or download binaries directly to/from a storage location.  For
 * example, this might be a cloud storage providing high-bandwidth direct
 * network access.  This API allows for requests to be authenticated and for
 * access permission checks to take place within the repository, but for clients
 * to then access the storage location directly.
 * <p>
 * The feature consists of two parts, direct binary upload and direct binary
 * download.
 * <p>
 *
 * <h3>Direct Binary Upload</h3>
 * <p>
 * This feature enables remote clients to upload binaries directly to a storage
 * location.
 * <p>
 * When adding binaries already present on the same JVM or server as Jackrabbit
 * or Oak, for example because they were generated locally, please use the
 * regular JCR API for {@link javax.jcr.Property#setValue(Binary) adding
 * binaries through input streams} instead. This feature is solely designed for
 * remote clients.
 * <p>
 * The direct binary upload process is split into 3 phases:
 * <ol>
 *     <li>
 *         <b>Initialize</b>: A remote client makes request to the
 *         Jackrabbit-based application to request an upload, which calls {@link
 *         #initiateBinaryUpload(long, int)} and returns the resulting {@link
 *         BinaryUpload information} to the remote client.
 *     </li>
 *     <li>
 *         <b>Upload</b>: The remote client performs the actual binary upload
 *         directly to the binary storage provider.  The {@link BinaryUpload}
 *         returned from the previous call to {@link
 *         #initiateBinaryUpload(long, int)} contains detailed instructions on
 *         how to complete the upload successfully. For more information, see
 *         the BinaryUpload documentation.
 *     </li>
 *     <li>
 *         <b>Complete</b>: The remote client notifies the Jackrabbit-based
 *         application that step 2 is complete.  The upload token returned in
 *         the first step (obtained by calling {@link
 *         BinaryUpload#getUploadToken()} is passed by the client to {@link
 *         #completeBinaryUpload(String)}. This will provide the application
 *         with a regular {@link Binary JCR Binary} that can then be used to
 *         write JCR content including the binary (such as an nt:file structure)
 *         and {@link Session#save() persist} it.
 *     </li>
 * </ol>
 * <p>
 * <h3>Direct Binary Download</h3>
 * <p>
 * The direct binary download process is described in detail in {@link
 * BinaryDownload}.
 */
@ProviderType
public interface JackrabbitValueFactory extends ValueFactory {
    /**
     * Initiate a transaction to upload binary data directly to a storage
     * location.  {@link IllegalArgumentException} will be thrown if an upload
     * cannot be supported for the required parameters, or if the parameters are
     * otherwise invalid.  For example, if the value of {@code maxSize} exceeds
     * the size limits for a single binary upload for the implementation or the
     * service provider, or if the value of {@code maxSize} divided by {@code
     * maxParts} exceeds the size limit for an upload or upload part of the
     * implementation or the service provider, {@link IllegalArgumentException}
     * may be thrown.
     * <p>
     * Each service provider has specific limitations on upload sizes,
     * multi-part upload support, part sizes, etc. which can result in {@link
     * IllegalArgumentException} being thrown.  You should consult the
     * documentation for your underlying implementation and your service
     * provider for details.
     * <p>
     * If this call is successful, a {@link BinaryUpload} is returned
     * which contains the information a client needs to successfully complete
     * a direct upload.
     *
     * @param maxSize The expected maximum size of the binary to be uploaded by
     *         the client.  If the actual size of the binary is known, this
     *         size should be used; otherwise, the client should make a best
     *         guess.  If a client calls this method with one size and then
     *         later determines that the guess was too small, the transaction
     *         should be restarted by calling this method again with the correct
     *         size.
     * @param maxURIs The maximum number of upload URIs that the client can
     *         accept.  The implementation will ensure that an upload of size
     *         {@code maxSize} can be completed by splitting the value of {@code
     *         maxSize} into parts, such that the size of the largest part does
     *         not exceed any known implementation or service provider
     *         limitations on upload part size and such that the number of parts
     *         does not exceed the value of {@code maxURIs}.  If this is not
     *         possible, {@link IllegalArgumentException} will be thrown.  A
     *         client may specify -1 for this value, indicating that any number
     *         of URIs may be returned.
     * @return A {@link BinaryUpload} that can be used by the client to complete
     *         the upload via a call to {@link #completeBinaryUpload(String)},
     *         or {@code null} if the implementation does not support the direct
     *         upload feature.
     * @throws IllegalArgumentException if the provided arguments are
     *         invalid or if a valid upload cannot be completed given the
     *         provided arguments.
     * @throws AccessDeniedException if it is determined that insufficient
     *         permission exists to perform the upload.
     */
    @Nullable
    BinaryUpload initiateBinaryUpload(long maxSize, int maxURIs)
            throws IllegalArgumentException, AccessDeniedException;

    /**
     * Complete a transaction to upload binary data directly to a storage
     * location.  The client must provide a valid {@code uploadToken} that can
     * only be obtained via a previous call to {@link
     * #initiateBinaryUpload(long, int)}.  If the {@code uploadToken} is
     * unreadable or invalid, {@link IllegalArgumentException} will be thrown.
     * <p>
     * Calling this method does not associate the returned {@link Binary} with
     * any location in the repository.  It is the responsibility of the client
     * to do this if desired.
     * <p>
     * The {@code uploadToken} can be obtained from the {@link
     * BinaryUpload} returned from a prior call to {@link
     * #initiateBinaryUpload(long, int)}.  Clients should treat the {@code
     * uploadToken} as an immutable string, and should expect that
     * implementations will sign the string and verify the signature when this
     * method is called.
     *
     * @param uploadToken A String that is used to identify the direct upload
     *         transaction.
     * @return The uploaded {@link Binary}, or {@code null} if the
     *         implementation does not support the direct upload feature.
     * @throws IllegalArgumentException if the {@code uploadToken} is
     *         unreadable or invalid.
     * @throws RepositoryException if a repository access error occurs.
     */
    @Nullable
    Binary completeBinaryUpload(@NotNull String uploadToken)
            throws IllegalArgumentException, RepositoryException;
}
