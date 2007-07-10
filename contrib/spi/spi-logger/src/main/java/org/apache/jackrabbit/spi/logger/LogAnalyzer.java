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
package org.apache.jackrabbit.spi.logger;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Arrays;
import java.text.NumberFormat;

/**
 * <code>LogAnalyzer</code> implements an analyzer for the log files created by
 * the {@link RepositoryServiceLogger}.
 */
public class LogAnalyzer {

    private final BufferedReader log;

    private static final Set EXCLUSIONS = new HashSet(Arrays.asList(
            new String[]{"getEvents(SessionInfo,long,EventFilter[])"}));

    /**
     * Runs the <code>LogAnalyzer</code> on a given log file. Calls to the
     * method {@link org.apache.jackrabbit.spi.RepositoryService#getEvents(org.apache.jackrabbit.spi.SessionInfo, long, org.apache.jackrabbit.spi.EventFilter[])}
     * are excluded because they contain a timeout parameter which would affect
     * the statistics adversely.
     *
     * @param args the first argument must be a valid path to an spi log file.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please specify the location of your SPI log file.");
            return;
        }
        try {
            Reader r = new InputStreamReader(new FileInputStream(args[0]));
            try {
                new LogAnalyzer(r).getReport(EXCLUSIONS).print(new PrintWriter(System.out));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } finally {
                try {
                    r.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Creates a new <code>LogAnalyzer</code>, which reads from the passed
     * <code>log</code>.
     *
     * @param log a reader to an SPI log file.
     */
    public LogAnalyzer(Reader log) {
        this.log = new BufferedReader(log);
    }

    /**
     * Creates a <code>Report</code> using a set of excluded methods.
     *
     * @param exclusions the methods to exclude from the report.
     * @return a <code>Report</code>.
     * @throws IOException if an error occurs while reading from the log
     *                     reader.
     */
    public Report getReport(Set exclusions) throws IOException {
        Report report = new Report();
        String line;
        long start = 0;
        String time = "";
        while ((line = log.readLine()) != null) {
            StringTokenizer tokenizer = new StringTokenizer(line, "|");
            // first token is timestamp
            time = tokenizer.nextToken();
            if (start == 0) {
                start = Long.parseLong(time);
            }
            // second token is methodName
            String methodName = tokenizer.nextToken();
            if (exclusions.contains(methodName)) {
                continue;
            }
            // third token is timing
            long nanos = Long.parseLong(tokenizer.nextToken());
            // ignore remaining tokens

            report.addMethodCall(methodName, nanos);
        }

        report.setStartTime(start);
        report.setEndTime(Long.parseLong(time));

        return report;
    }

    /**
     * Represents a report on an SPI log file.
     */
    public static final class Report {

        /**
         * Maps method name strings to a <code>Set</code> of
         * {@link LogAnalyzer.Timing}.
         */
        private final Map methodsToTimings = new HashMap();

        /**
         * The timestamp of the first log entry.
         */
        private long startTime = 0;

        /**
         * The timestamp of the last log entry.
         */
        private long endTime = 0;

        private Report() {
        }

        /**
         * Adds a method call to the report.
         *
         * @param methodSignature the method signature.
         * @param nanos the time spent in this method in nanoseconds.
         */
        private void addMethodCall(String methodSignature, long nanos) {
            Set timings = (Set) methodsToTimings.get(methodSignature);
            if (timings == null) {
                timings = new TreeSet();
                methodsToTimings.put(methodSignature, timings);
            }
            timings.add(new Timing(nanos));
        }

        /**
         * Sets the time of the first log entry.
         *
         * @param time the time of the first log entry.
         */
        private void setStartTime(long time) {
            this.startTime = time;
        }

        /**
         * Sets the time of the last log entry.
         *
         * @param time the time of the last log entry.
         */
        private void setEndTime(long time) {
            this.endTime = time;
        }

