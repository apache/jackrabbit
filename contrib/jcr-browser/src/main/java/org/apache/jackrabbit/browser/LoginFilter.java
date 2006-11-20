package org.apache.jackrabbit.browser;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.jackrabbit.command.CommandHelper;

public class LoginFilter implements Filter {

	public void destroy() {

	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws java.io.IOException, ServletException {

		chain.doFilter(request, response);

		// bind jcr session to http session on login
		try {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			if (httpRequest.getSession()
					.getAttribute(CommandHelper.SESSION_KEY) == null) {
				Session s = JcrSessionListener.getRepository().login(
						new SimpleCredentials(httpRequest.getRemoteUser(), ""
								.toCharArray()));
				httpRequest.getSession().setAttribute(
						CommandHelper.SESSION_KEY, s);
			}
		} catch (Exception e) {
			throw new ServletException(
					"unable to bind jcr session to http session", e);
		}
	}

	public void init(FilterConfig cfg) throws ServletException {

	}

}
