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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.examples.bugsearch.internal.BugSearchActivator;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;

/**
 * The application provider for the extensible Fan Shop application.
 */
public class BugSearchApplicationProvider extends ApplicationProvider {

	public static final String ID = BugSearchActivator.PLUGIN_ID + ".application.provider";

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 */
	public BugSearchApplicationProvider() {
		super(ID);
	}

	@Override
	public Application createApplication(final String applicationId, final IRuntimeContext context) throws CoreException {
		return new BugSearchApplication(applicationId, context);
	}

}
