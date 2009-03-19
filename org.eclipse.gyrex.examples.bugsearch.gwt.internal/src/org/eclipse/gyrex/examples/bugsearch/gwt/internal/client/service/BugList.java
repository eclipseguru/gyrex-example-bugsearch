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
package org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * The bug list.
 */
public class BugList implements IsSerializable {

	private long numFound;
	private long queryTime;

	private List<BugListFilter> filters;
	private List<Bug> bugs;
	private Map<String, List<String>> activeFilters;

	public void addBug(final Bug bug) {
		if (null == bugs) {
			bugs = new ArrayList<Bug>();
		}
		bugs.add(bug);
	}

	/**
	 * @param id
	 * @param label
	 */
	public void addFilter(final BugListFilter filter) {
		if (null == filters) {
			filters = new ArrayList<BugListFilter>();
		}
		filters.add(filter);

	}

	/**
	 * Returns the activeFilters.
	 * 
	 * @return the activeFilters
	 */
	public Map<String, List<String>> getActiveFilters() {
		return activeFilters;
	}

	/**
	 * Returns the bugs.
	 * 
	 * @return the bugs
	 */
	public List<Bug> getBugs() {
		if (null == bugs) {
			return Collections.emptyList();
		}

		return bugs;
	}

	/**
	 * Returns the filters.
	 * 
	 * @return the filters
	 */
	public List<BugListFilter> getFilters() {
		if (null == filters) {
			return Collections.emptyList();
		}
		return filters;
	}

	/**
	 * Returns the numFound.
	 * 
	 * @return the numFound
	 */
	public long getNumFound() {
		return numFound;
	}

	/**
	 * Returns the queryTime.
	 * 
	 * @return the queryTime
	 */
	public long getQueryTime() {
		return queryTime;
	}

	public void setActiveFilters(final Map<String, List<String>> activeFilters) {
		this.activeFilters = activeFilters;
	}

	/**
	 * @param numFound
	 */
	public void setNumFound(final long numFound) {
		this.numFound = numFound;
	}

	/**
	 * @param queryTime
	 */
	public void setQueryTime(final long queryTime) {
		this.queryTime = queryTime;
	}

}
