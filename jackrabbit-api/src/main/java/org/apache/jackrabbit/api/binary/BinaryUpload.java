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

import org.apache.jackrabbit.api.JackrabbitValueFactory;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This extension interface provides a mechanism whereby a client can upload a
 * binary directly to a storage location.  An object of this type can be
 * created by a call to {@link
 * JackrabbitValueFactory#initiateBinaryUpload(long, int)} which will return an
 * object of this type if the underlying implementation supports direct upload
 * functionality.  When calling this method, the client indicates the expected
 * size of the binary and the number of URIs that it is willing to accept.  The
 * implementation will attempt to create an instance of this class that is
 * suited to enabling the client to complete the upload successfully.
 * <p>
 * Using an instance of this class, a client can then use one or more of the
 * included URIs for uploading the binary directly by calling {@link
 * #getUploadURIs()} and iterating through the URIs returned.  Multi-part
 * uploads are supported by the interface, although they may not be supported
 * by the underlying implementation.
 * <p>
 * Once a client finishes uploading the binary data, the client must then call
 * {@link JackrabbitValueFactory#completeBinaryUpload(String)} to complete the
 * upload.  This call requires an upload token which can be obtained from an
 * instance of this class by calling {@link #getUploadToken()}.
 */
@ProviderType
public interface BinaryUpload {
    /**
     * Returns an Iterable of URIs that can be used for uploading binary data
     * directly to a storage location.  The first URI can be used for uploading
     * binary data as a single entity, or multiple URIs can be used if the
     * client wishes to do multi-part uploads.
     * <p>
     * Clients are not necessarily required to use all of the URIs provided.  A
     * client may choose to use fewer, or even only one of the URIs.  However,
     * regardless of the number of URIs used, they must be consumed in sequence.
     * For example, if a client wishes to upload a binary in three parts and
     * there are five URIs returned, the client must use the first URI to
     * upload the first part, the second URI to upload the second part, and
     * the third URI to upload the third part.  The client is not required to
     * use the fourth and fifth URIs.  However, using the second URI to upload
     * the third part may result in either an upload failure or a corrupted
     * upload; likewise, skipping the second URI to use subsequent URIs may
     * result in either an upload failure or a corrupted upload.
     * <p>
     * Clients should be aware that some storage providers have limitations on
     * the minimum and maximum size of a binary payload for a single upload, so
     * clients should take these limitations into account when deciding how many
     * of the URIs to use.  Underlying implementations may also choose to
     * enforce their own limitations.
     * <p>
     * While the API supports multi-part uploading via multiple upload URIs,
     * implementations are not required to support multi-part uploading.  If the
     * underlying implementation does not support multi-part uploading, a single
     * URI will be returned regardless of the size of the data being uploaded.
     * <p>
     * Some storage providers also support multi-part uploads by reusing a
     * single URI multiple times, in which case the implementation may also
     * return a single URI regardless of the size of the data being uploaded.
     * <p>
     * You should consult both the DataStore implementation documentation and
     * the storage service provider documentation for details on such matters as
     * multi-part upload support, upload minimum and maximum sizes, etc.
     *
     * @return Iterable of URIs that can be used for uploading directly to a
     *         storage location.
     */
    @NotNull
    Iterable<URI> getUploadURIs();

    /**
     * The smallest part size a client may upload for a multi-part upload, not
     * counting the final part.  This is usually either a service provider or
     * implementation limitation.
     * <p>
     * Note that the API offers no guarantees that uploading parts of this size
     * can successfully complete the requested upload using the URIs provided
     * via {@link #getUploadURIs()}.  In other words, clients wishing to perform
     * a multi-part upload must split the upload into parts of at least this
     * size, but the sizes may need to be larger in order to successfully
     * complete the upload.
     *
     * @return The smallest size acceptable for multi-part uploads.
     */
    long getMinPartSize();

    /**
     * The largest part size a client may upload for a multi-part upload.  This
     * is usually either a service provider or implementation limitation.
     * <p>
     * The API guarantees that a client can successfully complete a direct
     * upload of the binary data of the requested size using the provided URIs
     * by splitting the binary data into parts of the size returned by this
     * method.
     * <p>
     * The client is not required to use part sizes of this size; smaller sizes
     * may be used so long as they are at least as large as the size returned by
     * {@link #getMinPartSize()}.
     * <p>
     * If the binary size specified by a client when calling {@link
     * JackrabbitValueFactory#initiateBinaryUpload(long, int)} ends up being
     * smaller than the actual size of the binary being uploaded, these API
     * guarantees no longer apply, and it may not be possible to complete the
     * upload using the URIs provided.  In such cases, the client should restart
     * the transaction using the correct size.
     *
     * @return The maximum size of an upload part for multi-part uploads.
     */
    long getMaxPartSize();

    /**
     * Returns the upload token to be used in a subsequent call to {@link
     * JackrabbitValueFactory#completeBinaryUpload(String)}.  This upload token
     * is used by the implementation to identify this upload.  Clients should
     * treat the upload token as an immutable string, as the underlying
     * implementation may choose to implement techniques to detect tampering and
     * reject the upload if the token is modified.
     *
     * @return This upload's unique upload token.
     */
    @NotNull
    String getUploadToken();
}
