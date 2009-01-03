/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.cloudfree.examples.bugsearch.gwt.internal.client;

import com.google.gwt.i18n.client.Messages;

/**
 * Bug Search Messages
 */
public interface BugSearchMessages extends Messages {

	@DefaultMessage("{0} bugs")
	@PluralText( { "one", "1 bug" })
	String bugs(@PluralCount int count);

}
