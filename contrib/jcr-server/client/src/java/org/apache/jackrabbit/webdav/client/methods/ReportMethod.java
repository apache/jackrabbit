/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.webdav.client.methods;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.header.DepthHeader;
import org.apache.jackrabbit.webdav.DavMethods;
import org.jdom.Document;

/**
 * <code>ReportMethod</code>...
 */
public class ReportMethod extends DavMethodBase {

    private static Logger log = Logger.getLogger(ReportMethod.class);

    public ReportMethod(String uri, ReportInfo reportInfo) {
	super(uri);
	DepthHeader dh = new DepthHeader(reportInfo.getDepth());
	setRequestHeader(dh.getHeaderName(), dh.getHeaderValue());
	setRequestHeader("Content-Type","text/xml; charset=UTF-8");

	Document reportBody = new Document(reportInfo.getReportElement());
	setRequestBody(reportBody);
    }

    public String getName() {
	return DavMethods.METHOD_REPORT;
    }
}