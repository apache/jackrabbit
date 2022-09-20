package org.apache.jackrabbit.jcr2spi.hierarchy;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ChildNodeEntriesImplTest {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Test(expected = ConcurrentModificationException.class)
    public void concurrentModification() throws Throwable {
        ChildNodeEntriesImpl entries = new ChildNodeEntriesImpl(null, null, null);
        Collection<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            NodeEntry nodeEntry = mock(NodeEntry.class);
            futures.add(executorService.submit(() -> entries.getNext(nodeEntry)));
            futures.add(executorService.submit(() -> entries.add(nodeEntry)));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        }
        executorService.shutdown();
    }
}