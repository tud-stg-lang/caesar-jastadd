package org.caesarj.runtime.constructors;

import java.util.List;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * Results of analyzing a constructor produced in the
 * {@link org.caesarj.runtime.mixer.ConstructorMixer}
 * 
 * @author Marko Martin
 */
public class ConstructorAnalysisResult {

	private final MethodNode constructorNode;

	private final List<InsnList> argumentLoadInstructions;

	private final ParameterPattern constructorInvocationPattern;

	private final boolean isSuperInvocation;

	private final InsnList effectiveInstructions;

	/**
	 * @param constructorNode
	 *            the method node which is unchanged except for its instructions
	 *            which MUST be reset
	 * @param argumentLoadInstructions
	 *            instructions to push arguments for the constructor invocation
	 *            onto the stack
	 * @param effectiveInstructions
	 *            instructions executed by the constructor AFTER the call to
	 *            another constructor
	 * @param constructorInvocationPattern
	 *            the pattern with which the other constructor is called
	 * @param isSuperInvocation
	 *            if true, the constructor invocation calls a constructor of the
	 *            superclass, otherwise it calls a constructor of the same
	 *            ("this") class
	 */
	public ConstructorAnalysisResult(MethodNode constructorNode,
			List<InsnList> argumentLoadInstructions,
			InsnList effectiveInstructions,
			ParameterPattern constructorInvocationPattern,
			boolean isSuperInvocation) {
		this.constructorNode = constructorNode;
		this.argumentLoadInstructions = argumentLoadInstructions;
		this.effectiveInstructions = effectiveInstructions;
		this.constructorInvocationPattern = constructorInvocationPattern;
		this.isSuperInvocation = isSuperInvocation;
	}

	public MethodNode getConstructorNode() {
		return constructorNode;
	}

	public List<InsnList> getArgumentLoadInstructions() {
		return argumentLoadInstructions;
	}

	public ParameterPattern getConstructorInvocationPattern() {
		return constructorInvocationPattern;
	}

	public boolean isSuperInvocation() {
		return isSuperInvocation;
	}

	public InsnList getEffectiveInstructions() {
		return effectiveInstructions;
	}

}
