package org.caesarj.runtime.mixer;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.caesarj.runtime.constructors.ConcreteParameter;
import org.caesarj.runtime.constructors.ConstructorAnalysisResult;
import org.caesarj.runtime.constructors.ConstructorPatternMatcher;
import org.caesarj.runtime.constructors.ParameterPattern;
import org.caesarj.runtime.constructors.PatternArgumentLoadingInstructionComposer;
import org.caesarj.util.ClassAccess;
import org.caesarj.util.Cloner;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Creates constructors for the class which actually match the existing
 * constructors in the superclass.
 * 
 * @author Marko Martin
 */
public class ConstructorMixer extends ClassVisitor {

	private class ConstructorNode extends MethodNode {

		public ConstructorNode(int access, String name, String desc,
				String signature, String[] exceptions) {
			super(access, name, desc, signature, exceptions);
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			process(this);
		}

	}

	private static final Type TYPE_CJOBJECTIFC = Type
			.getType("Lorg/caesarj/runtime/CjObjectIfc;");

	private static final String CONSTRUCTOR_METHOD_NAME = "<init>";

	private final List<List<ConcreteParameter>> existingSuperConstructors = new ArrayList<List<ConcreteParameter>>();

	private final Set<List<ConcreteParameter>> existingThisConstructors = new HashSet<List<ConcreteParameter>>();

	private final ClassLoader classLoader;

	private String superClassName;

	private String className;

	/**
	 * @param classLoader
	 *            the class loader to be used to load related (super) classes
	 * @param cv
	 *            next visitor in chain
	 * @throws IllegalArgumentException
	 *             if constructorCalls or classLoader is null
	 */
	public ConstructorMixer(ClassLoader classLoader, ClassVisitor cv)
			throws IllegalArgumentException {
		super(Opcodes.ASM4, cv);
		if (classLoader == null)
			throw new IllegalArgumentException("classLoader must not be null.");
		this.classLoader = classLoader;
	}

	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		this.className = name;
		this.superClassName = superName;
		Constructor<?>[] superConstructors = null;
		try {
			superConstructors = ClassAccess.forName(
					superName.replace('/', '.'), true, classLoader)
					.getConstructors();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (superConstructors != null) {
			for (Constructor<?> constructor : superConstructors) {
				List<ConcreteParameter> parameters = new ArrayList<ConcreteParameter>();
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				/*
				 * Skip the first type because it is always CjObjectItf (outer
				 * class type):
				 */
				for (int i = 1; i < parameterTypes.length; i++) {
					Class<?> type = parameterTypes[i];
					/*
					 * TODO Set name of parameter (in ConcreteParameter
					 * constructor). Maybe use annotations for named parameters?
					 */
					parameters.add(new ConcreteParameter(type.getName()));
				}
				existingSuperConstructors.add(parameters);
			}
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		if (isConstructor(name))
			/*
			 * This will load all data of this method (including instructions)
			 * into the ConstructorNode. Further processing is started within
			 * its visitEnd method (see above in inner class ConstructorNode).
			 */
			return new ConstructorNode(access, name, desc, signature,
					exceptions);
		else
			return super.visitMethod(access, name, desc, signature, exceptions);
	}

	/**
	 * Processes the given completely read constructor node. I.e., analyzes the
	 * constructor and instantiates it by calling real referenced constructors.
	 * 
	 * @param constructorNode
	 */
	private void process(ConstructorNode constructorNode) {
		final ConstructorAnalysisResult analysisResult = analyze(constructorNode);
		if (analysisResult != null)
			createConstructors(analysisResult);
	}

	/**
	 * Extracts information from the given constructor about the contained
	 * constructor call. This is saved in the attribute
	 * {@link ConstructorCallAttribute} which will be removed from the
	 * constructor node if it is found.<br>
	 * Also, separates instructions of the constructor node into different
	 * arguments and remaining code.
	 * 
	 * @param constructorNode
	 * @return the results of analysis, or null if no
	 *         {@link ConstructorCallAttribute} is found
	 */
	private ConstructorAnalysisResult analyze(ConstructorNode constructorNode) {
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
			return null;
		}

		List<InsnList> argumentLoadInstructions = separateInstructions(constructorNode);
		InsnList outerClassInstanceLoadInstructions = argumentLoadInstructions
				.remove(0);
		return new ConstructorAnalysisResult(constructorNode,
				outerClassInstanceLoadInstructions, argumentLoadInstructions,
				constructorNode.instructions, constructorInvocationPattern,
				isSuperInvocation);
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

	/**
	 * Creates all instances of the given constructor by inserting instructions
	 * which call appropriate reference constructors.<br>
	 * Calls {@link #visitMethod(int, String, String, String, String[])} on
	 * super for every constructor to be created.
	 * 
	 * @param analysisResult
	 */
	private void createConstructors(ConstructorAnalysisResult analysisResult) {
		final Collection<List<ConcreteParameter>> constructorPool = analysisResult
				.isSuperInvocation() ? existingSuperConstructors
				: existingThisConstructors;
		final MethodNode constructorNode = analysisResult.getConstructorNode();
		@SuppressWarnings("unchecked")
		final String[] exceptions = ((List<String>) constructorNode.exceptions)
				.toArray(new String[0]);
		// Loop over potential targets of the constructor call:
		for (List<ConcreteParameter> constructorParameters : constructorPool) {
			final ConstructorPatternMatcher matcher = new ConstructorPatternMatcher(
					constructorParameters, classLoader);
			analysisResult.getConstructorInvocationPattern().accept(matcher);
			if (matcher.hasFailed())
				continue;

			/*
			 * The potential target constructor does indeed match the call
			 * pattern. Thus, create a new constructor which actually calls this
			 * target constructor:
			 */
			final InsnList instructions = composeInstructions(
					constructorParameters, analysisResult);
			if (instructions == null)
				continue;
			if (!saveConstructor(constructorNode))
				continue;
			constructorNode.instructions = instructions;
			final MethodVisitor methodVisitor = super
					.visitMethod(constructorNode.access, constructorNode.name,
							constructorNode.desc, constructorNode.signature,
							exceptions);
			if (methodVisitor != null)
				constructorNode.accept(methodVisitor);
		}
	}

	/**
	 * @param constructorParameters
	 * @param analysisResult
	 * @return instructions for a constructor calling the constructor with the
	 *         given parameters, or null if no such constructor can be composed
	 */
	private InsnList composeInstructions(
			List<ConcreteParameter> constructorParameters,
			ConstructorAnalysisResult analysisResult) {
		InsnList instructions = new InsnList();

		/*
		 * Load this to the stack which will be the return value.
		 */
		instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));

