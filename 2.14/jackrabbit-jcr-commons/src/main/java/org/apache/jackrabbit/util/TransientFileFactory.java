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
package org.apache.jackrabbit.util;

import java.io.File;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;

/**
 * The <code>TransientFileFactory</code> utility class can be used to create
 * <i>transient</i> files, i.e. temporary files that are automatically
 * removed once the associated <code>File</code> object is reclaimed by the
 * garbage collector.
 * <p>
 * File deletion is handled by a low-priority background thread.
 * <p>
 */
public class TransientFileFactory {

    /**
     * The singleton factory instance
     */
    private static TransientFileFactory INSTANCE;

    /**
     * Queue where <code>MoribundFileReference</code> instances will be enqueued
     * once the associated target <code>File</code> objects have been gc'ed.
     */
    private final ReferenceQueue<File> phantomRefQueue = new ReferenceQueue<File>();

    /**
     * Collection of <code>MoribundFileReference</code> instances currently
     * being tracked.
     */
    private final Collection<MoribundFileReference> trackedRefs =
        Collections.synchronizedList(new ArrayList<MoribundFileReference>());

    /**
     * The reaper thread responsible for removing files awaiting deletion
     */
    private final ReaperThread reaper;

    /**
     * Shutdown hook which removes all files awaiting deletion
     */
    private static Thread shutdownHook = null;

    /**
     * Returns the singleton <code>TransientFileFactory</code> instance.
     */
    public static TransientFileFactory getInstance() {
        synchronized (TransientFileFactory.class) {
            if (INSTANCE == null) {
                INSTANCE = new TransientFileFactory();
            }
            return INSTANCE;
        }
    }

    /**
     * Hidden constructor.
     */
    private TransientFileFactory() {
        // instantiate & start low priority reaper thread
        reaper = new ReaperThread("Transient File Reaper");
        reaper.setPriority(Thread.MIN_PRIORITY);
        reaper.setDaemon(true);
        reaper.start();
        // register shutdownhook for final cleaning up
        try {
            shutdownHook = new Thread() {
                public void run() {
                    doShutdown();
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (IllegalStateException e) {
            // can't register shutdownhook because
            // jvm shutdown sequence has already begun,
            // silently ignore...
        }
    }

    //------------------------------------------------------< factory methods >
    /**
     * Same as {@link File#createTempFile(String, String, File)} except that
     * the newly-created file will be automatically deleted once the
     * returned <code>File</code> object has been gc'ed.
     *
     * @param prefix    The prefix string to be used in generating the file's
     *                  name; must be at least three characters long
     * @param suffix    The suffix string to be used in generating the file's
     *                  name; may be <code>null</code>, in which case the
     *                  suffix <code>".tmp"</code> will be used
     * @param directory The directory in which the file is to be created, or
     *                  <code>null</code> if the default temporary-file
     *                  directory is to be used
     * @return the newly-created empty file
     * @throws IOException If a file could not be created
     */
    public File createTransientFile(String prefix, String suffix, File directory)
            throws IOException {
        File f = File.createTempFile(prefix, suffix, directory);
        trackedRefs.add(new MoribundFileReference(f, phantomRefQueue));
        return f;
    }

    /**
     * Shuts this factory down removing all temp files and removes shutdown hook.
     * <p>
     * <b>Warning!!!</b>
     * <p>
     * This should be called by a web-application <b><i>IF</i></b> it is unloaded
     * <b><i>AND IF</i></b> jackrabbit-jcr-commons.jar had been loaded by
     * the webapp classloader. This must be called after all repositories had
     * been stopped, so use with great care!
     * <p>
     * See http://issues.apache.org/jira/browse/JCR-1636 for details.
     */
    public static void shutdown() {
        getInstance().doShutdown();
    }

    /**
     * Actually shuts factory down removing all temp files. This happens when
     * VM shutdown hook works or when explicitly requested.
     * Shutdown hook is removed.
     */
    private synchronized void doShutdown() {
        // synchronize on the list before iterating over it in order
        // to avoid ConcurrentModificationException (JCR-549)
        // @see java.lang.util.Collections.synchronizedList(java.util.List)
        synchronized(trackedRefs) {
            for (Iterator<MoribundFileReference> it = trackedRefs.iterator(); it.hasNext();) {
                it.next().delete();
            }

        }
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // can't unregister shutdownhook because
                // jvm shutdown sequence has already begun,
                // silently ignore... 
            }
            shutdownHook = null;
        }
        reaper.stopWorking();
    }

    //--------------------------------------------------------< inner classes >
    /**
     * The reaper thread that will remove the files that are ready for deletion.
     */
    private class ReaperThread extends Thread {

        private volatile boolean stopping = false;

        ReaperThread(String name) {
            super(name);
        }

        /**
         * Run the reaper thread that will delete files as their associated
         * marker objects are reclaimed by the garbage collector.
         */
        public void run() {
            while (!stopping) {
                MoribundFileReference fileRef = null;
                try {
                    // wait until a MoribundFileReference is ready for deletion
                    fileRef = (MoribundFileReference) phantomRefQueue.remove();
                } catch (InterruptedException e) {
                    if (stopping) {
                        break;
                    }
                } catch (Exception e) {
                    // silently ignore...
                    continue;
                }
                // delete target
                fileRef.delete();
                fileRef.clear();
                trackedRefs.remove(fileRef);
            }
        }

        /**
         * Stops the reaper thread.
         */
        public void stopWorking() {
            stopping = true;
            interrupt();
        }
    }

    /**
     * Tracker object for a file pending deletion.
     */
    private static class MoribundFileReference extends PhantomReference<File> {

        /**
         * The full path to the file being tracked.
         */
        private final String path;

        /**
         * Constructs an instance of this class from the supplied parameters.
         *
         * @param file  The file to be tracked.
         * @param queue The queue on to which the tracker will be pushed.
         */
        MoribundFileReference(File file, ReferenceQueue<File> queue) {
            super(file, queue);
            this.path = file.getPath();
        }

        /**
         * Deletes the file associated with this instance.
         *
         * @return <code>true</code> if the file was deleted successfully;
         *         <code>false</code> otherwise.
         */
        boolean delete() {
            return new File(path).delete();
        }
    }
}
