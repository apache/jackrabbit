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
package org.apache.jackrabbit.servlet.jackrabbit;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.api.stats.RepositoryStatistics;
import org.apache.jackrabbit.api.stats.TimeSeries;
import org.apache.jackrabbit.core.RepositoryContext;

/**
 * Servlet that makes Jackrabbit repository statistics available as
 * a JSON object.
 *
 * @since Apache Jackrabbit 2.3.1
 */
public class StatisticsServlet extends HttpServlet {

    /** Serial version UID */
    private static final long serialVersionUID = -7494195499389135951L;

    @Override
    protected void doGet(
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String klass = RepositoryContext.class.getName();
        String name = getServletConfig().getInitParameter(klass);
        if (name == null) {
            name = klass;
        }

        RepositoryContext context = (RepositoryContext)
                getServletContext().getAttribute(name);
        if (context != null) {
            RepositoryStatistics statistics = context.getRepositoryStatistics();
            response.setContentType("application/json");
            Writer writer = response.getWriter();
            writer.write('{');
            write(writer, "read", statistics.getTimeSeries(
                    RepositoryStatistics.Type.SESSION_READ_COUNTER));
            writer.write(',');
            write(writer, "write", statistics.getTimeSeries(
                    RepositoryStatistics.Type.SESSION_WRITE_COUNTER));
            writer.write(',');
            write(writer, "login", statistics.getTimeSeries(
                    RepositoryStatistics.Type.SESSION_LOGIN_COUNTER));
            writer.write('}');
        } else {
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Jackrabbit repository internals are not available");
        }
    }

    private void write(Writer writer, String name, TimeSeries series)
            throws IOException {
        writer.write('"');
        writer.write(name);
        writer.write('"');
        writer.write(':');
        writer.write('{');
        write(writer, "second", series.getValuePerSecond());
        writer.write(',');
        write(writer, "minute", series.getValuePerMinute());
        writer.write(',');
        write(writer, "hour", series.getValuePerHour());
        writer.write(',');
        write(writer, "week", series.getValuePerWeek());
        writer.write('}');
    }

    private void write(Writer writer, String name, long[] values)
            throws IOException {
        writer.write('"');
        writer.write(name);
        writer.write('"');
        writer.write(':');
        writer.write('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(String.valueOf(values[i]));
        }
        writer.write(']');
    }
}
