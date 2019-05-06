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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.cluster.ChangeLogRecord;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.cluster.ClusterRecord;
import org.apache.jackrabbit.core.cluster.ClusterRecordDeserializer;
import org.apache.jackrabbit.core.cluster.ClusterRecordProcessor;
import org.apache.jackrabbit.core.cluster.LockRecord;
import org.apache.jackrabbit.core.cluster.NamespaceRecord;
import org.apache.jackrabbit.core.cluster.NodeTypeRecord;
import org.apache.jackrabbit.core.cluster.PrivilegeRecord;
import org.apache.jackrabbit.core.cluster.WorkspaceRecord;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.journal.RecordIterator;
import org.apache.jackrabbit.core.query.AbstractQueryHandler;
import org.apache.jackrabbit.core.query.ExecutableQuery;
import org.apache.jackrabbit.core.query.QueryHandler;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;
import org.apache.jackrabbit.core.query.lucene.directory.FSDirectoryManager;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.DefaultQueryNodeFactory;
import org.apache.jackrabbit.spi.commons.query.qom.OrderingImpl;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LimitTokenCountAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Payload;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.fork.ForkParser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Implements a {@link org.apache.jackrabbit.core.query.QueryHandler} using
 * Lucene.
 */
public class SearchIndex extends AbstractQueryHandler {

    /**
     * Valid node type names under /jcr:system. Used to determine if a
     * query needs to be executed also against the /jcr:system tree.
     */
    public static final Collection<Name> VALID_SYSTEM_INDEX_NODE_TYPE_NAMES =
        Collections.unmodifiableCollection(Arrays.asList(
                NameConstants.NT_CHILDNODEDEFINITION,
                NameConstants.NT_FROZENNODE,
                NameConstants.NT_NODETYPE,
                NameConstants.NT_PROPERTYDEFINITION,
                NameConstants.NT_VERSION,
                NameConstants.NT_VERSIONEDCHILD,
                NameConstants.NT_VERSIONHISTORY,
                NameConstants.NT_VERSIONLABELS,
                NameConstants.REP_NODETYPES,
                NameConstants.REP_SYSTEM,
                NameConstants.REP_VERSIONSTORAGE,
                // Supertypes
                NameConstants.NT_BASE,
                NameConstants.MIX_REFERENCEABLE));
        
    /**
     * Default query node factory.
     */
    private static final DefaultQueryNodeFactory DEFAULT_QUERY_NODE_FACTORY =
        new DefaultQueryNodeFactory(VALID_SYSTEM_INDEX_NODE_TYPE_NAMES);

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(SearchIndex.class);

    /**
     * Name of the file to persist search internal namespace mappings.
     */
    private static final String NS_MAPPING_FILE = "ns_mappings.properties";

    /**
     * The default value for property {@link #minMergeDocs}.
     */
    public static final int DEFAULT_MIN_MERGE_DOCS = 100;

    /**
     * The default value for property {@link #maxMergeDocs}.
     */
    public static final int DEFAULT_MAX_MERGE_DOCS = Integer.MAX_VALUE;

    /**
     * the default value for property {@link #mergeFactor}.
     */
    public static final int DEFAULT_MERGE_FACTOR = 10;

    /**
     * the default value for property {@link #maxFieldLength}.
     */
    public static final int DEFAULT_MAX_FIELD_LENGTH = 10000;

    /**
     * The default value for property {@link #extractorPoolSize}.
     * @deprecated this value is not used anymore. Instead the default value
     * is calculated as follows: 2 * Runtime.getRuntime().availableProcessors().
     */
    public static final int DEFAULT_EXTRACTOR_POOL_SIZE = 0;

    /**
     * The default value for property {@link #extractorBackLog}.
     */
    public static final int DEFAULT_EXTRACTOR_BACK_LOG = Integer.MAX_VALUE;

    /**
     * The default timeout in milliseconds which is granted to the text
     * extraction process until fulltext indexing is deferred to a background
     * thread.
     */
    public static final long DEFAULT_EXTRACTOR_TIMEOUT = 100;

    /**
     * The default value for {@link #termInfosIndexDivisor}.
     */
    public static final int DEFAULT_TERM_INFOS_INDEX_DIVISOR = 1;

    /**
     * The path factory.
     */
    protected static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();

    /**
     * The path of the root node.
     */
    protected static final Path ROOT_PATH;

    /**
     * The path <code>/jcr:system</code>.
     */
    protected static final Path JCR_SYSTEM_PATH;

    static {
        ROOT_PATH = PATH_FACTORY.create(NameConstants.ROOT);
        try {
            JCR_SYSTEM_PATH = PATH_FACTORY.create(ROOT_PATH, NameConstants.JCR_SYSTEM, false);
        } catch (RepositoryException e) {
            // should never happen, path is always valid
            throw new InternalError(e.getMessage());
        }
    }

    /**
     * The actual index
     */
    protected MultiIndex index;

    /**
     * The analyzer we use for indexing.
     */
    private final JackrabbitAnalyzer analyzer = new JackrabbitAnalyzer();

    /**
     * Path of the Tika configuration file used for text extraction.
     */
    private String tikaConfigPath = null;

    /**
     * Java command used to fork external parser processes,
     * or <code>null</code> (the default) for in-process text extraction.
     */
    private String forkJavaCommand = null;

    /**
     * The Tika parser for extracting text content from binary properties.
     * Initialized by the {@link #getParser()} method during first access.
     */
    private Parser parser = null;

    /**
     * The namespace mappings used internally.
     */
    private NamespaceMappings nsMappings;

    /**
     * The location of the search index.
     * <p>
     * Note: This is a <b>mandatory</b> parameter!
     */
    private String path;

    /**
     * minMergeDocs config parameter.
     */
    private int minMergeDocs = DEFAULT_MIN_MERGE_DOCS;

    /**
     * The maximum volatile index size in bytes until it is written to disk.
     * The default value is 1048576 (1MB).
     */
    private long maxVolatileIndexSize = 1024 * 1024;

    /**
     * volatileIdleTime config parameter.
     */
    private int volatileIdleTime = 3;

    /**
     * The maximum age (in seconds) of the index history. The default value is
     * zero. Which means, index commits are deleted as soon as they are not used
     * anymore.
     */
    private long maxHistoryAge = 0;

    /**
     * maxMergeDocs config parameter
     */
    private int maxMergeDocs = DEFAULT_MAX_MERGE_DOCS;

    /**
     * mergeFactor config parameter
     */
    private int mergeFactor = DEFAULT_MERGE_FACTOR;

    /**
     * maxFieldLength config parameter
     */
    private int maxFieldLength = DEFAULT_MAX_FIELD_LENGTH;

    /**
     * maxExtractLength config parameter. Positive values are used as-is,
     * negative values are interpreted as factors of the maxFieldLength
     * parameter.
     */
    private int maxExtractLength = -10;

    /**
     * extractorPoolSize config parameter
     */
    private int extractorPoolSize = 2 * Runtime.getRuntime().availableProcessors();

    /**
     * extractorBackLog config parameter
     */
    private int extractorBackLog = DEFAULT_EXTRACTOR_BACK_LOG;

    /**
     * extractorTimeout config parameter
     */
    private long extractorTimeout = DEFAULT_EXTRACTOR_TIMEOUT;

    /**
     * Number of documents that are buffered before they are added to the index.
     */
    private int bufferSize = 10;

    /**
     * Compound file flag
     */
    private boolean useCompoundFile = true;

    /**
     * Flag indicating whether document order is enabled as the default
     * ordering.
     * <p>
     * Default value is: <code>false</code>.
     */
    private boolean documentOrder = false;

    /**
     * If set <code>true</code> the index is checked for consistency on startup.
     * If <code>false</code> a consistency check is only performed when there
     * are entries in the redo log on startup.
     * <p>
     * Default value is: <code>false</code>.
     */
    private boolean forceConsistencyCheck = false;

    /**
     * If set <code>true</code> the index is checked for consistency depending
     * on the {@link #forceConsistencyCheck} parameter. If set to
     * <code>false</code>, no consistency check is performed, even if the redo
     * log had been applied on startup.
     * <p>
     * Default value is: <code>false</code>.
     */
    private boolean consistencyCheckEnabled = false;

    /**
     * If set <code>true</code> errors detected by the consistency check are
     * repaired. If <code>false</code> the errors are only reported in the log.
     * <p>
     * Default value is: <code>true</code>.
     */
    private boolean autoRepair = true;

    /**
     * The id resolver cache size.
     * <p>
     * Default value is: <code>1000</code>.
     */
    private int cacheSize = 1000;

    /**
     * The number of documents that are pre fetched when a query is executed.
     * <p>
     * Default value is: {@link Integer#MAX_VALUE}.
     */
    private int resultFetchSize = Integer.MAX_VALUE;

    /**
     * If set to <code>true</code> the fulltext field is stored and and a term
     * vector is created with offset information.
     * <p>
     * Default value is: <code>false</code>.
     */
    private boolean supportHighlighting = false;
    
    /**
     * If enabled, NodeIterator.getSize() may report a larger value than the
     * actual result. This value may shrink when the query result encounters
     * non-existing nodes or the session does not have access to a node. This
     * might be a security problem.
     */
    private boolean sizeEstimate = false;

    /**
     * The excerpt provider class. Implements {@link ExcerptProvider}.
     */
    private Class<?> excerptProviderClass = DefaultHTMLExcerpt.class;

    /**
     * The path to the indexing configuration file (can be an absolute path to a
     * file or a classpath resource).
     */
    private String indexingConfigPath;

