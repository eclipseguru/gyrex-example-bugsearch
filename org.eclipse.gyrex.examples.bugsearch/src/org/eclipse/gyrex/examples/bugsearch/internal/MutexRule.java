/**
 * Copyright (c) 2010 Gunnar Wagenknecht and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.examples.bugsearch.internal;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

class MutexRule implements ISchedulingRule {

	private final Object object;

	public MutexRule(final Object object) {
		this.object = object;
	}

	public boolean contains(final ISchedulingRule rule) {
		return rule == this;
	}

	public boolean isConflicting(final ISchedulingRule rule) {
		if (rule instanceof MutexRule) {
			return object.equals(((MutexRule) rule).object);
		}
		return false;
	}
}