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

import org.eclipse.gyrex.search.result.IResult;

/**
 * JSON Wrapper for {@link IResult}.
 */
public class SearchResult {

	private final IResult result;

	/**
	 * Creates a new instance.
	 * 
	 * @param result
	 */
	public SearchResult(final IResult result) {
		this.result = result;
	}

	/**
	 * @return
	 * @see org.eclipse.gyrex.search.result.IResult#getNumFound()
	 */
	public long getNumFound() {
		return result.getNumFound();
	}

	/**
	 * @return
	 * @see org.eclipse.gyrex.search.result.IResult#getQueryTime()
	 */
	public long getQueryTime() {
		return result.getQueryTime();
	}

	/**
	 * @return
	 * @see org.eclipse.gyrex.search.result.IResult#getStartOffset()
	 */
	public long getStartOffset() {
		return result.getStartOffset();
	}

}
