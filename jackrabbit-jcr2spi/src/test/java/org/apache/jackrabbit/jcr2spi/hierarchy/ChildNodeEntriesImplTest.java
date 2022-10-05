package org.apache.jackrabbit.jcr2spi.hierarchy;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChildNodeEntriesImplTest {

    private ExecutorService executorService;

    @Before
    public void setUp() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @After
    public void after() {
        executorService.shutdown();
    }

    @Test(expected = ConcurrentModificationException.class)
    public void concurrentModification() throws Throwable {
        ChildNodeEntriesImpl entries = new ChildNodeEntriesImpl(null, null, null);
        Collection<Callable<Object>> futures = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            NodeEntry nodeEntry = mock(NodeEntry.class);
            futures.add(() -> entries.getNext(nodeEntry));
            futures.add(() -> {
                entries.add(nodeEntry);
                return null;
            });
        }
        for (Future<?> future : executorService.invokeAll(futures)) {
            try {
                future.get();
            } catch (Exception e) {
                throw e.getCause();
            }
        }
    }
}