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
(function() { var f = document.getElementById('searchbox_017941334893793413703:sqfrdtd112s'); if (!f) { f = document.getElementById('searchbox_demo'); } if (f && f.q) { var q = f.q; var n = navigator; var l = location; if (n.platform == 'Win32') { q.style.cssText = 'border: 1px solid #7e9db9; padding: 2px;'; } var b = function() { if (q.value == '') { q.style.background = '#FFFFFF url(http:\x2F\x2Fwww.google.com\x2Fcoop\x2Fintl\x2Fen\x2Fimages\x2Fgoogle_custom_search_watermark.gif) left no-repeat'; } }; var f = function() { q.style.background = '#ffffff'; }; q.onfocus = f; q.onblur = b; if (!/[&?]q=[^&]/.test(l.search)) { b(); } } })();