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

import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.context.IRuntimeContext;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.eclipse.mylyn.tasks.core.TaskRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class BugSearchDataImport extends BugSearchIndexJob {

	public static enum Mode {
		INITIAL, UPDATE
	}

	private static final Logger LOG = LoggerFactory.getLogger(BugSearchDataImport.class);

	public static final String NOW = "Now";
	private final Mode mode;
	private final long interval;
	private final TimeUnit unit;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 * @param name
	 */
	public BugSearchDataImport(final IRuntimeContext context, final Mode mode, final long interval, final TimeUnit unit) {
		super("fan shop data import", context);
		this.mode = mode;
		this.interval = interval;
		this.unit = unit;
	}

	@Override
	protected void doIndex(final IProgressMonitor monitor, final TaskRepository repository, final BugzillaRepositoryConnector connector, final DocumentsPublisher publisher) {
		switch (mode) {
			case INITIAL:
				LOG.debug("initial indexing");
				queryForAllBugs(monitor, repository, connector, publisher);
				break;

			case UPDATE:
				LOG.debug("updating index");
				queryForChanges(monitor, repository, connector, publisher, (1 + unit.toHours(interval)) + "h", NOW);
				break;
		}
	}
}
