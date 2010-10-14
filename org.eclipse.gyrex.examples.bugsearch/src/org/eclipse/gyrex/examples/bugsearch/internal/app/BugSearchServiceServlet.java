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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.gyrex.cds.IListingService;
import org.eclipse.gyrex.cds.documents.IDocument;
import org.eclipse.gyrex.cds.query.ListingQuery;
import org.eclipse.gyrex.cds.result.IListingResult;
import org.eclipse.gyrex.cds.result.IListingResultFacet;
import org.eclipse.gyrex.cds.result.IListingResultFacetValue;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.service.Bug;
import org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.service.BugList;
import org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.service.BugListFilter;
import org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.service.BugListFilterValue;
import org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.service.BugSearchService;
import org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.service.ValueAscendingComparator;
import org.eclipse.gyrex.services.common.ServiceUtil;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

/**
 *
 */
public class BugSearchServiceServlet extends RemoteServiceServlet implements BugSearchService {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	private final IRuntimeContext context;

	private static final Logger LOG = LoggerFactory.getLogger(BugSearchServiceServlet.class);
	private static final Logger QUERY_LOG = LoggerFactory.getLogger("bugsearch.querylog");

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public BugSearchServiceServlet(final IRuntimeContext context) {
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see com.google.gwt.user.server.rpc.RemoteServiceServlet#doGetSerializationPolicy(javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.String)
	 */
	@Override
	protected SerializationPolicy doGetSerializationPolicy(final HttpServletRequest request, final String moduleBaseUrl, final String strongName) {
		//		final String mountPoint = (String) request.getAttribute(IApplicationConstants.REQUEST_ATTRIBUTE_MOUNT_POINT);
		//		if (!mountPoint.startsWith(moduleBaseUrl)) {
		//			BundleDebugOptions.debug("Module path outside application mount, this is not supported!");
		//			return null;
		//		}

		// get module path
		String modulePath = null;
		if (moduleBaseUrl != null) {
			try {
				modulePath = new URL(moduleBaseUrl).getPath();
			} catch (final MalformedURLException ex) {
				return null;
			}
		}

		// strip context path from module path
		final String contextPath = request.getContextPath();
		if ((modulePath == null) || !modulePath.startsWith(contextPath)) {
			LOG.trace("Module path outside application mount, this is not supported!");
			return null;
		}
		final String contextRelativePath = modulePath.substring(contextPath.length());

		// load the policy
		final String serializationPolicyFilePath = SerializationPolicyLoader.getSerializationPolicyFileName(contextRelativePath + strongName);
		final InputStream is = getServletContext().getResourceAsStream(serializationPolicyFilePath);
		try {
			if (is != null) {
				try {
					final List<ClassNotFoundException> exceptions = new ArrayList<ClassNotFoundException>();
					final SerializationPolicy serializationPolicy = SerializationPolicyLoader.loadFromStream(is, exceptions);
					if (!exceptions.isEmpty()) {
						for (final ClassNotFoundException classNotFoundException : exceptions) {
							LOG.trace("Error while loading serialization policy!", classNotFoundException);
						}
					}
					return serializationPolicy;
				} catch (final ParseException e) {
					getServletContext().log("ERROR: Failed to parse the policy file '" + serializationPolicyFilePath + "'", e);
				} catch (final IOException e) {
					getServletContext().log("ERROR: Could not read the policy file '" + serializationPolicyFilePath + "'", e);
				}
			} else {
				final String message = "ERROR: The serialization policy file '" + serializationPolicyFilePath + "' was not found; did you forget to include it in this deployment?";
				getServletContext().log(message);
			}
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (final IOException e) {
					// ignores
				}
			}
		}

		// fallback to legacy
		return RPC.getDefaultSerializationPolicy();
	}

	@Override
	public BugList findBugs(final String query, final Map<String, List<String>> filters) {
		final IListingService service = ServiceUtil.getService(IListingService.class, context);

		// the result
		final BugList bugList = new BugList();

		// the query
		final ListingQuery listingQuery = new ListingQuery();
		if (null != query) {
			listingQuery.setQuery(query);
		}
		listingQuery.setMaxResults(20);

		// filters
		if ((null != filters) && !filters.isEmpty()) {
			bugList.setActiveFilters(filters);
			final StringBuilder filterQuery = new StringBuilder(60);
			for (final Entry<String, List<String>> filter : filters.entrySet()) {
				filterQuery.setLength(0);
				filterQuery.append(ListingQuery.escapeQueryChars(filter.getKey()));
				filterQuery.append(":(");
				final List<String> values = filter.getValue();
				int i = 0;
				for (final String value : values) {
					if (i > 0) {
						filterQuery.append(' ');
					}
					filterQuery.append(ListingQuery.escapeQueryChars(value));
					i++;
				}
				filterQuery.append(')');
				listingQuery.addFilterQuery(filterQuery.toString());
			}
		}

		QUERY_LOG.info(listingQuery.toString());
		final IListingResult result = service.findListings(listingQuery);

		bugList.setNumFound(result.getNumFound());
		bugList.setQueryTime(result.getQueryTime());

		for (final IListingResultFacet facet : result.getFacets()) {
			final String id = facet.getId();
			// ignore some filters
			if (id.equals("cc") || id.equals("statusWhiteboard")) {
				continue;
			}
			final BugListFilter filter = new BugListFilter(id, facet.getLabel());
			final SortedSet<BugListFilterValue> values = new TreeSet<BugListFilterValue>(ValueAscendingComparator.INSTANCE);
			for (final IListingResultFacetValue facetValue : facet.getValues()) {
				// ignore empty string
				final String value = facetValue.getValue();
				if (value.trim().length() == 0) {
					continue;
				}
				// ignore 0 values
				final long count = facetValue.getCount();
				if (count <= 0) {
					continue;
				}
				values.add(new BugListFilterValue(value, count));
			}
			filter.setValues(values.toArray(new BugListFilterValue[values.size()]));
			bugList.addFilter(filter);
		}

		for (final IDocument listing : result.getListings()) {
			final int bugNum = NumberUtils.toInt(listing.getId(), 0);
			if (bugNum <= 0) {
				continue;
			}
			final Bug bug = new Bug(bugNum);
			bug.setSummary(listing.getTitle());
			bug.setProduct((String) listing.getAttribute("product").getValues()[0]);
			bug.setScore(((Float) listing.getAttribute("score").getValues()[0]).floatValue());
			bugList.addBug(bug);
		}

		return bugList;
	}

	/* (non-Javadoc)
	 * @see com.google.gwt.user.server.rpc.RemoteServiceServlet#processCall(java.lang.String)
	 */
	@Override
	public String processCall(final String payload) throws SerializationException {
		final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			return super.processCall(payload);
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}
}
