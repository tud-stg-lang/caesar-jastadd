package org.caesarj.runtime.mixer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.caesarj.runtime.mixer.ConstructorAnalyzer.ConstructorCall;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ConstructorMixer extends ClassVisitor {

	private static class SingleConstructorMixer extends MethodVisitor {

		private String owner;
		private String name;
		private String desc;

		private boolean done = false;

		/**
		 * A mixer which replaces the constructor invocation within a method by
		 * a call to the constructor with the given name, descriptor and owner.
		 * 
		 * @param owner
		 * @param name
		 * @param desc
		 * @param mv
		 */
		public SingleConstructorMixer(String owner, String name, String desc,
				MethodVisitor mv) {
			super(Opcodes.ASM4, mv);
			this.owner = owner;
			this.name = name;
			this.desc = desc;
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name,
				String desc) {
			if (done || opcode != Opcodes.INVOKESPECIAL || !isConstructor(name)) {
				super.visitMethodInsn(opcode, owner, name, desc);
				return;
			}
			super.visitMethodInsn(opcode, this.owner, this.name, this.desc);
			done = true;
		}

	}

	private static final String CONSTRUCTOR_METHOD_NAME = "<init>";

	private List<Object> constructors = new ArrayList<Object>();

	private final Map<String, ConstructorCall> constructorCalls;

	public ConstructorMixer(ClassVisitor cv,
			Map<String, ConstructorCall> constructorCalls) {
		super(Opcodes.ASM4, cv);
		this.constructorCalls = constructorCalls;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		if (!isConstructor(name))
			return super.visitMethod(access, name, desc, signature, exceptions);
		ConstructorCall call = constructorCalls.get(ConstructorAnalyzer
				.getIdForConstructor(desc));
		if (call == null) {
			System.err
					.format("ConstructorMixer.visitMethod: Expected to have a ConstructorCall object for method %s%n",
							name);
			return null;
		}
		// TODO
		return null;
	}

	private static boolean isConstructor(String name) {
		return CONSTRUCTOR_METHOD_NAME.equals(name);
	}

	@Override
	public void visitEnd() {
		// TODO
		// for (Object constructor : constructors) {
		// String calledConstructorTemplate =
		// getCalledConstructorSignature(constructor);
		// Object refClass;
		// if (callsSuperConstructor(constructor))
		// refClass = getSuperClass();
		// else
		// refClass = getThisClass();
		// List<Object> refConstructorCandidates = getConstructors(refClass);
		// for (Object refConstructorCandidate : refConstructorCandidates) {
		// Map<String, String> constructorParameterMapping =
		// instantiateConstructorTemplate(
		// calledConstructorTemplate, refConstructorCandidate);
		// if (constructorParameterMapping != null)
		// visitConstructor(constructor, refConstructorCandidate);
		// }
		// }
		super.visitEnd();
	}

	/**
	 * @param constructor
	 * @return the signature of the constructor called by the given constructor
	 */
	private static String getCalledConstructorSignature(Object constructor) {
		// TODO Auto-generated method stub
		return null;
	}

	private static boolean callsSuperConstructor(Object constructor) {
		// TODO Auto-generated method stub
		return false;
	}

	private Object getSuperClass() {
		// TODO Auto-generated method stub
		return null;
	}

	private Object getThisClass() {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<Object> getConstructors(Object containingClass) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param constructorTemplate
	 * @param constructor
	 * @return a mapping of parameters in the given template to parts of the
	 *         signature of the given concrete constructor, or null if no such
	 *         mapping exists
	 */
	private static Map<String, String> instantiateConstructorTemplate(
			String constructorTemplate, Object constructor) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Writes a concrete constructor instantiating the given template
	 * constructor with the given referenced constructor.
	 * 
	 * @param templateConstructor
	 * @param referencedConstructor
	 */
	private void visitConstructor(Object templateConstructor,
			Object referencedConstructor) {
		// TODO Auto-generated method stub

	}

}
