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
import org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.service.BugSearchService;
import org.eclipse.gyrex.examples.bugsearch.internal.BugSearchActivator;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.registry.internal.BundleResourceProvider;

import org.eclipse.core.runtime.CoreException;

/**
 * A fan shop application instance.
 */
public class BugSearchApplication extends Application {

	BugSearchApplication(final String id, final IRuntimeContext context) {
		super(id, context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.http.application.Application#doInit()
	 */
	@Override
	protected void doInit() throws CoreException {
		try {
			// register the front end and its services
			getApplicationServiceSupport().registerResources("/", "frontend", new BundleResourceProvider(BugSearchActivator.getInstance().getBundle("org.eclipse.gyrex.examples.bugsearch.gwt.internal")));
			getApplicationServiceSupport().registerServlet("/" + BugSearchService.ENTRYPOINT_SERVICE, new BugSearchServiceServlet(getContext()), null);

			// register the  listing servlet
			//getApplicationServiceSupport().registerServlet("/", new ListingServlet(getContext()), null);
			getApplicationServiceSupport().registerServlet("/query", new BugSearchRestServlet(getContext()), null);
		} catch (final ServletException e) {
			throw new CoreException(BugSearchActivator.getInstance().getStatusUtil().createError(0, e.getMessage(), e));
		}

	}
}