    /**
     * The DOM with the indexing configuration or <code>null</code> if there
     * is no such configuration.
     */
    private Element indexingConfiguration;

    /**
     * The indexing configuration.
     */
    private IndexingConfiguration indexingConfig;

    /**
     * The indexing configuration class.
     * Implements {@link IndexingConfiguration}.
     */
    private Class<?> indexingConfigurationClass = IndexingConfigurationImpl.class;

    /**
     * The class that implements {@link SynonymProvider}.
     */
    private Class<?> synonymProviderClass;

    /**
     * The currently set synonym provider.
     */
    private SynonymProvider synProvider;

    /**
     * The configuration path for the synonym provider.
     */
    private String synonymProviderConfigPath;

    /**
     * The FileSystem for the synonym if the query handler context does not
     * provide one.
     */
    private FileSystem synonymProviderConfigFs;

    /**
     * Indicates the index format version which is relevant to a <b>query</b>. This
     * value may be different from what {@link MultiIndex#getIndexFormatVersion()}
     * returns because queries may be executed on two physical indexes with
     * different formats. Index format versions are considered backward
     * compatible. That is, the lower version of the two physical indexes is
     * used for querying.
     */
    private IndexFormatVersion indexFormatVersion;

    /**
     * The class that implements {@link SpellChecker}.
     */
    private Class<?> spellCheckerClass;

    /**
     * The spell checker for this query handler or <code>null</code> if none is
     * configured.
     */
    private SpellChecker spellChecker;

    /**
     * The similarity in use for indexing and searching.
     */
    private Similarity similarity = Similarity.getDefault();

    /**
     * The name of the directory manager class implementation.
     */
    private String directoryManagerClass = FSDirectoryManager.class.getName();

    /**
     * The directory manager.
     */
    private DirectoryManager directoryManager;

    /**
     * Flag that indicates whether the {@link DirectoryManager} should
     * use the <code>SimpleFSDirectory</code> instead of letting Lucene
     * automatically pick an implementation based on the platform we are
     * running on. Note: see JCR-3818 for a discussion on the trade-off.
     */
    private boolean useSimpleFSDirectory = true;

    /**
     * The termInfosIndexDivisor.
     */
    private int termInfosIndexDivisor = DEFAULT_TERM_INFOS_INDEX_DIVISOR;

    /**
     * The field comparator source for indexed properties.
     */
    private SharedFieldComparatorSource scs;

    /**
     * Flag that indicates whether the hierarchy cache should be initialized
     * immediately on startup.
     */
    private boolean initializeHierarchyCache = true;

    /**
     * The name of the redo log factory class implementation.
     */
    private String redoLogFactoryClass = DefaultRedoLogFactory.class.getName();

    /**
     * The redo log factory.
     */
    private RedoLogFactory redoLogFactory;

    /**
     * Indicates if this <code>SearchIndex</code> is closed and cannot be used
     * anymore.
     */
    private boolean closed = false;

