/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.History;

public class BugSearchHistoryManager {

	private static final String QUERY_PREFIX = "q-";

	private Map<String, List<String>> activeFilters;

	private String query;

	private String escape(final String string) {
		return URL.encodeComponent(string).replaceAll("_", "__").replaceAll("-", "--");
	}

	private String generateHistoryToken() {
		if (null == activeFilters) {
			return null;
		}

		final StringBuilder historyToken = new StringBuilder();

		// filters
		for (final Entry<String, List<String>> entry : activeFilters.entrySet()) {
			if (historyToken.length() > 0) {
				historyToken.append("/");
			}
			historyToken.append(escape(entry.getKey())).append("-");
			int count = 0;
			for (final String value : entry.getValue()) {
				if (count > 0) {
					historyToken.append("_");
				}
				historyToken.append(escape(value));
				count++;
			}
		}

		// query
		if ((null != query) && (query.length() > 0)) {
			if (historyToken.length() > 0) {
				historyToken.append("/");
			}
			historyToken.append(QUERY_PREFIX).append(URL.encodeComponent(query));
		}

		return historyToken.toString();
	}

	public Map<String, List<String>> getActiveFilters() {
		if (null == activeFilters) {
			activeFilters = new LinkedHashMap<String, List<String>>();
		}
		return activeFilters;
	}

	/**
	 * Returns the query.
	 * 
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}

	private void onFilterUpdate(final String historyToken) {
		History.newItem(historyToken);
	}

	private void parseFilter(final String filterString) {
		try {
			final char[] cs = filterString.toCharArray();
			final StringBuilder id = new StringBuilder();
			final StringBuilder value = new StringBuilder();
			String filterId = null;
			final List<String> filterValues = new ArrayList<String>(1);
			boolean parseId = true;
			for (int i = 0; i < cs.length; i++) {
				final char c = cs[i];
				if (parseId) {
					if (c == '-') {
						// look ahead
						if (cs[i + 1] != '-') {
							// id finished, read values
							parseId = false;
							filterId = unescape(id.toString());
							continue;
						} else {
							// add both and continue
							i++;
							id.append("--");
							continue;
						}
					}
					id.append(c);
				} else {
					if (c == '_') {
						// look ahead
						if ((i == cs.length - 1) || (cs[i + 1] != '_')) {
							// next value
							filterValues.add(unescape(value.toString()));
							value.setLength(0);
							continue;
						} else {
							// add both and continue
							i++;
							value.append("__");
							continue;
						}
					}
					value.append(c);
				}
			}

			// don't forget last value
			if (value.length() > 0) {
				filterValues.add(unescape(value.toString()));
			}

			if ((null != filterId) && !filterValues.isEmpty()) {
				getActiveFilters().put(filterId, filterValues);
			}
		} catch (final IndexOutOfBoundsException e) {
			// invalid string;
		}

	}

	/**
	 * Activates a filter with the specified id and values.
	 * 
	 * @param id
	 * @param values
	 */
	public void setActiveFilter(final String id, final String... values) {

		// initialize active filters list if necessary
		final Map<String, List<String>> activeFilters = getActiveFilters();

		// remove if no values
		if ((null == values) || (values.length == 0)) {
			activeFilters.remove(id);
			return;
		}

		// copy values into list
		final List<String> valuesList = new ArrayList<String>(values.length);
		for (final String value : values) {
			valuesList.add(value);
		}

		// set active
		activeFilters.put(id, valuesList);
		onFilterUpdate(generateHistoryToken());
	}

	/**
	 * Sets the query.
	 * 
	 * @param query
	 *            the query to set
	 */
	public void setQuery(final String query) {
		this.query = query;
	}

	/**
	 * Activates or deactivates the filter with the specified id and value.
	 * 
	 * @param id
	 * @param value
	 * @return <code>true</code> if the filter was activated, <code>false</code>
	 *         otherwise
	 */
	public boolean toggleFilter(final String id, final String value) {

		// initialize active filters list if necessary
		final Map<String, List<String>> activeFilters = getActiveFilters();

		// get current values
		List<String> values = activeFilters.get(id);

		// initialize if necessary
		if (null == values) {
			values = new ArrayList<String>(1);
			activeFilters.put(id, values);
		}

		// remove or add filter
		if (values.contains(value)) {
			values.remove(value);
		} else {
			values.add(value);
		}

		// remove values if empty
		if (values.isEmpty()) {
			activeFilters.remove(id);
		}

		onFilterUpdate(generateHistoryToken());

		return false;
	}

	private String unescape(final String string) {
		return URL.decodeComponent(string.replaceAll("__", "_").replaceAll("--", "-"));
	}

	public void updateFromHistoryToken(final String historyToken) {
		activeFilters = null;
		query = null;

		if ((null == historyToken) || (historyToken.length() == 0)) {
			return;
		}
		final String[] filters = historyToken.split("/");
		for (final String filter : filters) {
			if (filter.startsWith(QUERY_PREFIX)) {
				query = URL.decodeComponent(filter.substring(QUERY_PREFIX.length()));
			} else {
				parseFilter(filter);
			}
		}
	}
}
