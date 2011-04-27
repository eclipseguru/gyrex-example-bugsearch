/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.examples.bugsearch.internal.restapi;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.examples.bugsearch.internal.BugSearchDebug;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all HTTP REST APIs.
 */
public abstract class BaseAPIServlet extends HttpServlet {

	private static final Logger LOG = LoggerFactory.getLogger(BaseAPIServlet.class);

	protected static final String CONTENT_TYPE_TEXT = "text/plain";
	protected static final String CONTENT_TYPE_JSON = "application/json";
	protected static final String PARAM_HELP = "help";
	protected static final String PARAM_TEXT = "text";
	protected static final String UTF_8 = "UTF-8";
	protected static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	protected static final String LAST_MODIFIED = "Last-Modified";
	protected static final String CACHE_CONTROL = "Cache-Control";

	private final long helpModifiedTs = (System.currentTimeMillis() / 1000) * 1000;

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/**
	 * Convenience method for building URLs that point to this servlet.
	 * 
	 * @param req
	 *            the request
	 * @return {@link StringBuilder} consisting of an URL which points to this
	 *         servlet (does not with a slash)
	 */
	protected static StringBuilder getBaseUrl(final HttpServletRequest req) {
		final StringBuilder builder = new StringBuilder(50);
		builder.append(req.getScheme());
		builder.append("://");
		builder.append(req.getServerName());
		if ((req.getScheme().equals("http") && (req.getServerPort() != 80)) || (req.getScheme().equals("https") && (req.getServerPort() != 443))) {
			builder.append(":");
			builder.append(req.getServerPort());
		}
		builder.append(req.getContextPath());
		builder.append(req.getServletPath());
		return builder;
	}

	/**
	 * Convenience method for returning the path info
	 * 
	 * @param req
	 * @return
	 */
	protected static IPath getPathInfo(final HttpServletRequest req) {
		final String pathInfo = req.getPathInfo();
		return null != pathInfo ? new Path(pathInfo).makeAbsolute() : Path.ROOT;
	}

	private final JsonFactory jsonFactory = new MappingJsonFactory();

	private final IRuntimeContext context;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public BaseAPIServlet(final IRuntimeContext context) {
		this.context = context;
	}

	/**
	 * Prints a help text.
	 * 
	 * @param req
	 * @param resp
	 */
	private void doHelp(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		// check cache headers
		if (req.getDateHeader(IF_MODIFIED_SINCE) == helpModifiedTs) {
			resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}

		// render help text
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType(CONTENT_TYPE_TEXT);
		resp.setCharacterEncoding(UTF_8);
		resp.setDateHeader(LAST_MODIFIED, helpModifiedTs);
		resp.setHeader(CACHE_CONTROL, "max-age=360000,public");

		final PrintWriter writer = resp.getWriter();
		writer.println("Servlet Usage");
		writer.println("=============");
		writer.println();
		printHelpText(writer, getBaseUrl(req).toString());
		writer.println();
		writer.println();
		writer.println("Debug Parameters");
		writer.println("----------------");
		writer.println();
		writer.println(PARAM_HELP + " ... print this help text");
		writer.println(PARAM_TEXT + " ... send response as plain text");
		writer.flush();
	}

	/**
	 * Returns the context.
	 * 
	 * @return the context
	 */
	public IRuntimeContext getContext() {
		return context;
	}

	/**
	 * Returns the shared JSON factory.
	 * 
	 * @return a shared {@link MappingJsonFactory} instance
	 */
	protected JsonFactory getJsonFactory() {
		return jsonFactory;
	}

	@Override
	public String getServletInfo() {
		return "BugSearch HTTP API " + getClass().getSimpleName() + " in " + context.getContextPath().toString();
	}

