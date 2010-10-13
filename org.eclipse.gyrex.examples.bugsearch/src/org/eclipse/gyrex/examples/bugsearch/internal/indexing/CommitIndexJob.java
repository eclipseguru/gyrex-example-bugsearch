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

import org.eclipse.gyrex.cds.IListingManager;
import org.eclipse.gyrex.cds.solr.internal.SolrListingsManager;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.examples.bugsearch.internal.BugSearchActivator;
import org.eclipse.gyrex.model.common.ModelUtil;
import org.eclipse.gyrex.persistence.solr.internal.SolrRepository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.eclipse.mylyn.tasks.core.TaskRepository;

import org.osgi.framework.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class CommitIndexJob extends BugSearchIndexJob {

	private static final Logger LOG = LoggerFactory.getLogger(CommitIndexJob.class);

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 * @param name
	 */
	public CommitIndexJob(final IRuntimeContext context) {
		super("commit index", context);
		setPriority(LONG);
		setRule(new MutexRule(context.getContextPath().toString().intern()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.examples.bugsearch.internal.indexing.BugSearchIndexJob#doIndex(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.mylyn.tasks.core.TaskRepository, org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryConnector, org.eclipse.gyrex.examples.bugsearch.internal.indexing.DocumentsPublisher)
	 */
	@Override
	protected void doIndex(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher) {
		// not used
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

			LOG.debug("Begin commit.");

			// optimize
			solrRepository.commit(true, true);

			LOG.debug("Commit finished.");

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

	@Override
	public String toString() {
		return super.toString() + " " + getJobStateAsString(getState());
	}
}
