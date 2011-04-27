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

import javax.servlet.ServletException;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.examples.bugsearch.internal.BugSearchActivator;
import org.eclipse.gyrex.examples.bugsearch.internal.restapi.SearchServlet;
import org.eclipse.gyrex.http.application.Application;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * A fan shop application instance.
 */
public class BugSearchApplication extends Application {

	BugSearchApplication(final String id, final IRuntimeContext context) {
		super(id, context);
	}

	@Override
	protected void doInit() throws CoreException {
		try {
			// register the API servlets
			getApplicationServiceSupport().registerServlet("/search", new SearchServlet(getContext()), null);
		} catch (final ServletException e) {
			throw new CoreException(new Status(IStatus.ERROR, BugSearchActivator.PLUGIN_ID, e.getMessage(), e));
		}

	}
}
