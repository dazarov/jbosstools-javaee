/******************************************************************************* 
 * Copyright (c) 2010 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.cdi.internal.core.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.jboss.tools.cdi.core.CDIConstants;
import org.jboss.tools.cdi.core.CDICorePlugin;
import org.jboss.tools.cdi.core.CDIVersion;
import org.jboss.tools.cdi.core.IClassBean;
import org.jboss.tools.cdi.core.IDecorator;
import org.jboss.tools.cdi.core.IInterceptor;
import org.jboss.tools.cdi.core.IStereotype;
import org.jboss.tools.cdi.core.preferences.CDIPreferences;
import org.jboss.tools.cdi.xml.beans.model.CDIBeansConstants;
import org.jboss.tools.common.EclipseUtil;
import org.jboss.tools.common.model.XModelObject;
import org.jboss.tools.common.model.impl.XModelObjectImpl;
import org.jboss.tools.common.model.util.EclipseJavaUtil;
import org.jboss.tools.common.model.util.EclipseResourceUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * beans.xml validator
 * 
 * @author Alexey Kazakov
 */
public class BeansXmlValidationDelegate extends CDICoreValidationDelegate {

	private AlternativeClassValidator alternativeClassValidator;
	private AlternativeStereotypeValidator alternativeStereotypeValidator;
	private DecoratorTypeValidator decoratorTypeValidator;
	private InterceptorTypeValidator interceptorTypeValidator;

	public BeansXmlValidationDelegate(CDICoreValidator validator) {
		super(validator);
	}

	private AlternativeClassValidator getAlternativeClassValidator() {
		if(alternativeClassValidator==null) {
			alternativeClassValidator = new AlternativeClassValidator();
		}
		return alternativeClassValidator;
	}

	private AlternativeStereotypeValidator getAlternativeStereotypeValidator() {
		if(alternativeStereotypeValidator==null) {
			alternativeStereotypeValidator = new AlternativeStereotypeValidator();
		}
		return alternativeStereotypeValidator;
	}

	private DecoratorTypeValidator getDecoratorTypeValidator() {
		if(decoratorTypeValidator==null) {
			decoratorTypeValidator = new DecoratorTypeValidator();
		}
		return decoratorTypeValidator;
	}

	private InterceptorTypeValidator getInterceptorTypeValidator() {
		if(interceptorTypeValidator==null) {
			interceptorTypeValidator = new InterceptorTypeValidator();
		}
		return interceptorTypeValidator;
	}

