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

/**
 * 
 */
public final class ValueAscendingComparator extends BugListFilterValueComparator {

	public static final ValueAscendingComparator INSTANCE = new ValueAscendingComparator();

	public int compare(final BugListFilterValue o1, final BugListFilterValue o2) {
		return o1.getValue().toLowerCase().compareTo(o2.getValue().toLowerCase());
	}
}