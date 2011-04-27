/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.examples.bugsearch.internal.restapi;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.context.IRuntimeContext;

import org.eclipse.core.runtime.IPath;

import org.codehaus.jackson.JsonGenerator;

/**
 *
 */
public class StatsServlet extends BaseAPIServlet {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public StatsServlet(final IRuntimeContext context) {
		super(context);
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final IPath path = getPathInfo(req);

		if (path.segmentCount() == 0) {
			doGetStats(req, resp);
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private void doGetStats(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		final Stats stats = new Stats();
		final JsonGenerator generator = prepareForJson(req, resp, 900);
		generator.writeObject(stats);
		generator.close();
	}
}