	public void validateBeansXml(CDICoreValidator.CDIValidationContext context, IFile beansXml) {
		XModelObject f = EclipseResourceUtil.createObjectForResource(beansXml);
		String xmodelpath = (f == null) ? "" : f.getPath();

		IModelManager manager = StructuredModelManager.getModelManager();
		if(manager == null) {
			// this may happen if plug-in org.eclipse.wst.sse.core 
			// is stopping or un-installed, that is Eclipse is shutting down.
			// there is no need to report it, just stop validation.
			return;
		}

		IStructuredModel model = null;
		try {
			model = manager.getModelForRead(beansXml);
			if (model instanceof IDOMModel) {
				IDOMModel domModel = (IDOMModel) model;
				IDOMDocument document = domModel.getDocument();

				/*
				 * 5.1.1. Declaring selected alternatives for a bean archive
				 *  - Each child <class> element must specify the name of an alternative bean class. If there is no class with the specified
				 *    name, or if the class with the specified name is not an alternative bean class, the container automatically detects the problem
				 *    and treats it as a deployment problem.
				 *  - If the same type is listed twice under the <alternatives> element, the container automatically detects the problem and
				 *    treats it as a deployment problem.
				 */
				validateTypeBeanForBeansXml(context, getAlternativeClassValidator(), document, beansXml, xmodelpath + "/" + CDIBeansConstants.NODE_ALTERNATIVES); //$NON-NLS-1$

				/*
				 * 5.1.1. Declaring selected alternatives for a bean archive
				 *  - Each child <stereotype> element must specify the name of an @Alternative stereotype annotation. If there is no annotation
				 *    with the specified name, or the annotation is not an @Alternative stereotype, the container automatically detects the
				 *    problem and treats it as a deployment problem.
				 *  - If the same type is listed twice under the <alternatives> element, the container automatically detects the problem and
				 *    treats it as a deployment problem. 
				 */
				validateTypeBeanForBeansXml(context, getAlternativeStereotypeValidator(), document,	beansXml, xmodelpath + "/" + CDIBeansConstants.NODE_ALTERNATIVES); //$NON-NLS-1$

				/*
				 * 8.2. Decorator enablement and ordering
				 *  - Each child <class> element must specify the name of a decorator bean class. If there is no class with the specified name,
				 *    or if the class with the specified name is not a decorator bean class, the container automatically detects the problem and
				 *    treats it as a deployment problem.
				 *  - If the same class is listed twice under the <decorators> element, the container automatically detects the problem and
				 *    treats it as a deployment problem.
				 */
				validateTypeBeanForBeansXml(context, getDecoratorTypeValidator(), document, beansXml, xmodelpath + "/" + CDIBeansConstants.NODE_DECORATORS); //$NON-NLS-1$

				/*
				 * 9.4. Interceptor enablement and ordering
				 * 	- Each child <class> element must specify the name of an interceptor class. If there is no class with the specified name, or if
				 * 	  the class with the specified name is not an interceptor class, the container automatically detects the problem and treats it as
				 * 	  a deployment problem.
				 *  - If the same class is listed twice under the <interceptors> element, the container automatically detects the problem and treats it as
				 *    a deployment problem.
				 */
				validateTypeBeanForBeansXml(context, getInterceptorTypeValidator(), document, beansXml, xmodelpath + "/" + CDIBeansConstants.NODE_INTERCEPTORS); //$NON-NLS-1$
			}
		} catch (CoreException e) {
			CDICorePlugin.getDefault().logError(e);
        } catch (IOException e) {
        	CDICorePlugin.getDefault().logError(e);
		} finally {
			if (model != null) {
				model.releaseFromRead();
			}
		}
	}

