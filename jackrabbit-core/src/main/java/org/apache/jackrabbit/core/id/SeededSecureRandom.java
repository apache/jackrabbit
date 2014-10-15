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
package org.apache.jackrabbit.core.id;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Automatically seeded singleton secure random number generator.
 *
 * @see <a href="https://issues.apache.org/jira/browse/JCR-1206">JCR-1206</a>:
 *      UUID generation: SecureRandom should be used by default
 */
class SeededSecureRandom extends SecureRandom implements Runnable {

    /**
     * Maximum number of milliseconds to wait for the seeding.
     */
    private static final int MAX_SEED_TIME = 1000;

    /**
     * Singleton instance of this class. Initialized when first accessed.
     */
    private static volatile Random instance = null;

    /**
     * Returns the singleton instance of this class. The instance is
     * created and seeded when this method is first called.
     *
     * @return seeded secure random number generator
     */
    public static Random getInstance() {
        if (instance == null) {
            synchronized (SeededSecureRandom.class) {
                if (instance == null) {
                    instance = new SeededSecureRandom();
                }
            }
        }
        return instance;
    }

    /**
     * Flag to indicate whether seeding is complete.
     */
    private volatile boolean seeded = false;

    /**
     * Creates and seeds a secure random number generator.
     */
    private SeededSecureRandom() {
        // Can not do that in a static initializer block, because
        // threads are not started after the initializer block exits
        Thread thread = new Thread(this, "SeededSecureRandom");
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join(MAX_SEED_TIME);
        } catch (InterruptedException e) {
            // ignore
        }

        if (!seeded) {
            // Alternative seed algorithm if the default is very slow
            setSeed(System.currentTimeMillis());
            setSeed(System.nanoTime());
            setSeed(new Object().hashCode());
            Runtime runtime = Runtime.getRuntime();
            setSeed(runtime.freeMemory());
            setSeed(runtime.maxMemory());
            setSeed(runtime.totalMemory());
            setSeed(System.getProperties().toString().hashCode());

            // Thread timing (a second thread is already running)
            for (int j = 0; j < 16; j++) {
                int i = 0;
                long start = System.currentTimeMillis();
                while (start == System.currentTimeMillis()) {
                    i++;
                }
                // Supplement the existing seed
                setSeed(i);
            }
        }
    }

    /**
     * Seeds this random number generator with 32 bytes of random data.
     * Run in an initializer thread as this may be slow on some systems, see
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6202721.
     */
    public void run() {
        setSeed(generateSeed(32));
        seeded = true;
    }

}
