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
package org.apache.jackrabbit.core.state;

public abstract class TimeBomb {

    private final boolean[] armed = {false};

    private final long millis;

    private Thread timer;

    public TimeBomb(long millis) {
        this.millis = millis;
    }

    public void arm() throws InterruptedException {
        synchronized (armed) {
            if (armed[0]) {
                return;
            } else {
                timer = new Thread(new Runnable() {
                    public void run() {
                        synchronized (armed) {
                            armed[0] = true;
                            armed.notify();
                        }
                        try {
                            Thread.sleep(millis);
                            explode();
                        } catch (InterruptedException e) {
                            // disarmed
                        }
                    }
                });
                timer.start();
            }
        }
        synchronized (armed) {
            while (!armed[0]) {
                armed.wait();
            }
        }
    }

    public void disarm() throws InterruptedException {
        synchronized (armed) {
            if (!armed[0]) {
                return;
            }
        }
        timer.interrupt();
        timer.join();
    }

    public abstract void explode();
}
