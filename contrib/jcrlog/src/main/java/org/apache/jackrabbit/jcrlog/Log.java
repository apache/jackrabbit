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
package org.apache.jackrabbit.jcrlog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class writes the log output to System.out and / or to a file. A simple
 * implementation, could be replaced with log4j or whatever.
 *
 * @author Thomas Mueller
 */
class Log {

    private long lastLogTime;

    private String fileName;

    private SimpleDateFormat dateFormat;

    private FileWriter fileWriter;

    private PrintWriter printWriter;

    private boolean systemOut;

    private String lineEnd;

    private boolean logReturn = true;
    private boolean logTime = false;
    private boolean logCaller = true;
    private boolean castToRealApi = false;
    private boolean logStream = false;

    private static void createDirs(String fileName) throws IOException {
        File f = new File(fileName);
        if (!f.exists()) {
            String parent = f.getParent();
            if (parent == null) {
                return;
            }
            File dir = new File(parent);
            if (dir.exists() || dir.mkdirs()) {
                return;
            }
            throw new IOException("Directory creation failed: " + fileName);
        }
    }

    private void print(String s, Throwable t) {
        System.out.println(s);
        if (t != null) {
            t.printStackTrace();
        }
    }

    private String format(String s) {
        long time = System.currentTimeMillis();
        if (time > lastLogTime + 1000) {
            lastLogTime = time;
            s = "//" + dateFormat.format(new Date()) + lineEnd + s;
        }
        return s;
    }

    void write(String s, Throwable t) {
        s = format(s);
        if (systemOut) {
            System.out.println(s);
            if (t != null) {
                t.printStackTrace();
            }
        }
        if (printWriter != null) {
            writeFile(s, t);
        }
    }

    private synchronized void writeFile(String s, Throwable t) {
        try {
            printWriter.println(s);
            if (t != null) {
                t.printStackTrace(printWriter);
            }
            printWriter.flush();
        } catch (Exception e) {
            print("Error writing to log file", e);
        }
    }

    Log(String fileName, boolean systemOut) {
        this.fileName = fileName;
        this.systemOut = systemOut;
        dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss ");
        lineEnd = System.getProperty("line.separator");
        if (fileName != null) {
            try {
                createDirs(fileName);
                File f = new File(fileName);
                if (f.exists() && !f.canWrite()) {
                    // file is read only - don't log
                    print("The file is read only, logging is disabled: "
                            + fileName, null);
                }
                fileWriter = new FileWriter(fileName);
                printWriter = new PrintWriter(fileWriter, true);
            } catch (IOException e) {
                print("Can not create or open log file: " + fileName, e);
            }
        }
    }

    private synchronized void closeWriter() {
        if (printWriter != null) {
            printWriter.flush();
            printWriter.close();
            printWriter = null;
        }
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                // ignore exception
            }
            fileWriter = null;
        }
    }

    void close() {
        closeWriter();
    }

    public void finalize() {
        close();
    }

    String getFileName() {
        return fileName;
    }

    boolean getLogReturn() {
        return logReturn;
    }

    void setLogCaller(boolean logCaller) {
        this.logCaller = logCaller;
    }

    boolean getLogCaller() {
        return logCaller;
    }

    boolean getLogTime() {
        return logTime;
    }

    void setLogReturn(boolean b) {
        this.logReturn = b;
    }

    void setCastToRealApi(boolean b) {
        this.castToRealApi = b;
    }

    boolean getCastToRealApi() {
        return castToRealApi;
    }

    void setLogStream(boolean b) {
        this.logStream = b;
    }

    boolean getLogStream() {
        return logStream;
    }

}
