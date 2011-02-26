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
package org.eclipse.gyrex.examples.bugsearch.internal.setup;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.gyrex.preferences.CloudScope;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// note, this package does not trigger bundle activation
// we must be careful to not reference classes (even constants) outside this package
public class BugSearchDevSetup {

	private static final Logger LOG = LoggerFactory.getLogger(BugSearchDevSetup.class);

	public static void enableServerRole() throws BackingStoreException {
		// enable required server roles
		//  - org.eclipse.gyrex.boot.role.admin.gwt
		//  - org.eclipse.gyrex.boot.role.http.registry
		//  - org.eclipse.gyrex.boot.role.http.jetty
		//  - org.eclipse.gyrex.examples.bugsearch
		try {
			final IEclipsePreferences preferences = new CloudScope().getNode("org.eclipse.gyrex.boot");
			String roles = preferences.get("rolesToStart", "");
			if (StringUtils.isNotBlank(roles)) {
				roles += ",";
			}
			roles += "org.eclipse.gyrex.boot.role.admin.gwt,org.eclipse.gyrex.boot.role.http.registry,org.eclipse.gyrex.boot.role.http.jetty,org.eclipse.gyrex.examples.bugsearch";
			preferences.put("rolesToStart", roles);
			preferences.flush();
		} catch (final Exception e) {
			LOG.error("Error while activating required server roles. " + e.getMessage(), e);
		}
	}

	private BugSearchDevSetup() {
	}

}