    /**
     * Initializes this <code>QueryHandler</code>. This implementation requires
     * that a path parameter is set in the configuration. If this condition
     * is not met, a <code>IOException</code> is thrown.
     *
     * @throws IOException if an error occurs while initializing this handler.
     */
    protected void doInit() throws IOException {
        QueryHandlerContext context = getContext();
        if (path == null) {
            throw new IOException("SearchIndex requires 'path' parameter in configuration!");
        }

        Set<NodeId> excludedIDs = new HashSet<NodeId>();
        if (context.getExcludedNodeId() != null) {
            excludedIDs.add(context.getExcludedNodeId());
        }

        synProvider = createSynonymProvider();
        directoryManager = createDirectoryManager();
        redoLogFactory = createRedoLogFactory();

        if (context.getParentHandler() instanceof SearchIndex) {
            // use system namespace mappings
            SearchIndex sysIndex = (SearchIndex) context.getParentHandler();
            nsMappings = sysIndex.getNamespaceMappings();
        } else {
            // read local namespace mappings
            File mapFile = new File(new File(path), NS_MAPPING_FILE);
            if (mapFile.exists()) {
                // be backward compatible and use ns_mappings.properties from
                // index folder
                nsMappings = new FileBasedNamespaceMappings(mapFile);
            } else {
                // otherwise use repository wide stable index prefix from
                // namespace registry
                nsMappings = new NSRegistryBasedNamespaceMappings(
                        context.getNamespaceRegistry());
            }
        }

        scs = new SharedFieldComparatorSource(
                FieldNames.PROPERTIES, context.getItemStateManager(),
                context.getHierarchyManager(), nsMappings);
        indexingConfig = createIndexingConfiguration(nsMappings);
        analyzer.setIndexingConfig(indexingConfig);

        // initialize the Tika parser
        parser = createParser();

        index = new MultiIndex(this, excludedIDs);
        if (index.numDocs() == 0) {
            Path rootPath;
            if (excludedIDs.isEmpty()) {
                // this is the index for jcr:system
                rootPath = JCR_SYSTEM_PATH;
            } else {
                rootPath = ROOT_PATH;
            }
            index.createInitialIndex(context.getItemStateManager(),
                    context.getRootId(), rootPath);
            checkPendingJournalChanges(context);
        }
        if (consistencyCheckEnabled
                && (index.getRedoLogApplied() || forceConsistencyCheck)) {
            log.info("Running consistency check...");
            try {
                ConsistencyCheck check = runConsistencyCheck();
                if (autoRepair) {
                    check.repair(true);
                } else {
                    List<ConsistencyCheckError> errors = check.getErrors();
                    if (errors.size() == 0) {
                        log.info("No errors detected.");
                    }
                    for (ConsistencyCheckError err : errors) {
                        log.info(err.toString());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to run consistency check on index: " + e);
            }
        }

        // initialize spell checker
        spellChecker = createSpellChecker();

        log.info("Index initialized: {} Version: {}",
                new Object[]{path, index.getIndexFormatVersion()});
        if (!index.getIndexFormatVersion().equals(getIndexFormatVersion())) {
            log.warn("Using Version {} for reading. Please re-index version " +
                    "storage for optimal performance.",
                    getIndexFormatVersion().getVersion());
        }
    }

    /**
     * Adds the <code>node</code> to the search index.
     * @param node the node to add.
     * @throws RepositoryException if an error occurs while indexing the node.
     * @throws IOException if an error occurs while adding the node to the index.
     */
    public void addNode(NodeState node) throws RepositoryException, IOException {
        throw new UnsupportedOperationException("addNode");
    }

    /**
     * Removes the node with <code>id</code> from the search index.
     * @param id the id of the node to remove from the index.
     * @throws IOException if an error occurs while removing the node from
     * the index.
     */
    public void deleteNode(NodeId id) throws IOException {
        throw new UnsupportedOperationException("deleteNode");
    }

    /**
     * This implementation forwards the call to
     * {@link MultiIndex#update(Collection, Collection)} and
     * transforms the two iterators to the required types.
     *
     * @param remove ids of nodes to remove.
     * @param add    NodeStates to add. Calls to <code>next()</code> on this
     *               iterator may return <code>null</code>, to indicate that a
     *               node could not be indexed successfully.
     * @throws RepositoryException if an error occurs while indexing a node.
     * @throws IOException         if an error occurs while updating the index.
     */
    public void updateNodes(Iterator<NodeId> remove, Iterator<NodeState> add)
            throws RepositoryException, IOException {
        checkOpen();

        Map<NodeId, NodeState> aggregateRoots = new HashMap<NodeId, NodeState>();
        Set<NodeId> removedIds = new HashSet<NodeId>();
        Set<NodeId> addedIds = new HashSet<NodeId>();

        long time = System.currentTimeMillis();
        Collection<NodeId> removeCollection = new ArrayList<NodeId>();
        while (remove.hasNext()) {
            NodeId id = remove.next();
            removeCollection.add(id);
            removedIds.add(id);
        }
        
        Collection<Document> addCollection = new ArrayList<Document>();
        while (add.hasNext()) {
            NodeState state = add.next();
            if (state != null) {
                NodeId id = state.getNodeId();
                addedIds.add(id);
                retrieveAggregateRoot(state, aggregateRoots);

                try {
                    addCollection.add(createDocument(
                            state, getNamespaceMappings(),
                            index.getIndexFormatVersion()));
                } catch (RepositoryException e) {
                    log.warn("Exception while creating document for node: "
                            + state.getNodeId() + ": " + e.toString());
                }
            }
        }
        time = System.currentTimeMillis() - time;
        log.debug("created the removeCollection {} and addCollection {} in {}ms", new Object[] {removeCollection.size(), addCollection.size(), time});

        index.update(removeCollection, addCollection);

        // remove any aggregateRoot nodes that are new
        // and therefore already up-to-date
        aggregateRoots.keySet().removeAll(addedIds);

        // based on removed ids get affected aggregate root nodes
        retrieveAggregateRoot(removedIds, aggregateRoots);

        // update aggregates if there are any affected
        if (!aggregateRoots.isEmpty()) {
            Collection<Document> modified =
                new ArrayList<Document>(aggregateRoots.size());

            for (NodeState state : aggregateRoots.values()) {
                try {
                    modified.add(createDocument(
                            state, getNamespaceMappings(),
                            index.getIndexFormatVersion()));
                } catch (RepositoryException e) {
                    log.warn("Exception while creating document for node: "
                            + state.getNodeId(), e);
                }
            }

            index.update(aggregateRoots.keySet(), modified);
        }
    }

    /**
     * Creates a new query by specifying the query statement itself and the
     * language in which the query is stated.  If the query statement is
     * syntactically invalid, given the language specified, an
     * InvalidQueryException is thrown. <code>language</code> must specify a query language
     * string from among those returned by QueryManager.getSupportedQueryLanguages(); if it is not
     * then an <code>InvalidQueryException</code> is thrown.
     *
     * @param sessionContext component context of the current session
     * @param statement the query statement.
     * @param language the syntax of the query statement.
     * @throws InvalidQueryException if statement is invalid or language is unsupported.
     * @return A <code>Query</code> object.
     */
    public ExecutableQuery createExecutableQuery(
            SessionContext sessionContext, String statement, String language)
            throws InvalidQueryException {
        QueryImpl query = new QueryImpl(
                sessionContext, this, getContext().getPropertyTypeRegistry(),
                statement, language, getQueryNodeFactory());
        query.setRespectDocumentOrder(documentOrder);
        return query;
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<NodeId> getWeaklyReferringNodes(NodeId id)
            throws RepositoryException, IOException {
        final List<Integer> docs = new ArrayList<Integer>();
        final List<NodeId> ids = new ArrayList<NodeId>();
        final IndexReader reader = getIndexReader();
        try {
            IndexSearcher searcher = new IndexSearcher(reader);
            try {
                Query q = new TermQuery(new Term(
                        FieldNames.WEAK_REFS, id.toString()));
                searcher.search(q, new AbstractHitCollector() {
                    @Override
                    public void collect(int doc, float score) {
                        docs.add(doc);
                    }
                });
            } finally {
                searcher.close();
            }
            for (Integer doc : docs) {
                Document d = reader.document(doc, FieldSelectors.UUID);
                ids.add(new NodeId(d.get(FieldNames.UUID)));
            }
        } finally {
            Util.closeOrRelease(reader);
        }
        return ids;
    }

    List<Document> getNodeDocuments(NodeId id) throws RepositoryException, IOException {
        final List<Integer> docIds = new ArrayList<Integer>(1);
        final List<Document> docs = new ArrayList<Document>();
        final IndexReader reader = getIndexReader();
        try {
            IndexSearcher searcher = new IndexSearcher(reader);
            try {
                Query q = new TermQuery(new Term(FieldNames.UUID, id.toString()));
                searcher.search(q, new AbstractHitCollector() {
                    @Override
                    protected void collect(final int doc, final float score) {
                        docIds.add(doc);
                    }
                });
                for (Integer docId : docIds) {
                    docs.add(reader.document(docId, FieldSelectors.UUID_AND_PARENT));
                }
            } finally {
                searcher.close();
            }
        } finally {
            Util.closeOrRelease(reader);
        }
        return docs;
    }

    /**
     * This method returns the QueryNodeFactory used to parse Queries. This method
     * may be overridden to provide a customized QueryNodeFactory
     *
     * @return the query node factory.
     */
    protected DefaultQueryNodeFactory getQueryNodeFactory() {
        return DEFAULT_QUERY_NODE_FACTORY;
    }

    /**
     * Waits until all pending text extraction tasks have been processed
     * and the updated index has been flushed to disk.
     *
     * @throws RepositoryException if the index update can not be written
     */
    public void flush() throws RepositoryException {
        try {
            index.waitUntilIndexingQueueIsEmpty();
            index.safeFlush();
            // flush may have pushed nodes into the indexing queue
            // -> wait again
            index.waitUntilIndexingQueueIsEmpty();
        } catch (IOException e) {
            throw new RepositoryException("Failed to flush the index", e);
        }
    }

    /**
     * Closes this <code>QueryHandler</code> and frees resources attached
     * to this handler.
     */
    public void close() throws IOException {
        if (synonymProviderConfigFs != null) {
            try {
                synonymProviderConfigFs.close();
            } catch (FileSystemException e) {
                log.warn("Exception while closing FileSystem", e);
            }
        }
        if (spellChecker != null) {
            spellChecker.close();
        }
        index.close();
        getContext().destroy();
        super.close();
        closed = true;
        log.info("Index closed: " + path);
    }

    /**
     * Executes the query on the search index.
     *
     * @param session         the session that executes the query.
     * @param queryImpl       the query impl.
     * @param query           the lucene query.
     * @param orderProps      name of the properties for sort order.
     * @param orderSpecs      the order specs for the sort order properties.
     *                        <code>true</code> indicates ascending order,
     *                        <code>false</code> indicates descending.
     * @param orderFuncs      functions for the properties for sort order. 
     * @param resultFetchHint a hint on how many results should be fetched.  @return the query hits.
     * @throws IOException if an error occurs while searching the index.
     */
    public MultiColumnQueryHits executeQuery(SessionImpl session,
                                             AbstractQueryImpl queryImpl,
                                             Query query,
                                             Path[] orderProps,
                                             boolean[] orderSpecs,
                                             String[] orderFuncs, long resultFetchHint)
            throws IOException {
        checkOpen();

        Sort sort = new Sort(createSortFields(orderProps, orderSpecs, orderFuncs));

        final IndexReader reader = getIndexReader(queryImpl.needsSystemTree());
        JackrabbitIndexSearcher searcher = new JackrabbitIndexSearcher(
                session, reader, getContext().getItemStateManager());
        searcher.setSimilarity(getSimilarity());
        return new FilterMultiColumnQueryHits(
                searcher.execute(query, sort, resultFetchHint,
                        QueryImpl.DEFAULT_SELECTOR_NAME)) {
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    Util.closeOrRelease(reader);
                }
            }
        };
    }

    /**
     * Executes the query on the search index.
     *
     * @param session         the session that executes the query.
     * @param query           the query.
     * @param orderings       the order specs for the sort order.
     * @param resultFetchHint a hint on how many results should be fetched.
     * @return the query hits.
     * @throws IOException if an error occurs while searching the index.
     */
    public MultiColumnQueryHits executeQuery(SessionImpl session,
                                             MultiColumnQuery query,
                                             Ordering[] orderings,
                                             long resultFetchHint)
            throws IOException {
        checkOpen();

        final IndexReader reader = getIndexReader();
        JackrabbitIndexSearcher searcher = new JackrabbitIndexSearcher(
                session, reader, getContext().getItemStateManager());
        searcher.setSimilarity(getSimilarity());
        return new FilterMultiColumnQueryHits(
                query.execute(searcher, orderings, resultFetchHint)) {
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    Util.closeOrRelease(reader);
                }
            }
        };
    }

    /**
     * Creates an excerpt provider for the given <code>query</code>.
     *
     * @param query the query.
     * @return an excerpt provider for the given <code>query</code>.
     * @throws IOException if the provider cannot be created.
     */
    public ExcerptProvider createExcerptProvider(Query query)
            throws IOException {
        ExcerptProvider ep;
        try {
            ep = (ExcerptProvider) excerptProviderClass.newInstance();
        } catch (Exception e) {
            throw Util.createIOException(e);
        }
        ep.init(query, this);
        return ep;
    }

    /**
     * Returns the analyzer in use for indexing.
     * @return the analyzer in use for indexing.
     */
    public Analyzer getTextAnalyzer() {
        return new LimitTokenCountAnalyzer(analyzer, getMaxFieldLength());
    }

    /**
     * Returns the path of the Tika configuration used for text extraction.
     *
     * @return path of the Tika configuration file
     */
    public String getTikaConfigPath() {
        return tikaConfigPath;
    }

    /**
     * Sets the path of the Tika configuration used for text extraction.
     * The path can be either a file system or a class resource path.
     * The default setting is the tika-config.xml class resource relative
     * to org.apache.core.query.lucene.
     *
     * @param tikaConfigPath path of the Tika configuration file
     */
    public void setTikaConfigPath(String tikaConfigPath) {
        this.tikaConfigPath = tikaConfigPath;
    }

    /**
     * Returns the java command used to fork external parser processes,
     * or <code>null</code> (the default) for in-process text extraction.
     *
     * @return fork java command
     */
    public String getForkJavaCommand() {
        return forkJavaCommand;
    }

    /**
     * Sets the java command used to fork external parser processes.
     *
     * @param command fork java command,
     *                or <code>null</code> for in-process extraction
     */
    public void setForkJavaCommand(String command) {
        this.forkJavaCommand = command;
    }

    /**
     * Returns the parser used for extracting text content
     * from binary properties for full text indexing.
     *
     * @return the configured parser
     */
    public Parser getParser() {
        return parser;
    }

    private Parser createParser() {
        URL url = null;
        if (tikaConfigPath != null) {
            File file = new File(tikaConfigPath);
            if (file.exists()) {
                try {
                    url = file.toURI().toURL();
                } catch (MalformedURLException e) {
                    log.warn("Invalid Tika configuration path: " + file, e);
                }
            } else {
                ClassLoader loader = SearchIndex.class.getClassLoader();
                url = loader.getResource(tikaConfigPath);
            }
        }
        if (url == null) {
            url = SearchIndex.class.getResource("tika-config.xml");
        }

        TikaConfig config = null;
        if (url != null) {
            try {
                config = new TikaConfig(url);
            } catch (Exception e) {
                log.warn("Tika configuration not available: " + url, e);
            }
        }
        if (config == null) {
            config = TikaConfig.getDefaultConfig();
        }

        if (forkJavaCommand != null) {
            ForkParser forkParser = new ForkParser(
                    SearchIndex.class.getClassLoader(),
                    new AutoDetectParser(config));
            forkParser.setJavaCommand(forkJavaCommand);
            forkParser.setPoolSize(extractorPoolSize);
            return forkParser;
        } else {
            return new AutoDetectParser(config);
        }
    }

    /**
     * Returns the namespace mappings for the internal representation.
     * @return the namespace mappings for the internal representation.
     */
    public NamespaceMappings getNamespaceMappings() {
        return nsMappings;
    }

    /**
     * @return the indexing configuration or <code>null</code> if there is
     *         none.
     */
    public IndexingConfiguration getIndexingConfig() {
        return indexingConfig;
    }

    /**
     * @return the synonym provider of this search index. If none is set for
     *         this search index the synonym provider of the parent handler is
     *         returned if there is any.
     */
    public SynonymProvider getSynonymProvider() {
        if (synProvider != null) {
            return synProvider;
        } else {
            QueryHandler handler = getContext().getParentHandler();
            if (handler instanceof SearchIndex) {
                return ((SearchIndex) handler).getSynonymProvider();
            } else {
                return null;
            }
        }
    }

    /**
     * @return the spell checker of this search index. If none is configured
     *         this method returns <code>null</code>.
     */
    public SpellChecker getSpellChecker() {
        return spellChecker;
    }

    /**
     * @return the similarity, which should be used for indexing and searching.
     */
    public Similarity getSimilarity() {
        return similarity;
    }

    /**
     * Returns an index reader for this search index. The caller of this method
     * is responsible for closing the index reader when he is finished using
     * it.
     *
     * @return an index reader for this search index.
     * @throws IOException the index reader cannot be obtained.
     */
    public IndexReader getIndexReader() throws IOException {
        return getIndexReader(true);
    }

    /**
     * Returns the index format version that this search index is able to
     * support when a query is executed on this index.
     *
     * @return the index format version for this search index.
     */
    public IndexFormatVersion getIndexFormatVersion() {
        if (indexFormatVersion == null) {
            if (getContext().getParentHandler() instanceof SearchIndex) {
                SearchIndex parent = (SearchIndex) getContext().getParentHandler();
                if (parent.getIndexFormatVersion().getVersion()
                        < index.getIndexFormatVersion().getVersion()) {
                    indexFormatVersion = parent.getIndexFormatVersion();
                } else {
                    indexFormatVersion = index.getIndexFormatVersion();
                }
            } else {
                indexFormatVersion = index.getIndexFormatVersion();
            }
        }
        return indexFormatVersion;
    }

    /**
     * @return the directory manager for this search index.
     */
    public DirectoryManager getDirectoryManager() {
        return directoryManager;
    }

    /**
     * @return the redo log factory for this search index.
     */
    public RedoLogFactory getRedoLogFactory() {
        return redoLogFactory;
    }

    /**
     * Runs a consistency check on this search index.
     *
     * @return the result of the consistency check.
     * @throws IOException if an error occurs while running the check.
     */
    public ConsistencyCheck runConsistencyCheck() throws IOException {
        return index.runConsistencyCheck();
    }

    /**
     * Returns an index reader for this search index. The caller of this method
     * is responsible for closing the index reader when he is finished using
     * it.
     *
     * @param includeSystemIndex if <code>true</code> the index reader will
     *                           cover the complete workspace. If
     *                           <code>false</code> the returned index reader
     *                           will not contains any nodes under /jcr:system.
     * @return an index reader for this search index.
     * @throws IOException the index reader cannot be obtained.
     */
    protected IndexReader getIndexReader(boolean includeSystemIndex)
            throws IOException {
        QueryHandler parentHandler = getContext().getParentHandler();
        CachingMultiIndexReader parentReader = null;
        if (parentHandler instanceof SearchIndex && includeSystemIndex) {
            parentReader = ((SearchIndex) parentHandler).index.getIndexReader();
        }

        IndexReader reader;
        if (parentReader != null) {
            CachingMultiIndexReader[] readers = {index.getIndexReader(), parentReader};
            reader = new CombinedIndexReader(readers);
        } else {
            reader = index.getIndexReader();
        }
        return new JackrabbitIndexReader(reader);
    }

    /**
     * Creates the SortFields for the order properties.
     *
     * @param orderProps the order properties.
     * @param orderSpecs the order specs for the properties.
     * @param orderFuncs the functions for the properties. 
     * @return an array of sort fields
     */
    protected SortField[] createSortFields(Path[] orderProps,
                                           boolean[] orderSpecs, String[] orderFuncs) {
        List<SortField> sortFields = new ArrayList<SortField>();
        for (int i = 0; i < orderProps.length; i++) {
            if (orderProps[i].getLength() == 1
                    && NameConstants.JCR_SCORE.equals(orderProps[i].getName())) {
                // order on jcr:score does not use the natural order as
                // implemented in lucene. score ascending in lucene means that
                // higher scores are first. JCR specs that lower score values
                // are first.
                sortFields.add(new SortField(null, SortField.SCORE, orderSpecs[i]));
            } else {
                if ("upper-case".equals(orderFuncs[i])) {
                    sortFields.add(new SortField(orderProps[i].getString(), new UpperCaseSortComparator(scs), !orderSpecs[i]));
                } else if ("lower-case".equals(orderFuncs[i])) {
                    sortFields.add(new SortField(orderProps[i].getString(), new LowerCaseSortComparator(scs), !orderSpecs[i]));
                } else if ("normalize".equals(orderFuncs[i])) {
                    sortFields.add(new SortField(orderProps[i].getString(), new NormalizeSortComparator(scs), !orderSpecs[i]));
                } else {
                    sortFields.add(new SortField(orderProps[i].getString(), scs, !orderSpecs[i]));
                }
            }
        }
        return sortFields.toArray(new SortField[sortFields.size()]);
    }

    /**
     * Creates internal orderings for the QOM ordering specifications.
     *
     * @param orderings the QOM ordering specifications.
     * @return the internal orderings.
     * @throws RepositoryException if an error occurs.
     */
    protected Ordering[] createOrderings(OrderingImpl[] orderings)
            throws RepositoryException {
        Ordering[] ords = new Ordering[orderings.length];
        for (int i = 0; i < orderings.length; i++) {
            ords[i] = Ordering.fromQOM(orderings[i], scs, nsMappings);
        }
        return ords;
    }

    /**
     * Creates a lucene <code>Document</code> for a node state using the
     * namespace mappings <code>nsMappings</code>.
     *
     * @param node               the node state to index.
     * @param nsMappings         the namespace mappings of the search index.
     * @param indexFormatVersion the index format version that should be used to
     *                           index the passed node state.
     * @return a lucene <code>Document</code> that contains all properties of
     *         <code>node</code>.
     * @throws RepositoryException if an error occurs while indexing the
     *                             <code>node</code>.
     */
    protected Document createDocument(NodeState node,
                                      NamespaceMappings nsMappings,
                                      IndexFormatVersion indexFormatVersion)
            throws RepositoryException {
        NodeIndexer indexer = new NodeIndexer(
                node, getContext().getItemStateManager(), nsMappings,
                getContext().getExecutor(), parser);
        indexer.setSupportHighlighting(supportHighlighting);
        indexer.setIndexingConfiguration(indexingConfig);
        indexer.setIndexFormatVersion(indexFormatVersion);
        indexer.setMaxExtractLength(getMaxExtractLength());
        Document doc = indexer.createDoc();
        mergeAggregatedNodeIndexes(node, doc, indexFormatVersion);
        return doc;
    }

    /**
     * Returns the actual index.
     *
     * @return the actual index.
     */
    protected MultiIndex getIndex() {
        return index;
    }

    /**
     * @return the field comparator source for this index.
     */
    protected SharedFieldComparatorSource getSortComparatorSource() {
        return scs;
    }

    /**
     * @param namespaceMappings The namespace mappings
     * @return the fulltext indexing configuration or <code>null</code> if there
     *         is no configuration.
     */
    protected IndexingConfiguration createIndexingConfiguration(NamespaceMappings namespaceMappings) {
        Element docElement = getIndexingConfigurationDOM();
        if (docElement == null) {
            return null;
        }
        try {
            IndexingConfiguration idxCfg = (IndexingConfiguration)
                    indexingConfigurationClass.newInstance();
            idxCfg.init(docElement, getContext(), namespaceMappings);
            return idxCfg;
        } catch (Exception e) {
            log.warn("Exception initializing indexing configuration from: "
                    + indexingConfigPath, e);
        }
        log.warn(indexingConfigPath + " ignored.");
        return null;
    }

    /**
     * @return the configured synonym provider or <code>null</code> if none is
     *         configured or an error occurs.
     */
    protected SynonymProvider createSynonymProvider() {
        SynonymProvider sp = null;
        if (synonymProviderClass != null) {
            try {
                sp = (SynonymProvider) synonymProviderClass.newInstance();
                sp.initialize(createSynonymProviderConfigResource());
            } catch (Exception e) {
                log.warn("Exception initializing synonym provider: "
                        + synonymProviderClass, e);
                sp = null;
            }
        }
        return sp;
    }

    /**
     * @return an initialized {@link DirectoryManager}.
     * @throws IOException if the directory manager cannot be instantiated or
     *          an exception occurs while initializing the manager.
     */
    protected DirectoryManager createDirectoryManager()
            throws IOException {
        try {
            Class<?> clazz = Class.forName(directoryManagerClass);
            if (!DirectoryManager.class.isAssignableFrom(clazz)) {
                throw new IOException(directoryManagerClass +
                        " is not a DirectoryManager implementation");
            }
            DirectoryManager df = (DirectoryManager) clazz.newInstance();
            df.init(this);
            return df;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
    }

    /**
     * Creates a redo log factory based on {@link #getRedoLogFactoryClass()}.
     *
     * @return the redo log factory.
     * @throws IOException if an error occurs while creating the factory.
     */
    protected RedoLogFactory createRedoLogFactory() throws IOException {
        try {
            Class<?> clazz = Class.forName(redoLogFactoryClass);
            if (!RedoLogFactory.class.isAssignableFrom(clazz)) {
                throw new IOException(redoLogFactoryClass +
                        " is not a RedoLogFactory implementation");
            }
            return (RedoLogFactory) clazz.newInstance();
        } catch (Exception e) {
            IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }
    }

    /**
     * Creates a file system resource to the synonym provider configuration.
     *
     * @return a file system resource or <code>null</code> if no path was
     *         configured.
     * @throws FileSystemException if an exception occurs accessing the file
     *                             system.
     * @throws IOException         if another exception occurs.
     */
    protected FileSystemResource createSynonymProviderConfigResource()
            throws FileSystemException, IOException {
        if (synonymProviderConfigPath != null) {
            FileSystemResource fsr;
            // simple sanity check
            if (synonymProviderConfigPath.endsWith(FileSystem.SEPARATOR)) {
                throw new FileSystemException(
                        "Invalid synonymProviderConfigPath: "
                        + synonymProviderConfigPath);
            }
            if (fs == null) {
                fs = new LocalFileSystem();
                int lastSeparator = synonymProviderConfigPath.lastIndexOf(
                        FileSystem.SEPARATOR_CHAR);
                if (lastSeparator != -1) {
                    File root = new File(path,
                            synonymProviderConfigPath.substring(0, lastSeparator));
                    ((LocalFileSystem) fs).setRoot(root.getCanonicalFile());
                    fs.init();
                    fsr = new FileSystemResource(fs,
                            synonymProviderConfigPath.substring(lastSeparator + 1));
                } else {
                    ((LocalFileSystem) fs).setPath(path);
                    fs.init();
                    fsr = new FileSystemResource(fs, synonymProviderConfigPath);
                }
                synonymProviderConfigFs = fs;
            } else {
                fsr = new FileSystemResource(fs, synonymProviderConfigPath);
            }
            return fsr;
        } else {
            // path not configured
            return null;
        }
    }

    /**
     * Creates a spell checker for this query handler.
     *
     * @return the spell checker or <code>null</code> if none is configured or
     *         an error occurs.
     */
    protected SpellChecker createSpellChecker() {
        SpellChecker spCheck = null;
        if (spellCheckerClass != null) {
            try {
                spCheck = (SpellChecker) spellCheckerClass.newInstance();
                spCheck.init(this);
            } catch (Exception e) {
                log.warn("Exception initializing spell checker: "
                        + spellCheckerClass, e);
            }
        }
        return spCheck;
    }

    /**
     * Returns the document element of the indexing configuration or
     * <code>null</code> if there is no indexing configuration.
     *
     * @return the indexing configuration or <code>null</code> if there is
     *         none.
     */
    protected Element getIndexingConfigurationDOM() {
        if (indexingConfiguration != null) {
            return indexingConfiguration;
        }
        if (indexingConfigPath == null) {
            return null;
        }
        File config = new File(indexingConfigPath);
        InputStream configStream = null;

        if (!config.exists()) {
            // check if it's a classpath resource
            configStream = getClass().getResourceAsStream(indexingConfigPath);

            if (configStream == null) {
                // only warn if not available also in the classpath
                log.warn("File does not exist: " + indexingConfigPath);
                return null;
            }
        } else if (!config.canRead()) {
            log.warn("Cannot read file: " + indexingConfigPath);
            return null;
        }
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(new IndexingConfigurationEntityResolver());

            if (configStream != null) {
                indexingConfiguration = builder
                    .parse(configStream).getDocumentElement();
            } else {
                indexingConfiguration = builder
                    .parse(config).getDocumentElement();
            }
        } catch (ParserConfigurationException e) {
            log.warn("Unable to create XML parser", e);
        } catch (IOException e) {
            log.warn("Exception parsing " + indexingConfigPath, e);
        } catch (SAXException e) {
            log.warn("Exception parsing " + indexingConfigPath, e);
        } finally {
            if (configStream != null) {
                try {
                    configStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return indexingConfiguration;
    }

    /**
     * Merges the fulltext indexed fields of the aggregated node states into
     * <code>doc</code>.
     *
     * @param state the node state on which <code>doc</code> was created.
     * @param doc the lucene document with index fields from <code>state</code>.
     * @param ifv the current index format version.
     */
    protected void mergeAggregatedNodeIndexes(NodeState state,
                                              Document doc,
                                              IndexFormatVersion ifv) {
        if (indexingConfig != null) {
            AggregateRule[] aggregateRules = indexingConfig.getAggregateRules();
            if (aggregateRules == null) {
                return;
            }
            try {
                ItemStateManager ism = getContext().getItemStateManager();
                for (AggregateRule aggregateRule : aggregateRules) {
                    boolean ruleMatched = false;
                    // node includes
                    NodeState[] aggregates = aggregateRule.getAggregatedNodeStates(state);
                    if (aggregates != null) {
                        ruleMatched = true;
                        for (NodeState aggregate : aggregates) {
                            Document aDoc = createDocument(aggregate, getNamespaceMappings(), ifv);
                            // transfer fields to doc if there are any
                            Fieldable[] fulltextFields = aDoc.getFieldables(FieldNames.FULLTEXT);
                            if (fulltextFields != null) {
                                for (Fieldable fulltextField : fulltextFields) {
                                    doc.add(fulltextField);
                                }
                                doc.add(new Field(
                                        FieldNames.AGGREGATED_NODE_UUID, false,
                                        aggregate.getNodeId().toString(),
                                        Field.Store.NO,
                                        Field.Index.NOT_ANALYZED_NO_NORMS,
                                        Field.TermVector.NO));
                            }
                        }
                        // make sure that fulltext fields are aligned properly
                        // first all stored fields, then remaining
                        Fieldable[] fulltextFields = doc
                                .getFieldables(FieldNames.FULLTEXT);
                        doc.removeFields(FieldNames.FULLTEXT);
                        Arrays.sort(fulltextFields, FIELDS_COMPARATOR_STORED);
                        for (Fieldable f : fulltextFields) {
                            doc.add(f);
                        }
                    }
                    // property includes
                    PropertyState[] propStates = aggregateRule.getAggregatedPropertyStates(state);
                    if (propStates != null) {
                        ruleMatched = true;
                        for (PropertyState propState : propStates) {
                            String namePrefix = FieldNames.createNamedValue(getNamespaceMappings().translateName(propState.getName()), "");
                            NodeState parent = (NodeState) ism.getItemState(propState.getParentId());
                            Document aDoc = createDocument(parent, getNamespaceMappings(), ifv);
                            try {
                                // find the right fields to transfer
                                Fieldable[] fields = aDoc.getFieldables(FieldNames.PROPERTIES);
                                for (Fieldable field : fields) {

                                    // assume properties fields use SingleTokenStream
                                    TokenStream tokenStream = field.tokenStreamValue();
                                    TermAttribute termAttribute = tokenStream.addAttribute(TermAttribute.class);
                                    PayloadAttribute payloadAttribute = tokenStream.addAttribute(PayloadAttribute.class);
                                    tokenStream.incrementToken();
                                    tokenStream.end();
                                    tokenStream.close();

                                    String value = new String(termAttribute.termBuffer(), 0, termAttribute.termLength());
                                    if (value.startsWith(namePrefix)) {
                                        // extract value
                                        String rawValue = value.substring(namePrefix.length());
                                        // create new named value
                                        Path p = getRelativePath(state, propState);
                                        String path = getNamespaceMappings().translatePath(p);
                                        value = FieldNames.createNamedValue(path, rawValue);
                                        termAttribute.setTermBuffer(value);
                                        PropertyMetaData pdm = PropertyMetaData
                                                .fromByteArray(payloadAttribute
                                                        .getPayload().getData());
                                        doc.add(new Field(field.name(),
                                                new SingletonTokenStream(value,
                                                        pdm.getPropertyType())));
                                        doc.add(new Field(
                                                FieldNames.AGGREGATED_NODE_UUID,
                                                false,
                                                parent.getNodeId().toString(),
                                                Field.Store.NO,
                                                Field.Index.NOT_ANALYZED_NO_NORMS,
                                                Field.TermVector.NO));
                                        if (pdm.getPropertyType() == PropertyType.STRING) {
                                            // add to fulltext index
                                            Field ft = new Field(
                                                    FieldNames.FULLTEXT,
                                                    false,
                                                    rawValue,
                                                    Field.Store.YES,
                                                    Field.Index.ANALYZED_NO_NORMS,
                                                    Field.TermVector.NO);
                                            doc.add(ft);
                                        }
                                    }
                                }
                            } finally {
                                Util.disposeDocument(aDoc);
                            }
                        }
                    }

                    // only use first aggregate definition that matches
                    if (ruleMatched) {
                        break;
                    }
                }
            } catch (NoSuchItemStateException e) {
                // do not fail if aggregate cannot be created
                log.info(
                        "Exception while building indexing aggregate for {}. Node is not available {}.",
                        state.getNodeId(), e.getMessage());
            } catch (Exception e) {
                // do not fail if aggregate cannot be created
                log.warn("Exception while building indexing aggregate for "
                        + state.getNodeId(), e);
            }
        }
    }

    private static final Comparator<Fieldable> FIELDS_COMPARATOR_STORED = new Comparator<Fieldable>() {
        public int compare(Fieldable o1, Fieldable o2) {
            return Boolean.valueOf(o2.isStored()).compareTo(o1.isStored());
        }
    };

    /**
     * Returns the relative path from <code>nodeState</code> to
     * <code>propState</code>.
     *
     * @param nodeState a node state.
     * @param propState a property state.
     * @return the relative path.
     * @throws RepositoryException if an error occurs while resolving paths.
     * @throws ItemStateException  if an error occurs while reading item
     *                             states.
     */
    protected Path getRelativePath(NodeState nodeState, PropertyState propState)
            throws RepositoryException, ItemStateException {
        HierarchyManager hmgr = getContext().getHierarchyManager();
        Path nodePath = hmgr.getPath(nodeState.getId());
        Path propPath = hmgr.getPath(propState.getId());
        Path p = nodePath.computeRelativePath(propPath);
        // make sure it does not contain indexes
        boolean clean = true;
        Path.Element[] elements = p.getElements();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].getIndex() != 0) {
                elements[i] = PATH_FACTORY.createElement(elements[i].getName());
                clean = false;
            }
        }
        if (!clean) {
            p = PATH_FACTORY.create(elements);
        }
        return p.getNormalizedPath();
    }

    /**
     * Retrieves the root of the indexing aggregate for <code>state</code> and
     * puts it into <code>aggregates</code>  map.
     *
     * @param state the node state for which we want to retrieve the aggregate
     *              root.
     * @param aggregates aggregate roots are collected in this map.
     */
    protected void retrieveAggregateRoot(NodeState state,
            Map<NodeId, NodeState> aggregates) {
        retrieveAggregateRoot(state, aggregates, state.getNodeId().toString(), 0);
    }
    
    /**
     * Retrieves the root of the indexing aggregate for <code>state</code> and
     * puts it into <code>aggregates</code> map.
     * 
     * @param state
     *            the node state for which we want to retrieve the aggregate
     *            root.
     * @param aggregates
     *            aggregate roots are collected in this map.
     * @param originNodeId
     *            the originating node, used for reporting only
     * @param level
     *            current aggregation level, used to limit recursive aggregation
     *            of nodes that have the same type
     */
    private void retrieveAggregateRoot(NodeState state,
            Map<NodeId, NodeState> aggregates, String originNodeId, long level) {
        if (indexingConfig == null) {
            return;
        }
        AggregateRule[] aggregateRules = indexingConfig.getAggregateRules();
        if (aggregateRules == null) {
            return;
        }
        for (AggregateRule aggregateRule : aggregateRules) {
            NodeState root = null;
            try {
                root = aggregateRule.getAggregateRoot(state);
            } catch (Exception e) {
                log.warn("Unable to get aggregate root for " + state.getNodeId(), e);
            }
            if (root == null) {
                continue;
            }
            if (root.getNodeTypeName().equals(state.getNodeTypeName())) {
                level++;
            } else {
                level = 0;
            }

            // JCR-2989 Support for embedded index aggregates
            if ((aggregateRule.getRecursiveAggregationLimit() == 0)
                    || (aggregateRule.getRecursiveAggregationLimit() != 0 && level <= aggregateRule
                            .getRecursiveAggregationLimit())) {

                // check if the update parent is already in the
                // map, then all its parents are already there so I can
                // skip this update subtree
                if (aggregates.put(root.getNodeId(), root) == null) {
                    retrieveAggregateRoot(root, aggregates, originNodeId, level);
                }
            } else {
                log.warn(
                        "Reached {} levels of recursive aggregation for nodeId {}, type {}, will stop at nodeId {}. Are you sure this did not occur by mistake? Please check the indexing-configuration.xml.",
                        new Object[] { level, originNodeId,
                                root.getNodeTypeName(), root.getNodeId() });
            }
        }
    }

    /**
     * Retrieves the root of the indexing aggregate for <code>removedIds</code>
     * and puts it into <code>map</code>.
     *
     * @param removedIds the ids of removed nodes.
     * @param aggregates aggregate roots are collected in this map
     */
    protected void retrieveAggregateRoot(
            Set<NodeId> removedIds, Map<NodeId, NodeState> aggregates) {
        if(removedIds.isEmpty() || indexingConfig == null){
            return;
        }
        AggregateRule[] aggregateRules = indexingConfig.getAggregateRules();
        if (aggregateRules == null) {
            return;
        }
        int found = 0;
        long time = System.currentTimeMillis();
        try {
            CachingMultiIndexReader reader = index.getIndexReader();
            try {
                Term aggregateIds =
                    new Term(FieldNames.AGGREGATED_NODE_UUID, "");
                TermDocs tDocs = reader.termDocs();
                try {
                    ItemStateManager ism = getContext().getItemStateManager();
                    for (NodeId id : removedIds) {
                        aggregateIds =
                            aggregateIds.createTerm(id.toString());
                        tDocs.seek(aggregateIds);
                        while (tDocs.next()) {
                            Document doc = reader.document(
                                    tDocs.doc(), FieldSelectors.UUID);
                            NodeId nId = new NodeId(doc.get(FieldNames.UUID));
                            NodeState nodeState = (NodeState) ism.getItemState(nId);
                            aggregates.put(nId, nodeState);
                            found++;

                            // JCR-2989 Support for embedded index aggregates
                            int sizeBefore = aggregates.size();
                            retrieveAggregateRoot(nodeState, aggregates);
                            found += aggregates.size() - sizeBefore;
                        }
                    }
                } finally {
                    tDocs.close();
                }
            } finally {
                reader.release();
            }
        } catch (NoSuchItemStateException e) {
            log.info(
                    "Exception while retrieving aggregate roots. Node is not available {}.",
                    e.getMessage());
        } catch (Exception e) {
            log.warn("Exception while retrieving aggregate roots", e);
        }
        time = System.currentTimeMillis() - time;
        log.debug("Retrieved {} aggregate roots in {} ms.", found, time);
    }

    //----------------------------< internal >----------------------------------

    /**
     * Combines multiple {@link CachingMultiIndexReader} into a <code>MultiReader</code>
     * with {@link HierarchyResolver} support.
     */
    protected static final class CombinedIndexReader
            extends MultiReader
            implements HierarchyResolver, MultiIndexReader {

        /**
         * The sub readers.
         */
        private final CachingMultiIndexReader[] subReaders;

        public CombinedIndexReader(CachingMultiIndexReader[] indexReaders) {
            super(indexReaders);
            this.subReaders = indexReaders;
        }

        /**
         * {@inheritDoc}
         */
        public int[] getParents(int n, int[] docNumbers) throws IOException {
            int i = readerIndex(n);
            DocId id = subReaders[i].getParentDocId(n - starts[i]);
            id = id.applyOffset(starts[i]);
            return id.getDocumentNumbers(this, docNumbers);
        }

        //-------------------------< MultiIndexReader >-------------------------

        /**
         * {@inheritDoc}
         */
        public IndexReader[] getIndexReaders() {
            IndexReader[] readers = new IndexReader[subReaders.length];
            System.arraycopy(subReaders, 0, readers, 0, subReaders.length);
            return readers;
        }

        /**
         * {@inheritDoc}
         */
        public void release() throws IOException {
            for (CachingMultiIndexReader subReader : subReaders) {
                subReader.release();
            }
        }

        public boolean equals(Object obj) {
            if (obj instanceof CombinedIndexReader) {
                CombinedIndexReader other = (CombinedIndexReader) obj;
                return Arrays.equals(subReaders, other.subReaders);
            }
            return false;
        }

        public int hashCode() {
            int hash = 0;
            for (CachingMultiIndexReader subReader : subReaders) {
                hash = 31 * hash + subReader.hashCode();
            }
            return hash;
        }

        /**
         * {@inheritDoc}
         */
        public ForeignSegmentDocId createDocId(NodeId id) throws IOException {
            for (CachingMultiIndexReader subReader : subReaders) {
                ForeignSegmentDocId doc = subReader.createDocId(id);
                if (doc != null) {
                    return doc;
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public int getDocumentNumber(ForeignSegmentDocId docId) {
            for (int i = 0; i < subReaders.length; i++) {
                CachingMultiIndexReader subReader = subReaders[i];
                int realDoc = subReader.getDocumentNumber(docId);
                if (realDoc >= 0) {
                    return realDoc + starts[i];
                }
            }
            return -1;
        }
    }

    //--------------------------< properties >----------------------------------

    /**
     * Sets the default analyzer in use for indexing. The given analyzer
     * class name must satisfy the following conditions:
     * <ul>
     *   <li>the class must exist in the class path</li>
     *   <li>the class must have a public default constructor, or
     *       a constructor that takes a Lucene {@link Version} argument</li>
     *   <li>the class must be a Lucene Analyzer</li>
     * </ul>
     * <p>
     * If the above conditions are met, then a new instance of the class is
     * set as the analyzer. Otherwise a warning is logged and the current
     * analyzer is not changed.
     * <p>
     * This property setter method is normally invoked by the Jackrabbit
     * configuration mechanism if the "analyzer" parameter is set in the
     * search configuration.
     *
     * @param analyzerClassName the analyzer class name
     */
    public void setAnalyzer(String analyzerClassName) {
        analyzer.setDefaultAnalyzerClass(analyzerClassName);
    }

    /**
     * Returns the class name of the default analyzer that is currently in use.
     *
     * @return class name of analyzer in use.
     */
    public String getAnalyzer() {
        return analyzer.getDefaultAnalyzerClass();
    }

    /**
     * Sets the location of the search index.
     *
     * @param path the location of the search index.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the location of the search index. Returns <code>null</code> if
     * not set.
     *
     * @return the location of the search index.
     */
    public String getPath() {
        return path;
    }

    /**
     * The lucene index writer property: useCompoundFile
     */
    public void setUseCompoundFile(boolean b) {
        useCompoundFile = b;
    }

    /**
     * Returns the current value for useCompoundFile.
     *
     * @return the current value for useCompoundFile.
     */
    public boolean getUseCompoundFile() {
        return useCompoundFile;
    }

    /**
     * The lucene index writer property: minMergeDocs
     */
    public void setMinMergeDocs(int minMergeDocs) {
        this.minMergeDocs = minMergeDocs;
    }

    /**
     * Returns the current value for minMergeDocs.
     *
     * @return the current value for minMergeDocs.
     */
    public int getMinMergeDocs() {
        return minMergeDocs;
    }

    /**
     * Sets the property: volatileIdleTime
     *
     * @param volatileIdleTime idle time in seconds
     */
    public void setVolatileIdleTime(int volatileIdleTime) {
        this.volatileIdleTime = volatileIdleTime;
    }

    /**
     * Returns the current value for volatileIdleTime.
     *
     * @return the current value for volatileIdleTime.
     */
    public int getVolatileIdleTime() {
        return volatileIdleTime;
    }

    /**
     * The lucene index writer property: maxMergeDocs
     */
    public void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
    }

    /**
     * Returns the current value for maxMergeDocs.
     *
     * @return the current value for maxMergeDocs.
     */
    public int getMaxMergeDocs() {
        return maxMergeDocs;
    }

    /**
     * The lucene index writer property: mergeFactor
     */
    public void setMergeFactor(int mergeFactor) {
        this.mergeFactor = mergeFactor;
    }

    /**
     * Returns the current value for the merge factor.
     *
     * @return the current value for the merge factor.
     */
    public int getMergeFactor() {
        return mergeFactor;
    }

    /**
     * @see VolatileIndex#setBufferSize(int)
     */
    public void setBufferSize(int size) {
        bufferSize = size;
    }

    /**
     * Returns the current value for the buffer size.
     *
     * @return the current value for the buffer size.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    public void setRespectDocumentOrder(boolean docOrder) {
        documentOrder = docOrder;
    }

    public boolean getRespectDocumentOrder() {
        return documentOrder;
    }

    public void setForceConsistencyCheck(boolean b) {
        forceConsistencyCheck = b;
    }

    public boolean getForceConsistencyCheck() {
        return forceConsistencyCheck;
    }

    public void setAutoRepair(boolean b) {
        autoRepair = b;
    }

    public boolean getAutoRepair() {
        return autoRepair;
    }

    public void setCacheSize(int size) {
        cacheSize = size;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setMaxFieldLength(int length) {
        maxFieldLength = length;
    }

    public int getMaxFieldLength() {
        return maxFieldLength;
    }

    public void setMaxExtractLength(int length) {
        maxExtractLength = length;
    }

    public int getMaxExtractLength() {
        if (maxExtractLength < 0) {
            return -maxExtractLength * maxFieldLength;
        } else {
            return maxExtractLength;
        }
    }

    /**
     * Sets the list of text extractors (and text filters) to use for
     * extracting text content from binary properties. The list must be
     * comma (or whitespace) separated, and contain fully qualified class
     * names of the {@code TextExtractor} (and {@code org.apache.jackrabbit.core.query.TextFilter}) classes
     * to be used. The configured classes must all have a public default
     * constructor.
     *
     * @param filterClasses comma separated list of class names
     * @deprecated 
     */
    public void setTextFilterClasses(String filterClasses) {
        log.warn("The textFilterClasses configuration parameter has"
                + " been deprecated, and the configured value will"
                + " be ignored: {}", filterClasses);
    }

    /**
     * Returns the fully qualified class names of the text filter instances
     * currently in use. The names are comma separated.
     *
     * @return class names of the text filters in use.
     * @deprecated 
     */
    public String getTextFilterClasses() {
        return "deprectated";
    }

    /**
     * Tells the query handler how many result should be fetched initially when
     * a query is executed.
     *
     * @param size the number of results to fetch initially.
     */
    public void setResultFetchSize(int size) {
        resultFetchSize = size;
    }

    /**
     * @return the number of results the query handler will fetch initially when
     *         a query is executed.
     */
    public int getResultFetchSize() {
        return resultFetchSize;
    }

    /**
     * The number of background threads for the extractor pool.
     *
     * @param numThreads the number of threads.
     */
    public void setExtractorPoolSize(int numThreads) {
        if (numThreads < 0) {
            numThreads = 0;
        }
        extractorPoolSize = numThreads;
    }

    /**
     * @return the size of the thread pool which is used to run the text
     *         extractors when binary content is indexed.
     */
    public int getExtractorPoolSize() {
        return extractorPoolSize;
    }

    /**
     * The number of extractor jobs that are queued until a new job is executed
     * with the current thread instead of using the thread pool.
     *
     * @param backLog size of the extractor job queue.
     */
    public void setExtractorBackLogSize(int backLog) {
        extractorBackLog = backLog;
    }

    /**
     * @return the size of the extractor queue back log.
     */
    public int getExtractorBackLogSize() {
        return extractorBackLog;
    }

    /**
     * The timeout in milliseconds which is granted to the text extraction
     * process until fulltext indexing is deferred to a background thread.
     *
     * @param timeout the timeout in milliseconds.
     */
    public void setExtractorTimeout(long timeout) {
        extractorTimeout = timeout;
    }

    /**
     * @return the extractor timeout in milliseconds.
     */
    public long getExtractorTimeout() {
        return extractorTimeout;
    }
    
    /**
     * If enabled, NodeIterator.getSize() may report a larger value than the
     * actual result. This value may shrink when the query result encounters
     * non-existing nodes or the session does not have access to a node. This
     * might be a security problem.
     * 
     * @param b <code>true</code> to enable
     */
    public void setSizeEstimate(boolean b) {
        if (b) {
            log.info("Size estimation is enabled");
        }
        this.sizeEstimate = b;
    }
    
    /**
     * Get the size estimate setting.
     * 
     * @return the setting
     */
    public boolean getSizeEstimate() {
        return sizeEstimate;
    }

    /**
     * If set to <code>true</code> additional information is stored in the index
     * to support highlighting using the rep:excerpt pseudo property.
     *
     * @param b <code>true</code> to enable highlighting support.
     */
    public void setSupportHighlighting(boolean b) {
        supportHighlighting = b;
    }

    /**
     * @return <code>true</code> if highlighting support is enabled.
     */
    public boolean getSupportHighlighting() {
        return supportHighlighting;
    }

    /**
     * Sets the class name for the {@link ExcerptProvider} that should be used
     * for the rep:excerpt pseudo property in a query.
     *
     * @param className the name of a class that implements {@link
     *                  ExcerptProvider}.
     */
    public void setExcerptProviderClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (ExcerptProvider.class.isAssignableFrom(clazz)) {
                excerptProviderClass = clazz;
            } else {
                log.warn("Invalid value for excerptProviderClass, {} does "
                        + "not implement ExcerptProvider interface.", className);
            }
        } catch (ClassNotFoundException e) {
            log.warn("Invalid value for excerptProviderClass, class {} not found.",
                    className);
        }
    }

    /**
     * @return the class name of the excerpt provider implementation.
     */
    public String getExcerptProviderClass() {
        return excerptProviderClass.getName();
    }

    /**
     * Sets the path to the indexing configuration file.
     *
     * @param path the path to the configuration file.
     */
    public void setIndexingConfiguration(String path) {
        indexingConfigPath = path;
    }

    /**
     * @return the path to the indexing configuration file.
     */
    public String getIndexingConfiguration() {
        return indexingConfigPath;
    }

    /**
     * Sets the name of the class that implements {@link IndexingConfiguration}.
     * The default value is <code>org.apache.jackrabbit.core.query.lucene.IndexingConfigurationImpl</code>.
     *
     * @param className the name of the class that implements {@link
     *                  IndexingConfiguration}.
     */
    public void setIndexingConfigurationClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (IndexingConfiguration.class.isAssignableFrom(clazz)) {
                indexingConfigurationClass = clazz;
            } else {
                log.warn("Invalid value for indexingConfigurationClass, {} "
                        + "does not implement IndexingConfiguration interface.",
                        className);
            }
        } catch (ClassNotFoundException e) {
            log.warn("Invalid value for indexingConfigurationClass, class {} not found.",
                    className);
        }
    }

    /**
     * @return the class name of the indexing configuration implementation.
     */
    public String getIndexingConfigurationClass() {
        return indexingConfigurationClass.getName();
    }

    /**
     * Sets the name of the class that implements {@link SynonymProvider}. The
     * default value is <code>null</code> (none set).
     *
     * @param className name of the class that implements {@link
     *                  SynonymProvider}.
     */
    public void setSynonymProviderClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (SynonymProvider.class.isAssignableFrom(clazz)) {
                synonymProviderClass = clazz;
            } else {
                log.warn("Invalid value for synonymProviderClass, {} "
                        + "does not implement SynonymProvider interface.",
                        className);
            }
        } catch (ClassNotFoundException e) {
            log.warn("Invalid value for synonymProviderClass, class {} not found.",
                    className);
        }
    }

    /**
     * @return the class name of the synonym provider implementation or
     *         <code>null</code> if none is set.
     */
    public String getSynonymProviderClass() {
        if (synonymProviderClass != null) {
            return synonymProviderClass.getName();
        } else {
            return null;
        }
    }

    /**
     * Sets the name of the class that implements {@link SpellChecker}. The
     * default value is <code>null</code> (none set).
     *
     * @param className name of the class that implements {@link SpellChecker}.
     */
    public void setSpellCheckerClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (SpellChecker.class.isAssignableFrom(clazz)) {
                spellCheckerClass = clazz;
            } else {
                log.warn("Invalid value for spellCheckerClass, {} "
                        + "does not implement SpellChecker interface.",
                        className);
            }
        } catch (ClassNotFoundException e) {
            log.warn("Invalid value for spellCheckerClass,"
                    + " class {} not found.", className);
        }
    }

    /**
     * @return the class name of the spell checker implementation or
     *         <code>null</code> if none is set.
     */
    public String getSpellCheckerClass() {
        if (spellCheckerClass != null) {
            return spellCheckerClass.getName();
        } else {
            return null;
        }
    }

    /**
     * Enables or disables the consistency check on startup. Consistency checks
     * are disabled per default.
     *
     * @param b <code>true</code> enables consistency checks.
     * @see #setForceConsistencyCheck(boolean)
     */
    public void setEnableConsistencyCheck(boolean b) {
        this.consistencyCheckEnabled = b;
    }

    /**
     * @return <code>true</code> if consistency checks are enabled.
     */
    public boolean getEnableConsistencyCheck() {
        return consistencyCheckEnabled;
    }

    /**
     * Sets the configuration path for the synonym provider.
     *
     * @param path the configuration path for the synonym provider.
     */
    public void setSynonymProviderConfigPath(String path) {
        synonymProviderConfigPath = path;
    }

    /**
     * @return the configuration path for the synonym provider. If none is set
     *         this method returns <code>null</code>.
     */
    public String getSynonymProviderConfigPath() {
        return synonymProviderConfigPath;
    }

    /**
     * Sets the similarity implementation, which will be used for indexing and
     * searching. The implementation must extend {@link Similarity}.
     *
     * @param className a {@link Similarity} implementation.
     */
    public void setSimilarityClass(String className) {
        try {
            Class<?> similarityClass = Class.forName(className);
            similarity = (Similarity) similarityClass.newInstance();
        } catch (Exception e) {
            log.warn("Invalid Similarity class: " + className, e);
        }
    }

    /**
     * @return the name of the similarity class.
     */
    public String getSimilarityClass() {
        return similarity.getClass().getName();
    }

    /**
     * Sets a new maxVolatileIndexSize value.
     *
     * @param maxVolatileIndexSize the new value.
     */
    public void setMaxVolatileIndexSize(long maxVolatileIndexSize) {
        this.maxVolatileIndexSize = maxVolatileIndexSize;
    }

    /**
     * @return the maxVolatileIndexSize in bytes.
     */
    public long getMaxVolatileIndexSize() {
        return maxVolatileIndexSize;
    }

    /**
     * @return the name of the directory manager class.
     */
    public String getDirectoryManagerClass() {
        return directoryManagerClass;
    }

    /**
     * Sets name of the directory manager class. The class must implement
     * {@link DirectoryManager}.
     *
     * @param className the name of the class that implements directory manager.
     */
    public void setDirectoryManagerClass(String className) {
        this.directoryManagerClass = className;
    }

    /**
     * If set <code>true</code> will indicate to the {@link DirectoryManager}
     * to use the <code>SimpleFSDirectory</code>.
     *
     * @param useSimpleFSDirectory whether to use <code>SimpleFSDirectory</code>
     *                             or automatically pick an implementation based
     *                             on the current platform.
     */
    public void setUseSimpleFSDirectory(boolean useSimpleFSDirectory) {
        this.useSimpleFSDirectory = useSimpleFSDirectory;
    }

    /**
     * @return <code>true</code> if the {@link DirectoryManager} should use
     * the <code>SimpleFSDirectory</code>.
     */
    public boolean isUseSimpleFSDirectory() {
        return useSimpleFSDirectory;
    }

    /**
     * @return the current value for termInfosIndexDivisor.
     */
    public int getTermInfosIndexDivisor() {
        return termInfosIndexDivisor;
    }

    /**
     * Sets a new value for termInfosIndexDivisor.
     *
     * @param termInfosIndexDivisor the new value.
     */
    public void setTermInfosIndexDivisor(int termInfosIndexDivisor) {
        this.termInfosIndexDivisor = termInfosIndexDivisor;
    }

    /**
     * @return <code>true</code> if the hierarchy cache should be initialized
     *         immediately on startup.
     */
    public boolean isInitializeHierarchyCache() {
        return initializeHierarchyCache;
    }

    /**
     * Whether the hierarchy cache should be initialized immediately on
     * startup.
     *
     * @param initializeHierarchyCache <code>true</code> if the cache should be
     *                                 initialized immediately.
     */
    public void setInitializeHierarchyCache(boolean initializeHierarchyCache) {
        this.initializeHierarchyCache = initializeHierarchyCache;
    }

    /**
     * @return the maximum age in seconds for outdated generations of
     * {@link IndexInfos}.
     */
    public long getMaxHistoryAge() {
        return maxHistoryAge;
    }

    /**
     * Sets a new value for the maximum age in seconds for outdated generations
     * of {@link IndexInfos}.
     *
     * @param maxHistoryAge age in seconds.
     */
    public void setMaxHistoryAge(long maxHistoryAge) {
        this.maxHistoryAge = maxHistoryAge;
    }

    /**
     * @return the name of the redo log factory class.
     */
    public String getRedoLogFactoryClass() {
        return redoLogFactoryClass;
    }

    /**
     * Sets the name of the redo log factory class. Must implement
     * {@link RedoLogFactory}.
     *
     * @param className the name of the redo log factory class.
     */
    public void setRedoLogFactoryClass(String className) {
        this.redoLogFactoryClass = className;
    }

    /**
     * In the case of an initial index build operation, this checks if there are
     * some new nodes pending in the journal and tries to preemptively delete
     * them, to keep the index consistent.
     * 
     * See JCR-3162
     * 
     * @param context
     * @throws IOException
     */
    private void checkPendingJournalChanges(QueryHandlerContext context) {
        ClusterNode cn = context.getClusterNode();
        if (cn == null) {
            return;
        }

        List<NodeId> addedIds = new ArrayList<NodeId>();
        long rev = cn.getRevision();

        List<ChangeLogRecord> changes = getChangeLogRecords(rev, context.getWorkspace());
        Iterator<ChangeLogRecord> iterator = changes.iterator();
        while (iterator.hasNext()) {
            ChangeLogRecord record = iterator.next();
            for (ItemState state : record.getChanges().addedStates()) {
                if (!state.isNode()) {
                    continue;
                }
                addedIds.add((NodeId) state.getId());
            }
        }
        if (!addedIds.isEmpty()) {
            Collection<NodeState> empty = Collections.emptyList();
            try {
                updateNodes(addedIds.iterator(), empty.iterator());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    //----------------------------< internal >----------------------------------

    /**
     * Checks if this <code>SearchIndex</code> is open, otherwise throws
     * an <code>IOException</code>.
     *
     * @throws IOException if this <code>SearchIndex</code> had been closed.
     */
    protected void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("query handler closed and cannot be used anymore.");
        }
    }

    /**
     * Polls the underlying journal for events of the type ChangeLogRecord that
     * happened after a given revision, on a given workspace.
     *
     * @param revision
     *            starting revision
     * @param workspace
     *            the workspace name
     * @return
     */
    private List<ChangeLogRecord> getChangeLogRecords(long revision,
            final String workspace) {
        log.debug(
                "Get changes from the Journal for revision {} and workspace {}.",
                revision, workspace);
        ClusterNode cn = getContext().getClusterNode();
        if (cn == null) {
            return Collections.emptyList();
        }
        Journal journal = cn.getJournal();
        final List<ChangeLogRecord> events = new ArrayList<ChangeLogRecord>();
        ClusterRecordDeserializer deserializer = new ClusterRecordDeserializer();
        RecordIterator records = null;
        try {
            records = journal.getRecords(revision);
            while (records.hasNext()) {
                Record record = records.nextRecord();
                if (!record.getProducerId().equals(cn.getId())) {
                    continue;
                }
                ClusterRecord r = null;
                try {
                    r = deserializer.deserialize(record);
                } catch (JournalException e) {
                    log.error(
                            "Unable to read revision '" + record.getRevision()
                                    + "'.", e);
                }
                if (r == null) {
                    continue;
                }
                r.process(new ClusterRecordProcessor() {
                    public void process(ChangeLogRecord record) {
                        String eventW = record.getWorkspace();
                        if (eventW != null ? eventW.equals(workspace) : workspace == null) {
                            events.add(record);
                        }
                    }

                    public void process(LockRecord record) {
                    }

                    public void process(NamespaceRecord record) {
                    }

                    public void process(NodeTypeRecord record) {
                    }

                    public void process(PrivilegeRecord record) {
                    }

                    public void process(WorkspaceRecord record) {
                    }
                });
            }
        } catch (JournalException e1) {
            log.error(e1.getMessage(), e1);
        } finally {
            if (records != null) {
                records.close();
            }
        }
        return events;
    }
}
