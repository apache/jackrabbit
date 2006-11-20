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
