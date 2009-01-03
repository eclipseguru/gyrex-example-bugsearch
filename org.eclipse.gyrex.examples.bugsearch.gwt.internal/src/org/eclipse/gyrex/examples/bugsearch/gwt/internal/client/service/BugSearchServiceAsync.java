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

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The main BugSearch service
 * @generated generated asynchronous callback interface to be used on the client side
 */
public interface BugSearchServiceAsync {

	/**
	 * @param  callback the callback that will be called to receive the return value
	 * @generated generated method with asynchronous callback parameter to be used on the client side
	 */
	void findBugs(String query, Map<String, List<String>> filters, AsyncCallback<BugList> callback);

}
