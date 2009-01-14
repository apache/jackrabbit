/**
 * 
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