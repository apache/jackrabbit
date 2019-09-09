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
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.Collections;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the global jackrabbit lucene analyzer. By default, all
 * properties are indexed with the <code>StandardAnalyzer(new String[]{})</code>,
 * unless in the &lt;SearchIndex&gt; configuration a global analyzer is defined.
 *
 * In the indexing configuration, properties can be configured to be
 * indexed with a specific analyzer. If configured, this analyzer is used to
 * index the text of the property and to parse searchtext for this property.
 */
public class JackrabbitAnalyzer extends Analyzer {

    private static Logger log =
            LoggerFactory.getLogger(JackrabbitAnalyzer.class);

    private static final Analyzer DEFAULT_ANALYZER = new ClassicAnalyzer(
            Version.LUCENE_36, Collections.emptySet());

    /**
     * Returns a new instance of the named Lucene {@link Analyzer} class,
     * or the default analyzer if the given class can not be instantiated.
     *
     * @param className name of the analyzer class
     * @return new analyzer instance, or the default analyzer
     */
    static Analyzer getAnalyzerInstance(String className) {
        Class<?> analyzerClass;
        try {
            analyzerClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.warn(className + " could not be found", e);
            return DEFAULT_ANALYZER;
        }
        if (!Analyzer.class.isAssignableFrom(analyzerClass)) {
            log.warn(className + " is not a Lucene Analyzer");
            return DEFAULT_ANALYZER;
        } else if (JackrabbitAnalyzer.class.isAssignableFrom(analyzerClass)) {
            log.warn(className + " can not be used as a JackrabbitAnalyzer component");
            return DEFAULT_ANALYZER;
        }

        Exception cause = null;
        Constructor<?>[] constructors = analyzerClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length == 1 && types[0] == Version.class) {
                try {
                    return (Analyzer) constructor.newInstance(Version.LUCENE_36);
                } catch (Exception e) {
                    cause = e;
                }
            }
        }
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterTypes().length == 0) {
                try {
                    return (Analyzer) constructor.newInstance();
                } catch (Exception e) {
                    cause = e;
                }
            }
        }

        log.warn(className + " could not be instantiated", cause);
        return DEFAULT_ANALYZER;
    }

    /**
     * The default Jackrabbit analyzer if none is configured in
     * <code>&lt;SearchIndex&gt;</code> configuration.
     */
    private Analyzer defaultAnalyzer = DEFAULT_ANALYZER;

    /**
     * The indexing configuration.
     */
    private IndexingConfiguration indexingConfig;

    /**
     * A param indexingConfig the indexing configuration.
     */
    protected void setIndexingConfig(IndexingConfiguration indexingConfig) {
        this.indexingConfig = indexingConfig;
    }

    /**
     * @param analyzer the default jackrabbit analyzer
     */
    protected void setDefaultAnalyzer(Analyzer analyzer) {
        defaultAnalyzer = analyzer;
    }

    String getDefaultAnalyzerClass() {
        return defaultAnalyzer.getClass().getName();
    }

    void setDefaultAnalyzerClass(String className) {
        setDefaultAnalyzer(getAnalyzerInstance(className));
    }

    /**
     * Creates a TokenStream which tokenizes all the text in the provided
     * Reader. If the fieldName (property) is configured to have a different
     * analyzer than the default, this analyzer is used for tokenization
     */
    public final TokenStream tokenStream(String fieldName, Reader reader) {
        if (indexingConfig != null) {
            Analyzer propertyAnalyzer = indexingConfig.getPropertyAnalyzer(fieldName);
            if (propertyAnalyzer != null) {
                return propertyAnalyzer.tokenStream(fieldName, reader);
            }
        }
        return defaultAnalyzer.tokenStream(fieldName, reader);
    }

    @Override
    public final TokenStream reusableTokenStream(String fieldName, Reader reader)
            throws IOException {
        if (indexingConfig != null) {
            Analyzer propertyAnalyzer = indexingConfig.getPropertyAnalyzer(fieldName);
            if (propertyAnalyzer != null) {
                return propertyAnalyzer.reusableTokenStream(fieldName, reader);
            }
        }
        return defaultAnalyzer.reusableTokenStream(fieldName, reader);
    }
}
