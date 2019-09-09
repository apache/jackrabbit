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

/**
 * CharSequence that applies an offset to a base CharSequence. The base
 * CharSequence can be replaced without creating a new CharSequence.
 */
final class OffsetCharSequence implements CharSequence, Comparable<OffsetCharSequence>, TransformConstants {

    /**
     * Indicates how the underlying char sequence is exposed / tranformed.
     */
    private final int transform;

    /**
     * The offset to apply to the base CharSequence
     */
    private final int offset;

    /**
     * The base character sequence
     */
    private CharSequence base;

    /**
     * Creates a new OffsetCharSequence with an <code>offset</code>.
     *
     * @param offset    the offset
     * @param base      the base CharSequence
     * @param transform how the <code>base</code> char sequence is
     *                  tranformed.
     */
    OffsetCharSequence(int offset, CharSequence base, int transform) {
        this.offset = offset;
        this.base = base;
        this.transform = transform;
    }

    /**
     * Creates a new OffsetCharSequence with an <code>offset</code>.
     *
     * @param offset    the offset
     * @param base      the base CharSequence
     */
    OffsetCharSequence(int offset, CharSequence base) {
        this(offset, base, TRANSFORM_NONE);
    }

    /**
     * Sets a new base sequence.
     *
     * @param base the base character sequence
     */
    public void setBase(CharSequence base) {
        this.base = base;
    }

    /**
     * @inheritDoc
     */
    public int length() {
        return base.length() - offset;
    }

    /**
     * @inheritDoc
     */
    public char charAt(int index) {
        if (transform == TRANSFORM_NONE) {
            return base.charAt(index + offset);
        } else if (transform == TRANSFORM_LOWER_CASE) {
            return Character.toLowerCase(base.charAt(index + offset));
        } else if (transform == TRANSFORM_UPPER_CASE) {
            return Character.toUpperCase(base.charAt(index + offset));
        }
        // shouldn't get here. return plain character
        return base.charAt(index + offset);
    }

    /**
     * @inheritDoc
     */
    public CharSequence subSequence(int start, int end) {
        CharSequence seq = base.subSequence(start + offset, end + offset);
        if (transform != TRANSFORM_NONE) {
            seq = new OffsetCharSequence(0, seq, transform);
        }
        return seq;
    }

    /**
     * @inheritDoc
     */
    public String toString() {
        if (transform == TRANSFORM_NONE) {
            return base.subSequence(offset, base.length()).toString();
        } else {
            int len = length();
            StringBuffer buf = new StringBuffer(len);
            for (int i = 0; i < len; i++) {
                buf.append(charAt(i));
            }
            return buf.toString();
        }
    }

    //-----------------------------< Comparable >-------------------------------

    /**
     * Compares this char sequence to another char sequence <code>o</code>.
     *
     * @param o the other char sequence.
     * @return as defined in {@link String#compareTo(Object)} but also takes
     *         {@link #transform} into account.
     */
    public int compareTo(OffsetCharSequence other) {
        int len1 = length();
        int len2 = other.length();
        int lim = Math.min(len1, len2);

        for (int i = 0; i  < lim; i++) {
            char c1 = charAt(i);
            char c2 = other.charAt(i);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }
}
