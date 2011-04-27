/*******************************************************************************
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.examples.bugsearch.internal.restapi;

import org.eclipse.gyrex.search.documents.IDocument;

import org.apache.commons.lang.math.NumberUtils;

/**
 * A bug entry
 */
public class Bug {

	private final IDocument document;

	/**
	 * Creates a new instance.
	 * 
	 * @param document
	 */
	public Bug(final IDocument document) {
		this.document = document;
	}

	/**
	 * @return
	 * @see org.eclipse.gyrex.search.documents.IDocument#getDescription()
	 */
	public String getDescription() {
		return document.getDescription();
	}

	/**
	 * @return
	 * @see org.eclipse.gyrex.search.documents.IDocument#getId()
	 */
	public Integer getId() {
		final int id = NumberUtils.toInt(document.getId());
		if (id > 0) {
			return id;
		}
		return null;
	}

	/**
	 * @return
	 * @see org.eclipse.gyrex.search.documents.IDocument#getSummary()
	 */
	public String getSummary() {
		return document.getSummary();
	}

	/**
	 * @return
	 * @see org.eclipse.gyrex.search.documents.IDocument#getTitle()
	 */
	public String getTitle() {
		return document.getTitle();
	}
}
