/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.api.binary;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Specifies the options to be used when obtaining a direct download URI via
 * {@link BinaryDownload#getURI(BinaryDownloadOptions)}.  Setting these options
 * allows the caller to instruct the service provider that these options should
 * be applied to the response to a request made with the URI returned.
 * <p>
 * To specify download options, obtain a {@link BinaryDownloadOptionsBuilder}
 * via the {@link #builder()} method, then specify the options desired and
 * get the object via {@link BinaryDownloadOptionsBuilder#build()}.
 * <p>
 * If no options are needed, use {@link BinaryDownloadOptions#DEFAULT} which
 * instructs the implementation to use the service provider default behavior.
 */
@ProviderType
public final class BinaryDownloadOptions {
    private final String mimeType;
    private final String encoding;
    private final String fileName;
    private final String dispositionType;

    private BinaryDownloadOptions(final String mimeType,
                                  final String encoding,
                                  final String fileName,
                                  final String dispositionType) {
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.fileName = fileName;
        this.dispositionType = dispositionType;
    }

    /**
     * Provides a default instance of this class.  Using the default instance
     * indicates that the caller is willing to accept the service provider
     * default behavior.
     */
    public static final BinaryDownloadOptions DEFAULT =
            BinaryDownloadOptions.builder().build();

    /**
     * Returns the MIME type that should be assumed for the binary that is to be
     * downloaded.  This value should be a valid {@code jcr:mimeType}.
     * <p>
     * If this value is set, this has the effect of instructing the service
     * provider to set this value as the MIME type of the content type in the
     * {@code Content-Type} header of the response to a request issued with a
     * URI obtained by calling {@link
     * BinaryDownload#getURI(BinaryDownloadOptions)}.  This value can be set by
     * calling {@link BinaryDownloadOptionsBuilder#withMimeType(String)} when
     * building an instance of this class.
     * <p>
     * Note that if the MIME type is text-based, the caller may also wish to set
     * the encoding which is done separately; see {@link #getEncoding()}.
     * <p>
     * The caller should ensure that the MIME type specified is valid; the
     * implementation is not required to perform any validation of this setting.
     * <p>
     * If no MIME type is specified, no {@code Content-Type} header will be
     * specified to the service provider.
     *
     * @return A String representation of the MIME type, or {@code null} if no
     *         MIME type has been specified.
     * @see <a href="https://docs.adobe.com/content/docs/en/spec/jcr/2.0/3_Repository_Model.html#3.7.11.10%20mix:mimeType">
     *     JCR 2.0 Repository Model - jcr:mimeType</a>
     */
    @Nullable
    public final String getMimeType() { return mimeType; }

    /**
     * Returns the encoding that should be assumed for the binary that is to be
     * downloaded.  This value should be a valid {@code jcr:encoding}.
     * <p>
     * If this value is set, this has the effect of instructing the service
     * provider to set this value as the encoding of the content type in the
     * {@code Content-Type} header of the response to a request issued with a
     * URI obtained by calling {@link
     * BinaryDownload#getURI(BinaryDownloadOptions)}.  This value can be set by
     * calling {@link BinaryDownloadOptionsBuilder#withEncoding(String)} when
     * building an instance of this class.
     * <p>
     * Note that setting the encoding only makes sense if the MIME type has also
     * been set to a text-based MIME type.  See {@link #getMimeType()}.
     * <p>
     * The caller should ensure that the proper encoding has been set for the
     * MIME type; the implementation is not required to perform any validation
     * of these settings.
     *
     * @return A String representation of the encoding, or {@code null} if no
     *         encoding has been specified.
     * @see <a href="https://docs.adobe.com/content/docs/en/spec/jcr/2.0/3_Repository_Model.html#3.7.11.10%20mix:mimeType">
     *     JCR 2.0 Repository Model - jcr:encoding</a>
     */
    @Nullable
    public final String getEncoding() { return encoding; }

    /**
     * Returns the filename that should be assumed for the binary that is to be
     * downloaded.
     * <p>
     * If this value is set, this has the effect of instructing the service
     * provider to set this value as the filename in the {@code
     * Content-Disposition} header of the response to a request issued with a
     * URI obtained by calling {@link
     * BinaryDownload#getURI(BinaryDownloadOptions)}.  This value can be set by
     * calling {@link BinaryDownloadOptionsBuilder#withFileName(String)} when
     * building an instance of this class.
     * <p>
     * Note that a disposition type is also required for the {@code
     * Content-Disposition} header.  If no disposition type is provided, {@code
     * inline} is the default.
     * <p>
     * If no filename is specified, no {@code Content-Disposition} header will
     * be specified to the service provider.
     *
     * @return A String representation of the file name, or {@code null} if no
     * file name has been specified.
     */
    @Nullable
    public final String getFileName() { return fileName; }

    /**
     * Returns the disposition type that should be assumed for the binary that
     * is to be downloaded.
     * <p>
     * If this value is set, this has the effect of instructing the service
     * provider to set this value as the disposition type in the {@code
     * Content-Disposition} header of the response to a request issued with a
     * URI obtained by calling {@link
     * BinaryDownload#getURI(BinaryDownloadOptions)}.  This value can be set by
     * calling {@link BinaryDownloadOptionsBuilder#withDispositionTypeInline()}
     * or {@link BinaryDownloadOptionsBuilder#withDispositionTypeAttachment()}
     * when building an instance of this class.
     * <p>
     * Note that a disposition type is required for the {@code
     * Content-Disposition} header.  If this value is not set, {@code inline} is
     * the default.
     *
     * @return A String representation of the disposition type, or {@code null}
     * if no disposition type has been specified.
     */
    @Nullable
    public final String getDispositionType() { return dispositionType; }

    /**
     * Returns a {@link BinaryDownloadOptionsBuilder} instance to be used for
     * creating an instance of this class.
     *
     * @return A builder instance.
     */
    @NotNull
    public static BinaryDownloadOptionsBuilder builder() {
        return new BinaryDownloadOptionsBuilder();
    }

    /**
     * Used to build an instance of {@link BinaryDownloadOptions} with the
     * options set as desired by the caller.
     */
    public static final class BinaryDownloadOptionsBuilder {
        private String mimeType = null;
        private String encoding = null;
        private String fileName = null;
        private DispositionType dispositionType = null;

        private BinaryDownloadOptionsBuilder() { }

        /**
         * Sets the MIME type of the {@link BinaryDownloadOptions} object to be
         * built.  This value should be a valid {@code jcr:mimeType}.
         * <p>
         * Calling this method has the effect of instructing the service
         * provider to set {@code mimeType} as the MIME type of the content type
         * in the {@code Content-Type} header of the response to a request
         * issued with a URI obtained by calling {@link
         * BinaryDownload#getURI(BinaryDownloadOptions)}.  This value can be
         * later retrieved by calling {@link
         * BinaryDownloadOptions#getMimeType()} on the instance returned from a
         * call to {@link #build()}.
         * <p>
         * Note that if the MIME type is text-based, the caller may also wish to
         * set the encoding which is done separately.  See {@link
         * #withEncoding(String)}.
         * <p>
         * The caller should ensure that the MIME type set is valid; the
         * implementation does not perform any validation of this setting.
         * <p>
         * If no MIME type is provided, no {@code Content-Type} header will be
         * specified to the service provider.
         *
         * @param mimeType A String representation of the jcr:mimeType.
         * @return The calling instance.
         * @see <a href="https://docs.adobe.com/content/docs/en/spec/jcr/2.0/3_Repository_Model.html#3.7.11.10%20mix:mimeType">
         *     JCR 2.0 Repository Model - jcr:mimeType</a>
         */
        @NotNull
        public BinaryDownloadOptionsBuilder withMimeType(@NotNull String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        /**
         * Sets the encoding of the {@link BinaryDownloadOptions} object to be
         * built.  This value should be a valid {@code jcr:encoding}.
         * <p>
         * Calling this method has the effect of instructing the service
         * provider to set {@code encoding} as the encoding of the content type
         * in the {@code Content-Type} header of the response to a request
         * issued with a URI obtained by calling {@link
         * BinaryDownload#getURI(BinaryDownloadOptions)}.  This value can be
         * later retrieved by calling {@link
         * BinaryDownloadOptions#getEncoding()} on the instance returned by a
         * call to {@link #build()}.
         * <p>
         * Note that setting the encoding only makes sense if the MIME type has
         * also been set to a text-based MIME type.  See {@link
         * #withMimeType(String)}.
         * <p>
         * The caller should ensure that the proper encoding has been set for
         * the MIME type; the implementation does not perform any validation of
         * these settings.
         *
         * @param encoding A String representation of the jcr:encoding.
         * @return The calling instance.
         * @see <a href="https://docs.adobe.com/content/docs/en/spec/jcr/2.0/3_Repository_Model.html#3.7.11.10%20mix:mimeType">
         *     JCR 2.0 Repository Model - jcr:encoding</a>
         */
        @NotNull
        public BinaryDownloadOptionsBuilder withEncoding(@NotNull String encoding) {
            this.encoding = encoding;
            return this;
        }

        /**
         * Sets the filename of the {@link BinaryDownloadOptions} object to be
         * built.
         * <p>
         * Calling this method has the effect of instructing the service
         * provider to set {@code fileName} as the filename in the {@code
         * Content-Disposition} header of the response to a request issued with
         * a URI obtained by calling {@link
         * BinaryDownload#getURI(BinaryDownloadOptions)}.  This value can be
         * later retrieved by calling {@link
         * BinaryDownloadOptions#getFileName()} on the instance returned by a
         * call to {@link #build()}.
         * <p>
         * Note that a disposition type is also required for the {@code
         * Content-Disposition} header.  If no disposition type is provided,
         * {@code inline} is the default.  See {@link
         * #withDispositionTypeInline()} or {@link
         * #withDispositionTypeAttachment()} to set the disposition type.
         * <p>
         * If no filename is provided, no {@code Content-Disposition} header
         * will be specified to the service provider.
         *
         * @param fileName A String representation of the filename.
         * @return The calling instance.
         */
        @NotNull
        public BinaryDownloadOptionsBuilder withFileName(@NotNull String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * Sets the disposition type of the {@link BinaryDownloadOptions} object
         * to be built to {@code inline}.
         * <p>
         * Calling this method has the effect of instructing the service
         * provider to set the disposition type in the {@code
         * Content-Disposition} header of the response to {@code inline}.  This
         * value can be later retrieved by calling {@link
         * BinaryDownloadOptions#getDispositionType()} on the instance built by
         * calling {@link #build()}.
         * <p>
         * Note that a disposition type is required for the {@code
         * Content-Disposition} header.  If this value is not set, {@code
         * inline} is the default.
         * <p>
         * Note that a fileName must also be set for the {@code
         * Content-Disposition} header to be specified to the service provider.
         * See {@link #withFileName(String)}.
         *
         * @return The calling instance.
         */
        @NotNull
        public BinaryDownloadOptionsBuilder withDispositionTypeInline() {
            dispositionType = DispositionType.INLINE;
            return this;
        }

        /**
         * Sets the disposition type of the {@link BinaryDownloadOptions} object
         * to be built to {@code attachment}.
         * <p>
         * Calling this method has the effect of instructing the service
         * provider to set the disposition type in the {@code
         * Content-Disposition} header of the response to {@code attachment}.
         * This value can later be retrieved by calling {@link
         * BinaryDownloadOptions#getDispositionType()} on the instance built by
         * calling {@link #build()}.
         * <p>
         * Note that a disposition type is required for the {@code
         * Content-Disposition} header.  If this value is not set, {@code
         * inline} is the default.
         * <p>
         * Note that a fileName must also be set for the {@code
         * Content-Disposition} header to be specified to the service provider.
         * See {@link #withFileName(String)}.
         *
         * @return The calling instance.
         */
        @NotNull
        public BinaryDownloadOptionsBuilder withDispositionTypeAttachment() {
            dispositionType = DispositionType.ATTACHMENT;
            return this;
        }

        /**
         * Construct a {@link BinaryDownloadOptions} instance with the
         * properties specified to the builder.
         *
         * @return A new {@link BinaryDownloadOptions} instance built with the
         *         properties specified to the builder.
         */
        @NotNull
        public BinaryDownloadOptions build() {
            return new BinaryDownloadOptions(mimeType,
                    encoding,
                    fileName,
                    null != dispositionType ? dispositionType.toString() : null
            );
        }

        private enum DispositionType {
            INLINE("inline"),
            ATTACHMENT("attachment");

            private final String value;

            DispositionType(final String value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return value;
            }
        }
    }
}
