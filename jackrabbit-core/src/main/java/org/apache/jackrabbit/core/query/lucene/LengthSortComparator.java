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

import org.apache.jackrabbit.core.query.lucene.SharedFieldComparatorSource.SimpleFieldComparator;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

import java.io.IOException;

/**
 * <code>LengthSortComparator</code> implements a <code>FieldComparator</code> which
 * sorts on the length of property values.
 */
public class LengthSortComparator extends FieldComparatorSource {

    /**
     * The index internal namespace mappings.
     */
    private final NamespaceMappings nsMappings;

    public LengthSortComparator(NamespaceMappings nsMappings) {
        this.nsMappings = nsMappings;
    }

    @Override
    public FieldComparator newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException {
        NameFactory factory = NameFactoryImpl.getInstance();
        try {
            return new SimpleFieldComparator(nsMappings.translateName(factory.create(fieldname)), FieldNames.PROPERTY_LENGTHS, numHits);
        }
        catch (IllegalNameException e) {
            throw Util.createIOException(e);
        }
    }

}