        /**
         * Prints this report to the passed <code>writer</code>.
         *
         * @param writer where the report is written to.
         */
        public void print(PrintWriter writer) {
            List methods = new ArrayList();
            methods.addAll(methodsToTimings.keySet());
            long totalNanos = 0;
            final Map accumulatedTime = new HashMap();
            for (Iterator it = methodsToTimings.keySet().iterator(); it.hasNext(); ) {
                String method = (String) it.next();
                Set timings = (Set) methodsToTimings.get(method);
                long methodTotal = 0;
                for (Iterator iter = timings.iterator(); iter.hasNext(); ) {
                    Timing t = (Timing) iter.next();
                    methodTotal += t.nanos;
                }
                accumulatedTime.put(method, new Long(methodTotal));
                totalNanos += methodTotal;
            }
            // sort by accumulated time
            Collections.sort(methods, new Comparator() {
                public int compare(Object o1, Object o2) {
                    Long t1 = (Long) accumulatedTime.get(o1);
                    Long t2 = (Long) accumulatedTime.get(o2);
                    return t2.compareTo(t1);
                }
            });
            writer.println();
            long timespan = (endTime - startTime) / 1000;
            long totalSeconds = (totalNanos / (1000 * 1000 * 1000));
            writer.println("Logged timespan:                  " +
                    timespan + "s");
            writer.println("Time spent in SPI implementation: " +
                    totalSeconds + "s (" + 100 * totalSeconds / timespan + "%)");
            writer.println();
            writer.println("         #calls   mean(ms)  method");
            NumberFormat percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMaximumFractionDigits(1);
            percentFormat.setMinimumFractionDigits(1);
            NumberFormat nFormat = NumberFormat.getNumberInstance();
            nFormat.setMaximumFractionDigits(1);
            nFormat.setMinimumFractionDigits(1);
            for (Iterator it = methods.iterator(); it.hasNext(); ) {
                String methodName = (String) it.next();
                StringBuffer tmp = new StringBuffer();
                long methodTotal = ((Long) accumulatedTime.get(methodName)).longValue();
                String percent = percentFormat.format((double) methodTotal / totalNanos);
                while (percent.length() < 6) {
                    percent = " " + percent;
                }
                tmp.append(percent);
                Set timings = (Set) methodsToTimings.get(methodName);
                String nCalls = String.valueOf(timings.size());
                while (nCalls.length() < 9) {
                    nCalls = " " + nCalls;
                }
                tmp.append(nCalls);
                String mean = nFormat.format((double) methodTotal / (1000 * 1000) / timings.size());
                while (mean.length() < 9) {
                    mean = " " + mean;
                }
                tmp.append(mean);
                tmp.append("    " + methodName);
                writer.println(tmp.toString());
            }
            writer.flush();
        }
    }

    /**
     * Helper class that contains the method timing in nanoseconds and a
     * sequence number, which allows a well-order on the timing objects.
     */
    private static final class Timing implements Comparable {

        /**
         * Sequence counter.
         */
        private static int SEQUENCE_NUMBER = 0;

        /**
         * Sequence number
         */
        private final int seq;

        /**
         * Time in nanoseconds
         */
        private final long nanos;

        Timing(long nanos) {
            synchronized (Timing.class) {
                this.seq = SEQUENCE_NUMBER++;
            }
            this.nanos = nanos;
        }

        public int hashCode() {
            return new Long(nanos).hashCode() ^ new Integer(seq).hashCode();
        }

        public boolean equals(Object obj) {
            if (obj instanceof Timing) {
                Timing other = (Timing) obj;
                return this.nanos == other.nanos && this.seq == other.seq;
            }
            return false;
        }

        public int compareTo(Object o) {
            Timing other = (Timing) o;
            if (this.nanos < other.nanos) {
                return -1;
            } else if (this.nanos == other.nanos) {
                return this.seq < other.seq ? -1 : (this.seq == other.seq ? 0 : -1);
            } else {
                return 1;
            }
        }
    }
}