	private void validateTypeBeanForBeansXml(CDICoreValidator.CDIValidationContext context, TypeValidator typeValidator, IDOMDocument document, IFile beansXml, String xmodelpath) {
		try {
			NodeList parentNodeList = document.getElementsByTagName(typeValidator.getParrentElementname());
			for (int i = 0; i < parentNodeList.getLength(); i++) {
				Node parentNode = parentNodeList.item(i);
				if(parentNode instanceof Element) {
					List<TypeNode> typeNodes = getTypeElements((Element)parentNode, typeValidator.getTypeElementName());
					Map<String, TypeNode> uniqueTypes = new HashMap<String, TypeNode>();
					for (TypeNode typeNode : typeNodes) {
						String typepath = xmodelpath;
						String attr = null;
						if(typeNode.getTypeName() != null) {
							typepath = typepath + "/" + typeNode.getTypeName(); //$NON-NLS-1$
							attr = typeValidator.getTypeElementName();
						}
						IType type = getType(context, beansXml, typeNode, typeValidator, typepath, attr);
						if(type!=null) {
							if(!validator.isAsYouTypeValidation() && !type.isBinary()) {
								validator.getValidationContext().addLinkedCoreResource(CDICoreValidator.SHORT_ID, beansXml.getFullPath().toOSString(), type.getPath(), false);
								Set<IPath> relatedResources = new HashSet<IPath>();
								IResource resource = type.getResource();
								if(resource instanceof IFile) {
									validator.collectAllRelatedInjectionsForBean((IFile)resource, relatedResources);
									for (IPath path : relatedResources) {
										validator.getValidationContext().addLinkedCoreResource(CDICoreValidator.SHORT_ID, path.toOSString(), beansXml.getFullPath(), false);
									}
								}
							}
							String typeError = typeValidator.validateType(context, type);
							if(typeError != null) {
								IMarker marker = validator.addProblem(typeValidator.getIllegalTypeErrorMessage(getVersion(context)), CDIPreferences.ILLEGAL_TYPE_NAME_IN_BEANS_XML,
										new String[]{typeNode.getTypeName()}, typeNode.getLength(), typeNode.getStartOffset(), beansXml, typeValidator.getIllegalTypeErrorMessageId());
								if(marker != null) bindMarkerToModel(marker, typepath, typeValidator.getTypeElementName());
								if(type.isBinary()) {
									continue;
								}
							}
							TypeNode node = uniqueTypes.get(typeNode.getTypeName());
							if(node!=null) {
								if(!node.isMarkedAsDuplicated()) {
									IMarker marker = validator.addProblem(typeValidator.getDuplicateTypeErrorMessage(getVersion(context)), CDIPreferences.DUPLICATE_TYPE_IN_BEANS_XML,
											new String[]{}, node.getLength(), node.getStartOffset(), beansXml);
									if(marker != null) bindMarkerToModel(marker, typepath, typeValidator.getTypeElementName());
								}
								node.setMarkedAsDuplicated(true);
								typeNode.setMarkedAsDuplicated(true);
								typeNode.setDuplicationIndex(node.getDuplicationIndex() + 1);
								IMarker marker = validator.addProblem(typeValidator.getDuplicateTypeErrorMessage(getVersion(context)), CDIPreferences.DUPLICATE_TYPE_IN_BEANS_XML,
										new String[]{}, typeNode.getLength(), typeNode.getStartOffset(), beansXml);
								if(marker != null) {
									int di = typeNode.getDuplicationIndex();
									if(di > 0) {
										typepath += XModelObjectImpl.DUPLICATE + di;
									}
									bindMarkerToModel(marker, typepath, typeValidator.getTypeElementName());
								}
							}
							uniqueTypes.put(typeNode.getTypeName(), typeNode);
						}
					}
				}
			}
		} catch (JavaModelException e) {
			CDICorePlugin.getDefault().logError(e);
        }
	}

	private Map<IProject, IJavaProject> javaProjects;

	public IJavaProject getJavaProject(IResource resource) {
		if(javaProjects == null) {
			javaProjects = new HashMap<IProject, IJavaProject>();
		}
		IProject project = resource.getProject();
		if(project.isAccessible()) {
			IJavaProject javaProject = javaProjects.get(project);
			if(javaProject==null) {
				javaProject = EclipseUtil.getJavaProject(project);
				if(javaProject!=null) {
					javaProjects.put(project, javaProject);
				}
			}
			return javaProject;
		}
		return null;
	}

	private IType getType(CDICoreValidator.CDIValidationContext context, IFile beansXml, TypeNode node, TypeValidator typeValidator, String xmodelpath, String attr) {
		IType type = null;
		String typeName = node.getTypeName();
		if(typeName!=null && typeName.trim().length() > 0) {
			try {
				IJavaProject javaProject = getJavaProject(beansXml);
				if(javaProject!=null) {
					type = EclipseJavaUtil.findType(javaProject, typeName);
				}
			} catch (JavaModelException e) {
				CDICorePlugin.getDefault().logError(e);
				return null;
			}
		} else {
			IMarker marker = validator.addProblem(typeValidator.getEmptyTypeErrorMessage(getVersion(context)), CDIPreferences.ILLEGAL_TYPE_NAME_IN_BEANS_XML,
					new String[]{node.getTypeName()}, node.getLength(), node.getStartOffset(), beansXml, typeValidator.getUnknownTypeErrorMessageId());
			bindMarkerToModel(marker, xmodelpath, attr);
			return null;
		}
		if(type==null) {
			addLinkedResourcesForUnknownType(beansXml, node.getTypeName());
			IMarker marker = validator.addProblem(typeValidator.getUnknownTypeErrorMessage(getVersion(context)), CDIPreferences.ILLEGAL_TYPE_NAME_IN_BEANS_XML,
					new String[]{node.getTypeName()}, node.getLength(), node.getStartOffset(), beansXml, typeValidator.getUnknownTypeErrorMessageId());
			bindMarkerToModel(marker, xmodelpath, attr);
		}
		return type;
	}

