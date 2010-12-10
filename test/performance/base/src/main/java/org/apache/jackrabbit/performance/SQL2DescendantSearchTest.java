package org.apache.jackrabbit.performance;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

/**
 * SQL-2 version of the sub-tree performance test.
 */
public class SQL2DescendantSearchTest extends DescendantSearchTest {

    protected Query createQuery(QueryManager manager, int i)
            throws RepositoryException {
        return manager.createQuery(
                "SELECT * FROM [nt:base] AS n WHERE ISDESCENDANTNODE(n, '/testroot') AND testcount=" + i,
                "JCR-SQL2");
    }

}
