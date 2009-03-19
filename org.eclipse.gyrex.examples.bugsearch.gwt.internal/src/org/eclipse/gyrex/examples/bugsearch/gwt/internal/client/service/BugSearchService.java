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

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The main BugSearch service
 */
@RemoteServiceRelativePath(BugSearchService.ENTRYPOINT_SERVICE)
public interface BugSearchService extends RemoteService {

	/** the configuration service entry point */
	String ENTRYPOINT_SERVICE = "services/bugs/search";

	BugList findBugs(String query, Map<String, List<String>> filters);

}
