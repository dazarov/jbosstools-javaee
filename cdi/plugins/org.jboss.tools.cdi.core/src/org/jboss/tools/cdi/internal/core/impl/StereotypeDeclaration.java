/******************************************************************************* 
 * Copyright (c) 2009 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.cdi.internal.core.impl;

import org.jboss.tools.cdi.core.ICDIAnnotation;
import org.jboss.tools.cdi.core.IStereotype;
import org.jboss.tools.cdi.core.IStereotypeDeclaration;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class StereotypeDeclaration extends AnnotationDeclaration implements IStereotypeDeclaration {

	public StereotypeDeclaration() {}

	public StereotypeDeclaration(AnnotationDeclaration d) {
		d.copyTo(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.cdi.core.IStereotypeDeclaration#getStereotype()
	 */
	public IStereotype getStereotype() {
		return project.getDelegate().getStereotype(getTypeName());
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.cdi.core.IAnnotationDeclaration#getAnnotation()
	 */
	public ICDIAnnotation getAnnotation() {
		return getStereotype();
	}
}