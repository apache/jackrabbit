package org.apache.jackrabbit.api.binary;

import java.util.Properties;

public class BinaryDirectDownloadOptions {
    private final String contentType;
    private final String name;

    private BinaryDirectDownloadOptions(final String contentType,
                                        final String name) {
        this.contentType = contentType;
        this.name = name;
    }

    public final String getContentType() {
        return contentType;
    }

    public final Properties toProperties() {
        Properties props = new Properties();
        props.put("Content-Type", contentType);
        if (null != name && 0 != name.length()) {
            props.put("Content-Disposition", name);
        }
        return props;
    }

    public static BinaryDirectDownloadOptionsBuilder builder() {
        return new BinaryDirectDownloadOptionsBuilder();
    }

    public static class BinaryDirectDownloadOptionsBuilder {
        private String contentType = "application/octet-stream";
        private String name = null;

        private BinaryDirectDownloadOptionsBuilder() { }

        public BinaryDirectDownloadOptionsBuilder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public BinaryDirectDownloadOptionsBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public BinaryDirectDownloadOptions build() {
            return new BinaryDirectDownloadOptions(contentType, name);
        }
    }
}
