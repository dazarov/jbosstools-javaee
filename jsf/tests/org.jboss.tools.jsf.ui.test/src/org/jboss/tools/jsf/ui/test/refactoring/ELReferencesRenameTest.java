package org.jboss.tools.jsf.ui.test.refactoring;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameNonVirtualMethodProcessor;
import org.eclipse.jface.text.BadLocationException;
import org.jboss.tools.common.base.test.RenameParticipantTestUtil;
import org.jboss.tools.common.base.test.RenameParticipantTestUtil.TestChangeStructure;
import org.jboss.tools.common.base.test.RenameParticipantTestUtil.TestTextChange;
import org.jboss.tools.jsf.ui.el.refactoring.RenameMethodParticipant;

public class ELReferencesRenameTest extends ELRefactoringTest {

	public ELReferencesRenameTest(){
		super("Rename Method Refactoring Test");
	}

	public void testRenameMethod() throws CoreException, BadLocationException {
		ArrayList<TestChangeStructure> list = new ArrayList<TestChangeStructure>();

		TestChangeStructure structure = new TestChangeStructure(jsfProject, "/WebContent/pages/hello.jsp");
		TestTextChange change = new TestTextChange("name", 5, "alias");
		change.setOffset(353);
		structure.addTextChange(change);
		list.add(structure);

		structure = new TestChangeStructure(jsfProject, "/WebContent/pages/inputUserName.jsp");
		change = new TestTextChange("name", 5, "alias");
		change.setOffset(499);
		structure.addTextChange(change);
		list.add(structure);

		IMethod method = RenameParticipantTestUtil.getJavaMethod(jsfProject, "demo.User", "getName");
		RenameNonVirtualMethodProcessor renameProcessor = new RenameNonVirtualMethodProcessor(method);

		RenameParticipantTestUtil.checkRenameParticipant(method, renameProcessor, new RenameMethodParticipant(), "alias", list);
	}
}