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
package org.eclipse.gyrex.examples.bugsearch.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Shared constants for wiring of BugSearch to the Eclipse BugSearch
 * application.
 */
public interface IEclipseBugSearchConstants {

	public static final IPath CONTEXT_PATH = new Path("/org.eclipse.gyrex.examples/bugsearch/eclipse");
	public static final String DEFAULT_URL = "http:///bugsearch/";
	public static final String KEY_URL = "url";
	public static final String APPLICATION_ID = "eclipsebugsearch";
	public static final String REPOSITORY_ID = "eclipsebugsearch.listings";

}
