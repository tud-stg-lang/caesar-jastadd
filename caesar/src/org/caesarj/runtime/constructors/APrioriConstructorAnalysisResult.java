package org.caesarj.runtime.constructors;

import java.util.List;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * Results of analyzing a constructor produced in the
 * {@link org.caesarj.runtime.mixer.ConstructorMixer}
 * <p>
 * The results saved in this class are independent of a target constructor which
 * is called by the analyzed constructor.
 * 
 * @author Marko Martin
 */
public class APrioriConstructorAnalysisResult {

	private final MethodNode constructorNode;

	private final List<InsnList> argumentLoadInstructions;

	private final ParameterPattern constructorPattern;

	private final ParameterPattern constructorInvocationPattern;

	private final boolean isSuperInvocation;

	private final InsnList effectiveInstructions;

	private final int maxLocals;

	private final int maxStack;

	/**
	 * @param constructorNode
	 *            the method node which has been analyzed
	 * @param argumentLoadInstructions
	 *            instructions to push arguments for the constructor invocation
	 *            onto the stack
	 * @param effectiveInstructions
	 *            instructions to be executed by the constructor AFTER the call
	 *            to another constructor
	 * @param maxLocals
	 *            the maximum number of assumed local variables for the
	 *            constructor before any transformation
	 * @param maxStack
	 *            the maximum available stack size reserved for this method
	 *            before any transformation
	 * @param constructorPattern
	 *            the parameter pattern of the calling constructor
	 * @param constructorInvocationPattern
	 *            the pattern with which the other constructor is called
	 * @param isSuperInvocation
	 *            if true, the constructor invocation calls a constructor of the
	 *            superclass, otherwise it calls a constructor of the same
	 *            ("this") class
	 */
	public APrioriConstructorAnalysisResult(MethodNode constructorNode,
			List<InsnList> argumentLoadInstructions,
			InsnList effectiveInstructions, int maxLocals, int maxStack,
			ParameterPattern constructorPattern,
			ParameterPattern constructorInvocationPattern,
			boolean isSuperInvocation) {
		this.constructorNode = constructorNode;
		this.argumentLoadInstructions = argumentLoadInstructions;
		this.effectiveInstructions = effectiveInstructions;
		this.maxLocals = maxLocals;
		this.maxStack = maxStack;
		this.constructorPattern = constructorPattern;
		this.constructorInvocationPattern = constructorInvocationPattern;
		this.isSuperInvocation = isSuperInvocation;
	}

	public MethodNode getConstructorNode() {
		return constructorNode;
	}

	public List<InsnList> getArgumentLoadInstructions() {
		return argumentLoadInstructions;
	}

	public InsnList getEffectiveInstructions() {
		return effectiveInstructions;
	}

	public int getMaxLocals() {
		return maxLocals;
	}

	public int getMaxStack() {
		return maxStack;
	}

	public ParameterPattern getConstructorPattern() {
		return constructorPattern;
	}

	public ParameterPattern getConstructorInvocationPattern() {
		return constructorInvocationPattern;
	}

	public boolean isSuperInvocation() {
		return isSuperInvocation;
	}

}