	private void bindMarkerToModel(IMarker marker, String path, String attribute) {
		try {
			if(marker!=null) {
				marker.setAttribute("path", path); //$NON-NLS-1$
				if(attribute != null) {
					marker.setAttribute("attribute", attribute); //$NON-NLS-1$
				}
			}
		} catch(CoreException e) {
			CDICorePlugin.getDefault().logError(e);
		}
	}

	private void addLinkedResourcesForUnknownType(IFile beansXml, String typeName) {
		if(!validator.isAsYouTypeValidation() && typeName!=null && typeName.trim().length()>0) {
			IStatus status = JavaConventions.validateJavaTypeName(typeName, CompilerOptions.VERSION_1_7, CompilerOptions.VERSION_1_7);
			if(status.getSeverity()!=IStatus.ERROR) {
				String packagePath = typeName.replace('.', '/');
				Set<IFolder> sources = EclipseResourceUtil.getSourceFolders(beansXml.getProject());
				for (IFolder source : sources) {
					IPath path = source.getFullPath().append(packagePath + ".java"); //$NON-NLS-1$
					validator.getValidationContext().addLinkedCoreResource(CDICoreValidator.SHORT_ID, beansXml.getFullPath().toOSString(), path, false);
				}
			}
		}
	}

	private List<TypeNode> getTypeElements(Element parentElement, String typeElementName) {
		List<TypeNode> result = new ArrayList<TypeNode>();
		NodeList list = parentElement.getElementsByTagName(typeElementName);
		for (int i = 0; i < list.getLength(); i++) {
			Node classNode = list.item(i);
			NodeList children = classNode.getChildNodes();

			boolean empty = true;
			for (int j = 0; j < children.getLength(); j++) {
				Node node = children.item(j);
				if(node.getNodeType() == Node.TEXT_NODE) {
					String value = node.getNodeValue();
					if(value!=null) {
						String className = value.trim();
						if(className.length()==0) {
							continue;
						}
						empty = false;
						if(node instanceof IndexedRegion) {
							int start = ((IndexedRegion)node).getStartOffset() + value.indexOf(className);
							int length = className.length();
							result.add(new TypeNode(start, length, className));
							break;
						}
					}
				}
			}

			if(empty && classNode instanceof IndexedRegion) {
				int start = ((IndexedRegion)classNode).getStartOffset();
				int end = ((IndexedRegion)classNode).getEndOffset();
				int length = end - start;
				result.add(new TypeNode(start, length, null));
			}
		}
		return result;
	}

	private static class TypeNode {
		private int startOffset;
		private int length;
		private String typeName;
		private boolean markedAsDuplicated;
		private int duplicationIndex = 0;

		public TypeNode(int startOffset, int length, String typeName) {
			this.startOffset = startOffset;
			this.length = length;
			this.typeName = typeName;
		}

		public int getStartOffset() {
			return startOffset;
		}

		public void setStartOffset(int startOffset) {
			this.startOffset = startOffset;
		}

		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}

