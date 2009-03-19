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
package org.eclipse.gyrex.examples.bugsearch.gwt.internal.client;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * Bug Search Images
 */
public interface BugSearchImages extends ImageBundle {

	@Resource("toolbar-arrow-down.gif")
	AbstractImagePrototype arrowDown();

	@Resource("toolbar-arrow-right.gif")
	AbstractImagePrototype arrowRight();

	@Resource("find-clear.gif")
	AbstractImagePrototype clear();
}
