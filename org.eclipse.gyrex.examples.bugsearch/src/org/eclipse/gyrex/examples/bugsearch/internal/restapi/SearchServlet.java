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
package org.eclipse.gyrex.examples.bugsearch.internal.restapi;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.model.common.ModelUtil;
import org.eclipse.gyrex.search.ISearchService;
import org.eclipse.gyrex.search.documents.IDocument;
import org.eclipse.gyrex.search.documents.IDocumentManager;
import org.eclipse.gyrex.search.query.IQuery;
import org.eclipse.gyrex.search.result.IResult;
import org.eclipse.gyrex.services.common.ServiceUtil;

import org.eclipse.core.runtime.IPath;

import org.codehaus.jackson.JsonGenerator;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet for searching bugs in a rest style approach.
 */
public class SearchServlet extends BaseAPIServlet {

	private static final Logger QUERY_LOG = LoggerFactory.getLogger("bugsearch.querylog");

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public SearchServlet(final IRuntimeContext context) {
		super(context);
	}

	private Bug asBug(final IDocument document) {
		return new Bug(document);
	}

	private SearchResult asBugSearchResult(final IResult result) {
		return new SearchResult(result);
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final IPath path = getPathInfo(req);

		if (path.segmentCount() == 0) {
			doSearch(req, resp);
		} else if (path.segmentCount() == 1) {
			doGetBug(path.segment(0), req, resp);
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private void doGetBug(final String bugId, final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		final IDocumentManager manager = ModelUtil.getManager(IDocumentManager.class, getContext());
		final IDocument document = manager.findById(bugId);
		if (document == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		final Bug bug = asBug(document);

		final JsonGenerator generator = prepareForJson(req, resp, 900);
		generator.writeObject(bug);
		generator.close();
	}

	private void doSearch(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		final ISearchService searchService = ServiceUtil.getService(ISearchService.class, getContext());

		final String queryString = req.getParameter("q");

		final IQuery query = searchService.createQuery();
		if (StringUtils.isNotBlank(queryString)) {
			query.setQuery(queryString);
		}

		QUERY_LOG.info(query.toString());
		final SearchResult result = asBugSearchResult(searchService.findByQuery(query));

		final JsonGenerator generator = prepareForJson(req, resp, 900);
		generator.writeObject(result);
		generator.close();
	}

	@Override
	protected void printHelpText(final PrintWriter writer, final String baseUrl) {
		writer.print("    Retrieve all bugs: ");
		writer.println(baseUrl);
		writer.print("Retrieve a single bug: ");
		writer.println(baseUrl.concat("<number>"));
		writer.println();
		writer.println();
		writer.println("Search/Guided Navigation Parameters");
		writer.println("-----------------------------------");
		writer.println();
		writer.println("q ... the query string");
		writer.println("      (see JavaDoc of org.eclipse.gyrex.cds.service.query.ListingQuery#setQuery(String))");
		writer.println("f ... a filter query (multiple possible, will be interpreted as AND; ..&f=..&f=..)");
		writer.println("      (eg. the facet 'filter' attribute from the result set)");
		writer.println("      (see JavaDoc of org.eclipse.gyrex.cds.service.query.ListingQuery#setAdvancedQuery(String) for escaping rules)");
		writer.println("s ... start index (zero-based, used for paging)");
		writer.println("r ... rows to return (defaults to 50, used for paging)");
		writer.println();
	}
}
