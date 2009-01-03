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
package org.eclipse.cloudfree.examples.bugsearch.internal.app;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.eclipse.cloudfree.examples.bugsearch.internal.BugSearchActivator;
import org.eclipse.equinox.jsp.jasper.JspServlet;

public class SolrAdminJspServlet extends JspServlet {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;
	private final CoreContainer coreContainer;
	private final String coreName;

	public SolrAdminJspServlet(final String alias, final CoreContainer coreContainer, final String coreName) {
		super(BugSearchActivator.getInstance().getBundle("org.apache.solr.servlet"), "/web", alias);
		this.coreContainer = coreContainer;
		this.coreName = coreName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.jsp.jasper.JspServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final SolrCore core = coreContainer.getCore(coreName);
		request.setAttribute("org.apache.solr.SolrCore", core);
		try {
			super.service(request, response);
		} finally {
			core.close();
		}
	}
}
