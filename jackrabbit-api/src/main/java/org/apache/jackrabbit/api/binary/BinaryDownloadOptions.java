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

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public class BinaryDownloadOptions {
    private final String contentType;
    private final String contentTypeEncoding;
    private final String fileName;
    private final String dispositionType;

    private BinaryDownloadOptions(final String contentType,
                                  final String contentTypeEncoding,
                                  final String fileName,
                                  final String dispositionType) {
        this.contentType = contentType;
        this.contentTypeEncoding = contentTypeEncoding;
        this.fileName = fileName;
        this.dispositionType = dispositionType;
    }

    public static final BinaryDownloadOptions DEFAULT = BinaryDownloadOptions.builder().build();

    public final String getContentType() {
        return contentType;
    }

    public final String getContentTypeEncoding() { return contentTypeEncoding; }

    public final String getFileName() { return fileName; }

    public final String getDispositionType() { return dispositionType; }

    public static BinaryDownloadOptionsBuilder builder() {
        return new BinaryDownloadOptionsBuilder();
    }

    public static class BinaryDownloadOptionsBuilder {
        private String contentType = null;
        private String contentTypeEncoding = null;
        private String fileName = null;
        private DispositionType dispositionType = null;

        private BinaryDownloadOptionsBuilder() { }

        public BinaryDownloadOptionsBuilder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public BinaryDownloadOptionsBuilder withContentTypeEncoding(String contentTypeEncoding) {
            this.contentTypeEncoding = contentTypeEncoding;
            return this;
        }

        public BinaryDownloadOptionsBuilder withFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public BinaryDownloadOptionsBuilder withDispositionTypeInline() {
            dispositionType = DispositionType.INLINE;
            return this;
        }

        public BinaryDownloadOptionsBuilder withDispositionTypeAttachment() {
            dispositionType = DispositionType.ATTACHMENT;
            return this;
        }

        public BinaryDownloadOptions build() {
            return new BinaryDownloadOptions(contentType,
                    contentTypeEncoding,
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
