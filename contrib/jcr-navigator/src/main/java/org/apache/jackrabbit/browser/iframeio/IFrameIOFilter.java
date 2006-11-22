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
package org.apache.jackrabbit.browser.iframeio;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * IframeIO responses need to be a little different from the ones that are sent
 * back from XMLHttpRequest responses. Because an iframe is used, the only
 * reliable, cross-browser way of knowing when the response is loaded is to use
 * an HTML document as the return type. <br>
 * see http://manual.dojotoolkit.org/WikiHome/DojoDotBook/Book24
 */
public class IFrameIOFilter implements Filter {

	private static String PRE = "<html><head></head><body><textarea>";
	private static String POST = "</textarea></body></html>";

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletResponse res = (HttpServletResponse) response;
		
		OutputStream out = res.getOutputStream();

		// Write prefix
		out.write(PRE.getBytes());

		// process request
		IFrameIOResponseWrapper wrapper = new IFrameIOResponseWrapper(res);
		chain.doFilter(request, wrapper);

		// write processed response
		out.write(wrapper.getData());

		// Write sufix
		out.write(POST.getBytes());
		out.close();

	}

	public void init(FilterConfig cfg) throws ServletException {
	}

}
