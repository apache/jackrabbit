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
package org.apache.jackrabbit.commons.query;

import java.util.List;
import java.util.ServiceLoader;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Collections;

import javax.jcr.query.InvalidQueryException;

/**
 * Implements a central access to QueryObjectModelBuilder instances.
 */
public class QueryObjectModelBuilderRegistry {

    /**
     * List of <code>QueryObjectModelBuilder</code> instances known to the classloader.
     */
    private static final List<QueryObjectModelBuilder> BUILDERS = new ArrayList<QueryObjectModelBuilder>();

    /**
     * Set of languages known to the registered builders.
     */
    private static final Set<String> LANGUAGES;

    static {
        Set<String> languages = new HashSet<String>();
        Iterator<QueryObjectModelBuilder> it = ServiceLoader.load(QueryObjectModelBuilder.class,
                QueryObjectModelBuilder.class.getClassLoader()).iterator();
        while (it.hasNext()) {
            QueryObjectModelBuilder builder = it.next();
            BUILDERS.add(builder);
            languages.addAll(Arrays.asList(builder.getSupportedLanguages()));
        }
        LANGUAGES = Collections.unmodifiableSet(languages);
    }

    /**
     * Returns the <code>QueryObjectModelBuilder</code> for
     * <code>language</code>.
     *
     * @param language the language of the query statement.
     * @return the <code>QueryObjectModelBuilder</code> for
     *         <code>language</code>.
     * @throws InvalidQueryException if there is no query object model builder
     *                               for <code>language</code>.
     */
    public static QueryObjectModelBuilder getQueryObjectModelBuilder(String language)
            throws InvalidQueryException {
        for (QueryObjectModelBuilder builder : BUILDERS) {
            if (builder.canHandle(language)) {
                return builder;
            }
        }
        throw new InvalidQueryException("Unsupported language: " + language);
    }

    /**
     * Returns the set of query languages supported by all registered
     * {@link QueryObjectModelBuilder} implementations.
     *
     * @return String array containing the names of the supported languages.
     */
    public static String[] getSupportedLanguages() {
        return LANGUAGES.toArray(new String[LANGUAGES.size()]);
    }
}
