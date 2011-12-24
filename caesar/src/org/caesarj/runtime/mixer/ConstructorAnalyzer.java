package org.caesarj.runtime.mixer;

import java.util.HashMap;
import java.util.Map;

import org.caesarj.runtime.constructors.ParameterPattern;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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

	private static final String CONSTRUCTOR_METHOD_NAME = "<init>";

	public static String getIdForConstructor(String methodDescriptor) {
		// return className + "." + methodDescriptor;
		return methodDescriptor;
	}

	private class ConstructorMethodAnalyzer extends MethodVisitor {

		/**
		 * the descriptor of the constructor being analyzed
		 */
		private final String descriptor;

		public ConstructorMethodAnalyzer(String descriptor, MethodVisitor mv) {
			super(Opcodes.ASM4, mv);
			this.descriptor = descriptor;
		}

		// @Override
		// public void visitMethodInsn(int opcode, String owner, String name,
		// String desc) {
		// if (opcode == Opcodes.INVOKESPECIAL
		// && CONSTRUCTOR_METHOD_NAME.equals(name))
		// superConstructorCalls.put(descriptor, new ConstructorCall(desc,
		// !className.equals(owner)));
		// super.visitMethodInsn(opcode, owner, name, desc);
		// }

		@Override
		public void visitAttribute(Attribute attr) {
			// TODO extract parameters of constructor call from appropriate
			// attribute (to be created)
			if (attr instanceof ConstructorCallAttribute) {
				ConstructorCallAttribute constructorCallAttribute = (ConstructorCallAttribute) attr;
				constructorCalls.put(
						getIdForConstructor(descriptor),
						new ConstructorCall(constructorCallAttribute
								.getPattern(), constructorCallAttribute
								.isSuperConstructorCall()));
			} else
				super.visitAttribute(attr);
		}

	}

	private Map<String, ConstructorCall> constructorCalls = new HashMap<String, ConstructorCall>();

	public ConstructorAnalyzer(ClassVisitor cv) {
		super(Opcodes.ASM4, cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor visitor = super.visitMethod(access, name, desc,
				signature, exceptions);
		if (CONSTRUCTOR_METHOD_NAME.equals(name))
			visitor = new ConstructorMethodAnalyzer(desc, visitor);
		return visitor;
	}

	/**
	 * @return maps constructor descriptors to the contained constructor call as
	 *         found during analysis<br>
	 *         The constructor descriptor has the format as returned by
	 *         {@link #getIdForConstructor(String)}.
	 */
	public Map<String, ConstructorCall> getConstructorCalls() {
		return constructorCalls;
	}

}
