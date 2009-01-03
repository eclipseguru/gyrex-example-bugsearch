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
package org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BugListFilter implements IsSerializable {

	private String id;
	private String label;
	private List<BugListFilterValue> values;

	/**
	 * Creates a new instance.
	 */
	private BugListFilter() {
		// empty
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param label
	 */
	public BugListFilter(final String id, final String label) {
		this();
		if (null != id) {
			this.id = id;
		}
		if (null != label) {
			this.label = label;
		}
	}

	/**
	 * @param value
	 * @param count
	 */
	public void addValue(final BugListFilterValue value) {
		if (null == values) {
			values = new ArrayList<BugListFilterValue>();
		}
		values.add(value);
	}

	/**
	 * Returns the id.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the label.
	 * 
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Returns the values.
	 * 
	 * @return the values
	 */
	public List<BugListFilterValue> getValues() {
		if (null == values) {
			return Collections.emptyList();
		}
		return values;
	}

	/**
	 * @param array
	 */
	public void setValues(final BugListFilterValue[] values) {
		this.values = new ArrayList<BugListFilterValue>(Arrays.asList(values));
	}

}