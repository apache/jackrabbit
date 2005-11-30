/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.webdav.util;

import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.text.DateFormatSymbols;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <code>HttpDateFormat</code>...
 */
public class HttpDateFormat extends SimpleDateFormat {

    private static Logger log = Logger.getLogger(HttpDateFormat.class);
    private static final TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");

    public HttpDateFormat() {
        super();
        super.setTimeZone(GMT_TIMEZONE);
    }

    public HttpDateFormat(String pattern) {
        super(pattern);
        super.setTimeZone(GMT_TIMEZONE);
    }

    public HttpDateFormat(String pattern, DateFormatSymbols formatSymbols) {
        super(pattern, formatSymbols);
        super.setTimeZone(GMT_TIMEZONE);
    }

    public HttpDateFormat(String pattern, Locale locale) {
        super(pattern, locale);
        super.setTimeZone(GMT_TIMEZONE);
    }
}