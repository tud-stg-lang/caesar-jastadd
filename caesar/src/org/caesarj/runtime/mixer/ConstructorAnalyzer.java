package org.caesarj.runtime.mixer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.caesarj.runtime.constructors.ConstructorAnalysisResult;
import org.caesarj.runtime.constructors.ParameterPattern;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Analyzes constructors of a class in order to detect which super constructor
 * is invoked in each constructor.
 * 
 * @author Marko Martin
 */
public class ConstructorAnalyzer extends ClassVisitor {

	public static class ConstructorCall {

		private final ParameterPattern parameterPattern;

		private final boolean isSuper;

		public ConstructorCall(ParameterPattern parameterPattern,
				boolean isSuper) {
			this.parameterPattern = parameterPattern;
			this.isSuper = isSuper;
		}

		public ParameterPattern getParameterPattern() {
			return parameterPattern;
		}

		/**
		 * @return true if the constructor of the super class was called or
		 *         false if the constructor of this class was called
		 */
		public boolean isSuper() {
			return isSuper;
		}

	}

	private class ConstructorNode extends MethodNode {

		public ConstructorNode(int access, String name, String desc,
				String signature, String[] exceptions) {
			super(access, name, desc, signature, exceptions);
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			analyze(this);
		}

	}

	private static final String CONSTRUCTOR_METHOD_NAME = "<init>";

	private final List<ConstructorAnalysisResult> results = new ArrayList<ConstructorAnalysisResult>();

	public ConstructorAnalyzer(ClassVisitor cv) {
		super(Opcodes.ASM4, cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor visitor = super.visitMethod(access, name, desc,
				signature, exceptions);
		if (CONSTRUCTOR_METHOD_NAME.equals(name)) {
			ConstructorNode node = new ConstructorNode(access, name, desc,
					signature, exceptions);
			visitor = visitor == null ? node : new ListDelegationMethodVisitor(
					new MethodVisitor[] { visitor, node });
		}
		return visitor;
	}

	private void analyze(MethodNode constructorNode) {
		ParameterPattern constructorInvocationPattern = null;
		boolean isSuperInvocation = true;
		@SuppressWarnings("unchecked")
		Iterator<Attribute> attrIterator = constructorNode.attrs.iterator();
		while (attrIterator.hasNext()) {
			Attribute attribute = attrIterator.next();
			if (!(attribute instanceof ConstructorCallAttribute))
				continue;
			ConstructorCallAttribute constructorCallAttribute = (ConstructorCallAttribute) attribute;
			constructorInvocationPattern = constructorCallAttribute
					.getPattern();
			isSuperInvocation = constructorCallAttribute
					.isSuperConstructorCall();
			attrIterator.remove();
			break;
		}
		if (constructorInvocationPattern == null) {
			System.err
					.println("ConstructorAnalyzer.analyze: Expected to find ConstructorCallAttribute for constructor.");
			return;
		}

		List<InsnList> argumentLoadInstructions = separateInstructions(constructorNode);
		InsnList outerClassInstanceLoadInstructions = argumentLoadInstructions
				.remove(0);
		results.add(new ConstructorAnalysisResult(constructorNode,
				outerClassInstanceLoadInstructions, argumentLoadInstructions,
				constructorNode.instructions, constructorInvocationPattern,
				isSuperInvocation));
	}

	/**
	 * @param constructorNode
	 *            the constructor node to be analyzed<br>
	 *            Also, the instructions of this node will be modified by
	 *            deleting all instructions preceding the constructor invocation
	 *            and the constructor invocation itself.
	 * @return instructions used to push constructor call arguments onto the
	 *         stack, separated per argument<br>
	 *         This method assumes constructors to have the following (pseudo)
	 *         format:<br>
	 *         <code>
	 *         LOAD_A0<br>
	 *         LOAD_ARGUMENT<br>
	 *         (NOP, LOAD_ARGUMENT)*<br>
	 *         CALL_CONSTRUCTOR<br>
	 *         REST_OF_CONSTRUCTOR<br>
	 *         </code> Thereby, LOAD_ARGUMENT may not contain NOPs.
	 * @see org.caesarj.ast.ConstructorAccess#emitArgument(org.caesarj.ast.CodeGeneration,
	 *      int)
	 */
	private List<InsnList> separateInstructions(MethodNode constructorNode) {
		final List<InsnList> argumentLoadInstructions = new ArrayList<InsnList>();
		@SuppressWarnings("unchecked")
		/*
		 * Skip the first instruction which loads this onto the stack:
		 */
		ListIterator<AbstractInsnNode> instructionIterator = constructorNode.instructions
				.iterator();
		while (instructionIterator.hasNext()) {
			AbstractInsnNode node = instructionIterator.next();
			instructionIterator.remove();
			if (node.getOpcode() == Opcodes.ALOAD
					&& node instanceof VarInsnNode
					&& ((VarInsnNode) node).var == 0)
				break;
		}
		InsnList currentArgument = new InsnList();
		boolean foundConstructorCall = false;
		while (instructionIterator.hasNext() && !foundConstructorCall) {
			AbstractInsnNode node = instructionIterator.next();
			switch (node.getOpcode()) {
			case Opcodes.NOP:
				argumentLoadInstructions.add(currentArgument);
				currentArgument = new InsnList();
				break;
			case Opcodes.INVOKESPECIAL:
				if (CONSTRUCTOR_METHOD_NAME
						.equals(((MethodInsnNode) node).name)) {
					if (currentArgument.size() > 0)
						argumentLoadInstructions.add(currentArgument);
					foundConstructorCall = true;
					break;
				}
			default:
				AbstractInsnNode clonedNode = node.clone(Collections.EMPTY_MAP);
				if (clonedNode != null)
					currentArgument.add(clonedNode);
			}
			instructionIterator.remove();
		}
		if (!foundConstructorCall)
			System.err
					.format("ConstructorAnalyzer.: Did not find constructor call during method analysis of method %s.%n",
							constructorNode.name);
		return argumentLoadInstructions;
	}

	public List<ConstructorAnalysisResult> getResults() {
		return results;
	}

}
