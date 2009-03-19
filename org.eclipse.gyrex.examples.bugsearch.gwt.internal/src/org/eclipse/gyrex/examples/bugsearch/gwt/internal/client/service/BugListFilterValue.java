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

import com.google.gwt.user.client.rpc.IsSerializable;

public class BugListFilterValue implements IsSerializable {

	private String value;
	private long count;

	private BugListFilterValue() {
		// empty (for GWT)
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param value
	 * @param count
	 */
	public BugListFilterValue(final String value, final long count) {
		this();
		if (null != value) {
			this.value = value;
		}
		if (count >= 0) {
			this.count = count;
		}
	}

	/**
	 * Returns the count.
	 * 
	 * @return the count
	 */
	public long getCount() {
		return count;
	}

	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

}