		/*
		 * Load outer class instance.
		 */
		instructions.add(Cloner.clone(analysisResult
				.getOuterClassInstanceLoadInstructions()));

		/*
		 * Load argument values.
		 */
		PatternArgumentLoadingInstructionComposer argumentLoadingInstructionComposer = new PatternArgumentLoadingInstructionComposer(
				analysisResult.getArgumentLoadInstructions());
		analysisResult.getConstructorInvocationPattern().accept(
				argumentLoadingInstructionComposer);
		instructions.add(argumentLoadingInstructionComposer
				.getInstructionList());

		/*
		 * Call referenced constructor.
		 */
		Type[] argumentTypes = new Type[constructorParameters.size() + 1];
		argumentTypes[0] = TYPE_CJOBJECTIFC;
		for (int i = 1; i < argumentTypes.length; i++)
			try {
				argumentTypes[i] = Type.getType(ClassAccess.forName(
						constructorParameters.get(i - 1).getTypeName(), false,
						classLoader));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return null;
			}
		instructions
				.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, analysisResult
						.isSuperInvocation() ? superClassName : className,
						CONSTRUCTOR_METHOD_NAME, Type.getMethodDescriptor(
								Type.VOID_TYPE, argumentTypes)));

		/*
		 * Append remaining instructions.
		 */
		instructions
				.add(Cloner.clone(analysisResult.getEffectiveInstructions()));
		return instructions;
	}

	/**
	 * Adds the specified constructor to the list
	 * {@link #existingThisConstructors}.
	 * 
	 * @param constructorNode
	 * @return true if no constructor with the given parameters was existing
	 *         before
	 */
	private boolean saveConstructor(MethodNode constructorNode) {
		/*
		 * TODO Actual arguments of this constructor might later depend on the
		 * matched constructor.
		 */
		Type[] argumentTypes = Type.getArgumentTypes(constructorNode.desc);
		List<ConcreteParameter> parameters = new ArrayList<ConcreteParameter>();
		/*
		 * Skip the first type because it is always CjObjectItf (outer class
		 * type):
		 */
		for (int i = 1; i < argumentTypes.length; i++) {
			Type type = argumentTypes[i];
			/*
			 * TODO Test whether this works for primitive types. TODO Set name
			 * of parameter. Use annotations for named parameters?
			 */
			parameters.add(new ConcreteParameter(type.getClassName()));
		}
		return existingThisConstructors.add(parameters);
	}

	private static boolean isConstructor(String name) {
		return CONSTRUCTOR_METHOD_NAME.equals(name);
	}

}
