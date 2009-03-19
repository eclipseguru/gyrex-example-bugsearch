/*******************************************************************************
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.examples.bugsearch.internal.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.WeakHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.BinaryQueryResponseWriter;
import org.apache.solr.request.QueryResponseWriter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.servlet.SolrRequestParsers;
import org.apache.solr.servlet.cache.HttpCacheHeaderUtil;
import org.apache.solr.servlet.cache.Method;
import org.eclipse.gyrex.http.application.ApplicationException;

public class SolrServlet extends HttpServlet {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	private final String pathPrefix;
	private final CoreContainer coreContainer;
	private final String coreName;

	protected final WeakHashMap<SolrCore, SolrRequestParsers> parsers = new WeakHashMap<SolrCore, SolrRequestParsers>();

	/**
	 * Creates a new instance.
	 * 
	 * @param pathPrefix
	 */
	public SolrServlet(final String pathPrefix, final CoreContainer coreContainer, final String coreName) {
		this.pathPrefix = pathPrefix;
		this.coreContainer = coreContainer;
		this.coreName = coreName;
	}

	protected void execute(final HttpServletRequest req, final SolrRequestHandler handler, final SolrQueryRequest sreq, final SolrQueryResponse rsp) {
		// a custom filter could add more stuff to the request before passing it on.
		// for example: sreq.getContext().put( "HttpServletRequest", req );
		// used for logging query stats in SolrCore.execute()
		sreq.getContext().put("webapp", req.getContextPath());
		sreq.getCore().execute(handler, sreq, rsp);
	}

	@Override
	protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

		SolrRequestHandler handler = null;
		SolrQueryRequest solrReq = null;
		SolrCore core = null;
		try {
			String path = req.getServletPath();
			if (req.getPathInfo() != null) {
				// this lets you handle /update/commit when /update is a servlet
				path += req.getPathInfo();
			}
			if ((pathPrefix != null) && path.startsWith(pathPrefix)) {
				path = path.substring(pathPrefix.length());
			}

			// get the core
			core = coreContainer.getCore(coreName);
			if (core != null) {
				final SolrConfig config = core.getSolrConfig();
				// get or create/cache the parser for the core
				SolrRequestParsers parser = null;
				parser = parsers.get(core);
				if (parser == null) {
					parser = new SolrRequestParsers(config);
					parsers.put(core, parser);
				}

				// Determine the handler from the url path if not set
				// (we might already have selected the cores handler)
				if ((handler == null) && (path.length() > 1)) { // don't match "" or "/" as valid path
					handler = core.getRequestHandler(path);
					// no handler yet but allowed to handle select; let's check
					if ((handler == null) && parser.isHandleSelect()) {
						if ("/select".equals(path) || "/select/".equals(path)) {
							solrReq = parser.parse(core, path, req);
							final String qt = solrReq.getParams().get(CommonParams.QT);
							if ((qt != null) && qt.startsWith("/")) {
								throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Invalid query type.  Do not use /select to access: " + qt);
							}
							handler = core.getRequestHandler(qt);
							if (handler == null) {
								throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "unknown handler: " + qt);
							}
						}
					}
				}

				if (handler == null) {
					resp.sendError(404, "Handler Not Found");
					return;
				}
				// if not a /select, create the request
				if (solrReq == null) {
					solrReq = parser.parse(core, path, req);
				}

				final Method reqMethod = Method.getMethod(req.getMethod());
				HttpCacheHeaderUtil.setCacheControlHeader(config, resp, reqMethod);
				// unless we have been explicitly told not to, do cache validation
				// if we fail cache validation, execute the query
				if (config.getHttpCachingConfig().isNever304() || !HttpCacheHeaderUtil.doCacheHeaderValidation(solrReq, req, reqMethod, resp)) {
					final SolrQueryResponse solrRsp = new SolrQueryResponse();
					/* even for HEAD requests, we need to execute the handler to
					 * ensure we don't get an error (and to make sure the correct
					 * QueryResponseWriter is selectedand we get the correct
					 * Content-Type)
					 */
					execute(req, handler, solrReq, solrRsp);
					HttpCacheHeaderUtil.checkHttpCachingVeto(solrRsp, resp, reqMethod);
					if (solrRsp.getException() != null) {
						throw new ApplicationException(solrRsp.getException());
					} else {
						// Now write it out
						final QueryResponseWriter responseWriter = core.getQueryResponseWriter(solrReq);
						resp.setContentType(responseWriter.getContentType(solrReq, solrRsp));
						if (Method.HEAD != reqMethod) {
							if (responseWriter instanceof BinaryQueryResponseWriter) {
								final BinaryQueryResponseWriter binWriter = (BinaryQueryResponseWriter) responseWriter;
								binWriter.write(resp.getOutputStream(), solrReq, solrRsp);
							} else {
								final PrintWriter out = resp.getWriter();
								responseWriter.write(out, solrReq, solrRsp);

							}

						}
						//else http HEAD request, nothing to write out, waited this long just to get RepositoryContentType
					}
				}
				return; // we are done with a valid handler
			}
		} catch (final Exception e) {
			throw new ApplicationException(e);
		} finally {
			if (solrReq != null) {
				solrReq.close();
			}
			if (core != null) {
				core.close();
			}
		}
	}
}
