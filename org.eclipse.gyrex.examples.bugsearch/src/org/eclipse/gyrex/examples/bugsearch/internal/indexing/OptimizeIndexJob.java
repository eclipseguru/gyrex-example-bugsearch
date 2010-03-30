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
package org.eclipse.gyrex.examples.bugsearch.internal.indexing;

import org.eclipse.gyrex.cds.model.IListingManager;
import org.eclipse.gyrex.cds.model.solr.internal.SolrListingsManager;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.examples.bugsearch.internal.BugSearchActivator;
import org.eclipse.gyrex.model.common.ModelUtil;
import org.eclipse.gyrex.persistence.solr.internal.SolrRepository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.osgi.framework.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class OptimizeIndexJob extends Job {

	private static final Logger LOG = LoggerFactory.getLogger(OptimizeIndexJob.class);
	private final IRuntimeContext context;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 * @param name
	 */
	public OptimizeIndexJob(final IRuntimeContext context) {
		super("fan shop index optimize");
		this.context = context;
		setPriority(LONG);
		setRule(new MutexRule(context.getContextPath().toString().intern()));
	}

	/**
	 * Returns the context.
	 * 
	 * @return the context
	 */
	public IRuntimeContext getContext() {
		return context;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {

			final Bundle bundle = BugSearchActivator.getInstance().getBundle();
			if (null == bundle) {
				// abort, bundle is inactive
				return Status.CANCEL_STATUS;
			}

			final IListingManager listingManager = ModelUtil.getManager(IListingManager.class, getContext());
			final SolrRepository solrRepository = (SolrRepository) ((SolrListingsManager) listingManager).getAdapter(SolrRepository.class);
			if (null == solrRepository) {
				return Status.CANCEL_STATUS;
			}

			LOG.debug("Begin optimization.");

			// optimize
			solrRepository.optimize(true, true);

			LOG.debug("Optimization finished.");

		} catch (final IllegalStateException e) {
			// abort, bundle is inactive
			LOG.warn("Something is missing, cancelling job.", e);
			return Status.CANCEL_STATUS;
		} catch (final Exception e) {
			e.printStackTrace();
			return BugSearchActivator.getInstance().getStatusUtil().createError(0, e.getMessage(), e);
		}

		return Status.OK_STATUS;
	}
}
