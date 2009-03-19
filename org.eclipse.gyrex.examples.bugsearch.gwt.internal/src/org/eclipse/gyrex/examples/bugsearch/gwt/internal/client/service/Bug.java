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
package org.eclipse.gyrex.examples.bugsearch.gwt.internal.client.service;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * 
 */
public class Bug implements IsSerializable {

	private String summary;
	private int id;
	private String product;
	private float score;

	/**
	 * Creates a new instance.
	 */
	private Bug() {
		// empty
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 */
	public Bug(final int id) {
		this();
		if (id > 0) {
			this.id = id;
		}
	}

	/**
	 * Returns the id.
	 * 
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the product.
	 * 
	 * @return the product
	 */
	public String getProduct() {
		if (null == product) {
			return "";
		}
		return product;
	}

	/**
	 * Returns the score.
	 * 
	 * @return the score
	 */
	public float getScore() {
		return score;
	}

	/**
	 * Returns the summary.
	 * 
	 * @return the summary
	 */
	public String getSummary() {
		if (null == summary) {
			return "";
		}
		return summary;
	}

	/**
	 * Sets the product.
	 * 
	 * @param product
	 *            the product to set
	 */
	public void setProduct(final String product) {
		this.product = product;
	}

	public void setScore(final float score) {
		this.score = score;
	}

	public void setSummary(final String summary) {
		this.summary = summary;
	}
}