		/**
		 * Returns type name or null if value in XML is empty or whitespace.
		 * @return
		 */
		public String getTypeName() {
			return typeName;
		}

		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}

		public boolean isMarkedAsDuplicated() {
			return markedAsDuplicated;
		}

		public void setMarkedAsDuplicated(boolean markedAsDuplicated) {
			this.markedAsDuplicated = markedAsDuplicated;
		}
	
		public int getDuplicationIndex() {
			return duplicationIndex;
		}

		public void setDuplicationIndex(int i) {
			duplicationIndex = i;
		}
	}

	protected CDIVersion getVersion(CDICoreValidator.CDIValidationContext context) {
		return context.getCdiProject().getVersion();
	}
	
	private static interface TypeValidator {

		String validateType(CDICoreValidator.CDIValidationContext context, IType type) throws JavaModelException;

		String getTypeElementName();

		String getParrentElementname();

		String getEmptyTypeErrorMessage(CDIVersion version);

		String getUnknownTypeErrorMessage(CDIVersion version);
		
		int getUnknownTypeErrorMessageId();

		String getIllegalTypeErrorMessage(CDIVersion version);
		
		int getIllegalTypeErrorMessageId();

		String getDuplicateTypeErrorMessage(CDIVersion version);

	}

	private abstract class AbstractTypeValidator implements TypeValidator {

		@Override
		public String getTypeElementName() {
			return "class"; //$NON-NLS-1$
		}

		@Override
		public String validateType(CDICoreValidator.CDIValidationContext context, IType type) throws JavaModelException {
			if(!validateKindOfType(type)) {
				return getIllegalTypeErrorMessage(getVersion(context));
			}
			if(type.isBinary()) {			
				if(!validateBinaryType(type)) {
					return getIllegalTypeErrorMessage(getVersion(context));
				}
			} else if(!validateSourceType(context, type)) {
				return getIllegalTypeErrorMessage(getVersion(context));
			}
			return null;
		}

		abstract public boolean validateSourceType(CDICoreValidator.CDIValidationContext context, IType type);

		/**
		 * Validates if the type represens class/annotation/...
		 * @param type
		 * @return
		 * @throws JavaModelException 
		 */
		public boolean validateKindOfType(IType type) throws JavaModelException {
			return type.isClass();
		}

		public boolean validateBinaryType(IType type) throws JavaModelException {
			IAnnotation[] annotations = type.getAnnotations();
			for (IAnnotation annotation : annotations) {
				if(annotation.getElementName().equals(getAnnotationName())) {
					return true;
				}
			}
			return false;
		}

		protected abstract String getAnnotationName();
	}

	private class AlternativeClassValidator extends AbstractTypeValidator {

		@Override
		public boolean validateSourceType(CDICoreValidator.CDIValidationContext context, IType type) {
			IClassBean classBean = context.getCdiProject().getBeanClass(type);
			return classBean!=null && classBean.isAlternative();
		}

		@Override
		public String getParrentElementname() {
			return "alternatives"; //$NON-NLS-1$
		}

		@Override
		public String getEmptyTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.EMPTY_ALTERNATIVE_BEAN_CLASS_NAME[version.getIndex()];
		}

		@Override
		public String getUnknownTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.UNKNOWN_ALTERNATIVE_BEAN_CLASS_NAME[version.getIndex()];
		}

		@Override
		public int getUnknownTypeErrorMessageId() {
			return CDIValidationErrorManager.UNKNOWN_ALTERNATIVE_BEAN_CLASS_NAME_ID;
		}

		@Override
		public String getIllegalTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.ILLEGAL_ALTERNATIVE_BEAN_CLASS[version.getIndex()];
		}

		@Override
		public int getIllegalTypeErrorMessageId() {
			return CDIValidationErrorManager.ILLEGAL_ALTERNATIVE_BEAN_CLASS_ID;
		}

		@Override
		public String getDuplicateTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.DUPLICATE_ALTERNATIVE_TYPE[version.getIndex()];
		}

		@Override
		protected String getAnnotationName() {
			return CDIConstants.ALTERNATIVE_ANNOTATION_TYPE_NAME;
		}
	}

	private class AlternativeStereotypeValidator extends AbstractTypeValidator {

		@Override
		public boolean validateSourceType(CDICoreValidator.CDIValidationContext context, IType type) {
			IStereotype stereotype = context.getCdiProject().getStereotype(type);
			return stereotype!=null && stereotype.isAlternative();
		}

		@Override
		public boolean validateKindOfType(IType type) throws JavaModelException {
			return type.isAnnotation();
		}

		@Override
		public String getTypeElementName() {
			return "stereotype"; //$NON-NLS-1$
		}

		@Override
		public String getParrentElementname() {
			return "alternatives"; //$NON-NLS-1$
		}

		@Override
		public String getEmptyTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.EMPTY_ALTERNATIVE_ANNOTATION_NAME[version.getIndex()];
		}

		@Override
		public String getUnknownTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.UNKNOWN_ALTERNATIVE_ANNOTATION_NAME[version.getIndex()];
		}

		@Override
		public int getUnknownTypeErrorMessageId() {
			return CDIValidationErrorManager.UNKNOWN_ALTERNATIVE_ANNOTATION_NAME_ID;
		}

		@Override
		public String getIllegalTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.ILLEGAL_ALTERNATIVE_ANNOTATION[version.getIndex()];
		}

		@Override
		public int getIllegalTypeErrorMessageId() {
			return CDIValidationErrorManager.ILLEGAL_ALTERNATIVE_ANNOTATION_ID;
		}

		@Override
		public String getDuplicateTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.DUPLICATE_ALTERNATIVE_TYPE[version.getIndex()];
		}

		@Override
		protected String getAnnotationName() {
			return CDIConstants.ALTERNATIVE_ANNOTATION_TYPE_NAME;
		}
	}

	private class DecoratorTypeValidator extends AbstractTypeValidator {

		@Override
		public boolean validateSourceType(CDICoreValidator.CDIValidationContext context, IType type) {
			IClassBean classBean = context.getCdiProject().getBeanClass(type);
			return classBean instanceof IDecorator;
		}

		@Override
		public String getParrentElementname() {
			return "decorators"; //$NON-NLS-1$
		}

		@Override
		public String getEmptyTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.EMPTY_DECORATOR_BEAN_CLASS_NAME[version.getIndex()];
		}

		@Override
		public String getUnknownTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.UNKNOWN_DECORATOR_BEAN_CLASS_NAME[version.getIndex()];
		}

		@Override
		public int getUnknownTypeErrorMessageId() {
			return CDIValidationErrorManager.UNKNOWN_DECORATOR_BEAN_CLASS_NAME_ID;
		}

		@Override
		public String getIllegalTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.ILLEGAL_DECORATOR_BEAN_CLASS[version.getIndex()];
		}

		@Override
		public int getIllegalTypeErrorMessageId() {
			return CDIValidationErrorManager.ILLEGAL_DECORATOR_BEAN_CLASS_ID;
		}

		@Override
		public String getDuplicateTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.DUPLICATE_DECORATOR_CLASS[version.getIndex()];
		}

		@Override
		protected String getAnnotationName() {
			return CDIConstants.DECORATOR_STEREOTYPE_TYPE_NAME;
		}
	}

	private class InterceptorTypeValidator extends AbstractTypeValidator {

		@Override
		public boolean validateSourceType(CDICoreValidator.CDIValidationContext context, IType type) {
			IClassBean classBean = context.getCdiProject().getBeanClass(type);
			return classBean instanceof IInterceptor;
		}

		@Override
		public String getParrentElementname() {
			return "interceptors"; //$NON-NLS-1$
		}

		@Override
		public String getEmptyTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.EMPTY_INTERCEPTOR_CLASS_NAME[version.getIndex()];
		}

		@Override
		public String getUnknownTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.UNKNOWN_INTERCEPTOR_CLASS_NAME[version.getIndex()];
		}

		@Override
		public int getUnknownTypeErrorMessageId() {
			return CDIValidationErrorManager.UNKNOWN_INTERCEPTOR_CLASS_NAME_ID;
		}

		@Override
		public String getIllegalTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.ILLEGAL_INTERCEPTOR_CLASS[version.getIndex()];
		}

		@Override
		public int getIllegalTypeErrorMessageId() {
			return CDIValidationErrorManager.ILLEGAL_INTERCEPTOR_CLASS_ID;
		}

		@Override
		public String getDuplicateTypeErrorMessage(CDIVersion version) {
			return CDIValidationMessages.DUPLICATE_INTERCEPTOR_CLASS[version.getIndex()];
		}

		@Override
		protected String getAnnotationName() {
			return CDIConstants.INTERCEPTOR_ANNOTATION_TYPE_NAME;
		}
	}
}