	/**
	 * Creates and returns a {@link JsonGenerator} that either writes pretty
	 * printed {@link #CONTENT_TYPE_TEXT plain text} or
	 * {@link #CONTENT_TYPE_JSON JSON} depending on the request {@code text}
	 * parameter.
	 * <p>
	 * The response's character encoding is set to UTF-8.
	 * </p>
	 * 
	 * @param req
	 *            the HTTP request
	 * @param resp
	 *            the HTTP response to which the generator will write
	 * @return a Jackson generator for writing JSON
	 * @throws IOException
	 */
	protected JsonGenerator prepareForJson(final HttpServletRequest req, final HttpServletResponse resp, final int maxAgeSeconds) throws IOException {
		final boolean plainTextWanted = req.getParameter(PARAM_TEXT) != null;
		if (plainTextWanted) {
			resp.setContentType(CONTENT_TYPE_TEXT);
		} else {
			resp.setContentType(CONTENT_TYPE_JSON);
		}
		resp.setCharacterEncoding(UTF_8);

		// enable some basic caching (15min)
		if (maxAgeSeconds > 0) {
			resp.setHeader("Cache-Control", "max-age=900, public");
		}

		final JsonGenerator generator = jsonFactory.createJsonGenerator(resp.getWriter());

		if (plainTextWanted) {
			generator.useDefaultPrettyPrinter();
		}

		return generator;
	}

	/**
	 * Prints a help text to the specified writer.
	 * <p>
	 * The default implementation does nothing. Subclass should override and
	 * print usage info for web developers.
	 * </p>
	 * 
	 * @param writer
	 *            the writer
	 * @param baseUrl
	 *            the url for accessing the servlet (see
	 *            {@link #getBaseUrl(HttpServletRequest)})
	 */
	protected void printHelpText(final PrintWriter writer, final String baseUrl) {
		// empty
	}

	@Override
	protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		if (BugSearchDebug.httpApiRequests) {
			LOG.debug("[API REQUEST] {} {}{}", new Object[] { req.getMethod(), req.getServletPath(), req.getPathInfo() });
		}

		if ("GET".equals(req.getMethod()) && (req.getParameter(PARAM_HELP) != null)) {
			doHelp(req, resp);
			if (BugSearchDebug.httpApiRequests) {
				LOG.debug("[API RESPONSE] {} {}{}: {}", new Object[] { req.getMethod(), req.getServletPath(), req.getPathInfo(), "HELP" });
			}
			return;
		}

		// process
		try {
			super.service(req, resp);
			if (BugSearchDebug.httpApiRequests) {
				LOG.debug("[API RESPONSE] {} {}{}: {}", new Object[] { req.getMethod(), req.getServletPath(), req.getPathInfo(), resp });
			}
		} catch (final IllegalStateException e) {
			// IllegalStateException are typically used in Gyrex to indicate that something isn't ready
			// we convert it into UnavailableException to allow recovering on a dynamic platform
			if (BugSearchDebug.httpApiRequests) {
				LOG.debug("[API EXCEPTION] {} {}{}: {}", new Object[] { req.getMethod(), req.getServletPath(), req.getPathInfo(), ExceptionUtils.getMessage(e) });
			}
			throw new UnavailableException(e.getMessage(), 5); // TODO make configurable
		} catch (final IOException e) {
			// log and re-throw
			if (BugSearchDebug.httpApiRequests) {
				LOG.debug("[API EXCEPTION] {} {}{}: {}", new Object[] { req.getMethod(), req.getServletPath(), req.getPathInfo(), ExceptionUtils.getMessage(e) });
			}
			throw e;
		} catch (final ServletException e) {
			// log and re-throw
			if (BugSearchDebug.httpApiRequests) {
				LOG.debug("[API EXCEPTION] {} {}{}: {}", new Object[] { req.getMethod(), req.getServletPath(), req.getPathInfo(), ExceptionUtils.getMessage(e) });
			}
			throw e;
		} catch (final RuntimeException e) {
			// log and re-throw
			if (BugSearchDebug.httpApiRequests) {
				LOG.debug("[API EXCEPTION] {} {}{}: {}", new Object[] { req.getMethod(), req.getServletPath(), req.getPathInfo(), ExceptionUtils.getMessage(e) });
			}
			throw e;
		}
	}
}
