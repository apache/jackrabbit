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
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>Util</code> provides various static utility methods.
 */
public class Util {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    /**
     * Disposes the document <code>old</code>. Closes any potentially open
     * readers held by the document.
     * 
     * @param old
     *            the document to dispose.
     */
    public static void disposeDocument(Document old) {
        for (Fieldable f : old.getFields()) {
            try {
                if (f.readerValue() != null) {
                    f.readerValue().close();
                } else if (f instanceof LazyTextExtractorField) {
                    LazyTextExtractorField field = (LazyTextExtractorField) f;
                    field.dispose();
                }
            } catch (IOException ex) {
                log.warn("Exception while disposing index document: " + ex);
            }
        }
    }

    /**
     * Returns <code>true</code> if the document is ready to be added to the
     * index. That is all text extractors have finished their work.
     * 
     * @param doc
     *            the document to check.
     * @return <code>true</code> if the document is ready; <code>false</code>
     *         otherwise.
     */
    public static boolean isDocumentReady(Document doc) {
        for (Fieldable f : doc.getFields()) {
            if (f instanceof LazyTextExtractorField) {
                LazyTextExtractorField field = (LazyTextExtractorField) f;
                if (!field.isExtractorFinished()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Depending on the index format this method returns a query that matches
     * all nodes that have a property with a given <code>name</code>.
     * 
     * @param name
     *            the property name.
     * @param version
     *            the index format version.
     * @return Query that matches all nodes that have a property with the given
     *         <code>name</code>.
     */
    public static Query createMatchAllQuery(String name,
            IndexFormatVersion version, PerQueryCache cache) {
        if (version.getVersion() >= IndexFormatVersion.V2.getVersion()) {
            // new index format style
            return new JackrabbitTermQuery(new Term(FieldNames.PROPERTIES_SET,
                    name));
        } else {
            return new MatchAllQuery(name, cache);
        }
    }

    /**
     * Creates an {@link IOException} with <code>t</code> as its cause.
     * 
     * @param t
     *            the cause.
     */
    public static IOException createIOException(Throwable t) {
        IOException ex = new IOException(t.getMessage());
        ex.initCause(t);
        return ex;
    }

    /**
     * Depending on the type of the <code>reader</code> this method either
     * closes or releases the reader. The reader is released if it implements
     * {@link ReleaseableIndexReader}.
     * 
     * @param reader
     *            the index reader to close or release.
     * @throws IOException
     *             if an error occurs while closing or releasing the index
     *             reader.
     */
    public static void closeOrRelease(IndexReader reader) throws IOException {
        if (reader instanceof ReleaseableIndexReader) {
            ((ReleaseableIndexReader) reader).release();
        } else {
            reader.close();
        }
    }

    /**
     * Returns a comparable for the internal <code>value</code>.
     * 
     * @param value
     *            an internal value.
     * @return a comparable for the given <code>value</code>.
     * @throws RepositoryException
     *             if retrieving the <code>Comparable</code> fails.
     */
    public static Comparable getComparable(InternalValue value)
            throws RepositoryException {
        switch (value.getType()) {
        case PropertyType.BINARY:
            return null;
        case PropertyType.BOOLEAN:
            return value.getBoolean();
        case PropertyType.DATE:
            return value.getDate().getTimeInMillis();
        case PropertyType.DOUBLE:
            return value.getDouble();
        case PropertyType.LONG:
            return value.getLong();
        case PropertyType.DECIMAL:
            return value.getDecimal();
        case PropertyType.NAME:
            return value.getName().toString();
        case PropertyType.PATH:
            return value.getPath().toString();
        case PropertyType.URI:
        case PropertyType.WEAKREFERENCE:
        case PropertyType.REFERENCE:
        case PropertyType.STRING:
            return value.getString();
        default:
            return null;
        }
    }

    /**
     * Returns a comparable for the internal <code>value</code>.
     * 
     * @param value
     *            an internal value.
     * @return a comparable for the given <code>value</code>.
     * @throws ValueFormatException
     *             if the given <code>value</code> cannot be converted into a
     *             comparable (i.e. unsupported type).
     * @throws RepositoryException
     *             if an error occurs while converting the value.
     */
    public static Comparable getComparable(Value value)
            throws ValueFormatException, RepositoryException {
        switch (value.getType()) {
        case PropertyType.BOOLEAN:
            return value.getBoolean();
        case PropertyType.DATE:
            return value.getDate().getTimeInMillis();
        case PropertyType.DOUBLE:
            return value.getDouble();
        case PropertyType.LONG:
            return value.getLong();
        case PropertyType.DECIMAL:
            return value.getDecimal();
        case PropertyType.NAME:
        case PropertyType.PATH:
        case PropertyType.URI:
        case PropertyType.WEAKREFERENCE:
        case PropertyType.REFERENCE:
        case PropertyType.STRING:
            return value.getString();
        default:
            throw new RepositoryException("Unsupported type: "
                    + PropertyType.nameFromValue(value.getType()));
        }
    }

    /**
     * Compares values <code>c1</code> and <code>c2</code>. If the values have
     * differing types, then the order is defined on the type itself by calling
     * <code>compareTo()</code> on the respective type class names.
     * 
     * @param c1
     *            the first value.
     * @param c2
     *            the second value.
     * @return a negative integer if <code>c1</code> should come before
     *         <code>c2</code><br>
     *         a positive integer if <code>c1</code> should come after
     *         <code>c2</code><br>
     *         <code>0</code> if they are equal.
     */
    public static int compare(Comparable c1, Comparable c2) {
        if (c1 == c2) {
            return 0;
        } else if (c1 == null) {
            return -1;
        } else if (c2 == null) {
            return 1;
        } else if (c1.getClass() == c2.getClass()) {
            return c1.compareTo(c2);
        } else {
            // differing types -> compare class names
            String name1 = c1.getClass().getName();
            String name2 = c2.getClass().getName();
            return name1.compareTo(name2);
        }
    }

    /**
     * Compares two arrays of Comparable(s) in the same style as
     * {@link #compare(Value[], Value[])}.
     * 
     * The 2 methods *have* to work in the same way for the sort to be
     * consistent
     */
    public static int compare(Comparable<?>[] c1, Comparable<?>[] c2) {
        if(c1 == null && c2 == null){
            return 0;
        }
        if (c1 == null) {
            return -1;
        }
        if (c2 == null) {
            return 1;
        }
        for (int i = 0; i < c1.length && i < c2.length; i++) {
            int d = compare(c1[i], c2[i]);
            if (d != 0) {
                return d;
            }
        }
        return c1.length - c2.length;
    }

    /**
     * Compares two arrays of Value(s) in the same style as
     * {@link #compare(Comparable[], Comparable[])}.
     * 
     * The 2 methods *have* to work in the same way for the sort to be
     * consistent
     */
    public static int compare(Value[] a, Value[] b) throws RepositoryException {
        if(a == null && b == null){
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        for (int i = 0; i < a.length && i < b.length; i++) {
            int d = compare(a[i], b[i]);
            if (d != 0) {
                return d;
            }
        }
        return a.length - b.length;
    }

    /**
     * Compares the two values. If the values have differing types, then an
     * attempt is made to convert the second value into the type of the first
     * value.
     * <p>
     * Comparison of binary values is not supported.
     * 
     * @param v1
     *            the first value.
     * @param v2
     *            the second value.
     * @return result of the comparison as specified in
     *         {@link Comparable#compareTo(Object)}.
     * @throws ValueFormatException
     *             if the given <code>value</code> cannot be converted into a
     *             comparable (i.e. unsupported type).
     * @throws RepositoryException
     *             if an error occurs while converting the value.
     */
    public static int compare(Value v1, Value v2) throws ValueFormatException,
            RepositoryException {
        Comparable c1 = getComparable(v1);
        Comparable c2;
        switch (v1.getType()) {
        case PropertyType.BOOLEAN:
            c2 = v2.getBoolean();
            break;
        case PropertyType.DATE:
            c2 = v2.getDate().getTimeInMillis();
            break;
        case PropertyType.DOUBLE:
            c2 = v2.getDouble();
            break;
        case PropertyType.LONG:
            c2 = v2.getLong();
            break;
        case PropertyType.DECIMAL:
            c2 = v2.getDecimal();
            break;
        case PropertyType.NAME:
            if (v2.getType() == PropertyType.URI) {
                String s = v2.getString();
                if (s.startsWith("./")) {
                    s = s.substring(2);
                }
                // need to decode
                try {
                    c2 = URLDecoder.decode(s, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RepositoryException(e);
                }
            } else {
                c2 = v2.getString();
            }
            break;
        case PropertyType.PATH:
        case PropertyType.REFERENCE:
        case PropertyType.WEAKREFERENCE:
        case PropertyType.URI:
        case PropertyType.STRING:
            c2 = v2.getString();
            break;
        default:
            throw new RepositoryException("Unsupported type: "
                    + PropertyType.nameFromValue(v2.getType()));
        }
        return compare(c1, c2);
    }

    /**
     * Creates a regexp from <code>likePattern</code>.
     * 
     * @param likePattern
     *            the pattern.
     * @return the regular expression <code>Pattern</code>.
     */
    public static Pattern createRegexp(String likePattern) {
        // - escape all non alphabetic characters
        // - escape constructs like \<alphabetic char> into \\<alphabetic char>
        // - replace non escaped _ % into . and .*
        StringBuffer regexp = new StringBuffer();
        boolean escaped = false;
        for (int i = 0; i < likePattern.length(); i++) {
            if (likePattern.charAt(i) == '\\') {
                if (escaped) {
                    regexp.append("\\\\");
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else {
                if (Character.isLetterOrDigit(likePattern.charAt(i))) {
                    if (escaped) {
                        regexp.append("\\\\").append(likePattern.charAt(i));
                        escaped = false;
                    } else {
                        regexp.append(likePattern.charAt(i));
                    }
                } else {
                    if (escaped) {
                        regexp.append('\\').append(likePattern.charAt(i));
                        escaped = false;
                    } else {
                        switch (likePattern.charAt(i)) {
                        case '_':
                            regexp.append('.');
                            break;
                        case '%':
                            regexp.append(".*");
                            break;
                        default:
                            regexp.append('\\').append(likePattern.charAt(i));
                        }
                    }
                }
            }
        }
        return Pattern.compile(regexp.toString(), Pattern.DOTALL);
    }

    /**
     * Returns length of the internal value.
     * 
     * @param value
     *            a value.
     * @return the length of the internal value or <code>-1</code> if the length
     *         cannot be determined.
     */
    public static long getLength(InternalValue value) {
        if (value.getType() == PropertyType.NAME
                || value.getType() == PropertyType.PATH) {
            return -1;
        } else {
            try {
                return value.getLength();
            } catch (RepositoryException e) {
                log.warn("Unable to determine length of value. {}", e.getMessage());
                return -1;
            }
        }
    }
}
