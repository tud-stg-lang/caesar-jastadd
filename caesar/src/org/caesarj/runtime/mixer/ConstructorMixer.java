package org.caesarj.runtime.mixer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.caesarj.ast.CodeGeneration;
import org.caesarj.runtime.constructors.APosterioriConstructorAnalysisResult;
import org.caesarj.runtime.constructors.APrioriConstructorAnalysisResult;
import org.caesarj.runtime.constructors.CallingConstructorParameterPatternAnalyzer;
import org.caesarj.runtime.constructors.ConcreteParameter;
import org.caesarj.runtime.constructors.ParameterName;
import org.caesarj.runtime.constructors.ParameterPattern;
import org.caesarj.runtime.constructors.PatternArgumentLoadingInstructionComposer;
import org.caesarj.runtime.constructors.PatternToParameterMatcher;
import org.caesarj.util.ClassAccess;
import org.caesarj.util.Cloner;
import org.caesarj.util.InstructionTransformer;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
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

	/**
	 * variable index of the first parameter after $cj$outer
	 */
	private static final int FIRST_PARAMETER_INDEX = 2;

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
		if (superConstructors != null)
			saveSuperConstructors(superConstructors);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	/**
	 * Saves the given super constructor in the necessary list representation in
	 * {@link #existingSuperConstructors}.
	 * 
	 * @param superConstructors
	 *            the super constructors, not null
	 */
	private void saveSuperConstructors(Constructor<?>[] superConstructors) {
		for (Constructor<?> constructor : superConstructors) {
			final List<ConcreteParameter> parameters = new ArrayList<ConcreteParameter>();
			final Class<?>[] parameterTypes = constructor.getParameterTypes();
			final Annotation[][] parameterAnnotations = constructor
					.getParameterAnnotations();
			/*
			 * Skip the first type because it is always CjObjectItf (outer class
			 * type):
			 */
			for (int i = 1; i < parameterTypes.length; i++) {
				Class<?> type = parameterTypes[i];
				String name = null;
				for (Annotation annotation : parameterAnnotations[i]) {
					if (annotation.annotationType().equals(ParameterName.class)) {
						name = ((ParameterName) annotation).value();
						break;
					} else if (annotation instanceof Proxy
							&& annotation.annotationType().getName()
									.equals(ParameterName.class.getName())) {
						try {
							name = (String) Proxy.getInvocationHandler(
									annotation).invoke(annotation,
									ParameterName.class.getMethod("value"),
									null);
						} catch (Throwable e) {
							e.printStackTrace();
							continue;
						}
						break;
					}
				}
				parameters.add(new ConcreteParameter(name, type.getName()));
			}
			existingSuperConstructors.add(parameters);
		}
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
			/*
			 * Just forward this method to the next class visitor if it is not a
			 * constructor and, thus, does not need to be changed.
			 */
			return super.visitMethod(access, name, desc, signature, exceptions);
	}

	private static boolean isConstructor(String name) {
		return CONSTRUCTOR_METHOD_NAME.equals(name);
	}

	/**
	 * Processes the given completely read constructor node. I.e., analyzes the
	 * constructor and instantiates it by calling real referenced constructors.
	 * 
	 * @param constructorNode
	 */
	private void process(ConstructorNode constructorNode) {
		final APrioriConstructorAnalysisResult constructorCallAnalysisResult = analyze(constructorNode);
		if (constructorCallAnalysisResult == null)
			/*
			 * This is a "normal" constructor because there is no
			 * ConstructorCallAttribute associated with it. Hence, just create
			 * the constructor without change.
			 */
			createConstructorForNode(constructorNode);
		else
			/*
			 * This is a conditional constructor. Proceed with further analysis
			 * and create appropriate concrete constructors.
			 */
			createConstructors(constructorCallAnalysisResult);
	}

	/**
	 * Extracts information from the given constructor about the contained
	 * constructor call. This is saved in the attribute
	 * {@link ConditionalConstructorAttribute} which will be removed from the
	 * constructor node if it is found.<br>
	 * Also, separates instructions of the constructor node into different
	 * arguments and remaining code.
	 * 
	 * @param constructorNode
	 * @return the results of analysis, or null if no
	 *         {@link ConditionalConstructorAttribute} is found
	 */
	private APrioriConstructorAnalysisResult analyze(
			ConstructorNode constructorNode) {
		ParameterPattern constructorPattern = null;
		ParameterPattern constructorInvocationPattern = null;
		boolean isSuperInvocation = true;
		@SuppressWarnings("unchecked")
		Iterator<Attribute> attrIterator = constructorNode.attrs.iterator();
		while (attrIterator.hasNext()) {
			Attribute attribute = attrIterator.next();
			if (!(attribute instanceof ConditionalConstructorAttribute))
				continue;
			ConditionalConstructorAttribute constructorCallAttribute = (ConditionalConstructorAttribute) attribute;
			constructorPattern = constructorCallAttribute.getPattern();
			constructorInvocationPattern = constructorCallAttribute
					.getCalledPattern();
			isSuperInvocation = constructorCallAttribute
					.isSuperConstructorCall();
			attrIterator.remove();
			break;
		}
		if (constructorPattern == null)
			return null;

		List<InsnList> argumentLoadInstructions = separateInstructions(constructorNode);
		return new APrioriConstructorAnalysisResult(constructorNode,
				argumentLoadInstructions, constructorNode.instructions,
				constructorNode.maxLocals, constructorNode.maxStack,
				constructorPattern, constructorInvocationPattern,
				isSuperInvocation);
	}

	/**
	 * @param constructorNode
	 *            the constructor node to be analyzed<br>
	 *            The instructions of this node will be modified by deleting all
	 *            instructions which are used for loading arguments for a
	 *            constructor invocation.
	 * @return instructions used to push constructor call arguments onto the
	 *         stack, separated per argument<br>
	 *         This method assumes constructors to have the following (pseudo)
	 *         format:<br>
	 *         <code>
	 *         (LOAD_ARGUMENT, NOP)*<br>
	 *         NOP<br>
	 *         REST_OF_CONSTRUCTOR
	 *         </code><br>
	 *         Thereby, LOAD_ARGUMENT may not contain NOPs, and
	 *         REST_OF_CONSTRUCTOR may not contain a constructor invocation.<br>
	 *         TODO Can we separate the arguments in a less fragile way?
	 * @see org.caesarj.ast.CondConstructorAccess#createBCode(CodeGeneration)
	 */
	private List<InsnList> separateInstructions(MethodNode constructorNode) {
		final List<InsnList> argumentLoadInstructions = new ArrayList<InsnList>();
		@SuppressWarnings("unchecked")
		/*
		 * Skip the first instruction which loads this onto the stack:
		 */
		ListIterator<AbstractInsnNode> instructionIterator = constructorNode.instructions
				.iterator();
		InsnList currentArgument = new InsnList();
		boolean allArgumentsPassed = false;
		boolean sawArgumentInstruction = false;
		do {
			AbstractInsnNode node = instructionIterator.next();
			instructionIterator.remove();
			switch (node.getOpcode()) {
			case Opcodes.NOP:
				if (!sawArgumentInstruction) {
					allArgumentsPassed = true;
					break;
				}
				argumentLoadInstructions.add(currentArgument);
				currentArgument = new InsnList();
				sawArgumentInstruction = false;
				break;
			default:
				currentArgument.add(node);
				if (!(node instanceof LabelNode || node instanceof LineNumberNode))
					sawArgumentInstruction = true;
			}
		} while (instructionIterator.hasNext() && !allArgumentsPassed);
		return argumentLoadInstructions;
	}

	private void createConstructorForNode(MethodNode constructorNode) {
		@SuppressWarnings("unchecked")
		final MethodVisitor methodVisitor = super.visitMethod(
				constructorNode.access, constructorNode.name,
				constructorNode.desc, constructorNode.signature,
				((List<String>) constructorNode.exceptions)
						.toArray(new String[0]));
		if (methodVisitor != null)
			constructorNode.accept(methodVisitor);
	}

	/**
	 * Creates all instances of the given constructor by inserting instructions
	 * which call appropriate reference constructors.<br>
	 * Calls {@link #visitMethod(int, String, String, String, String[])} on
	 * super for every constructor to be created.
	 * 
	 * @param aPrioriAnalysisResult
	 */
	private void createConstructors(
			APrioriConstructorAnalysisResult aPrioriAnalysisResult) {
		final Collection<List<ConcreteParameter>> constructorPool = aPrioriAnalysisResult
				.isSuperInvocation() ? existingSuperConstructors
				: existingThisConstructors;
		final Set<List<ConcreteParameter>> newThisConstructors = new HashSet<List<ConcreteParameter>>();
		final MethodNode constructorNode = aPrioriAnalysisResult
				.getConstructorNode();
		// Loop over potential targets of the constructor call:
		for (List<ConcreteParameter> calledConstructorParameters : constructorPool) {
			/*
			 * Match the call pattern against the concrete parameters of the
			 * currently considered target constructor:
			 */
			final Map<ParameterPattern, List<Integer>> callPatternToParametersMap = matchPatternWithParameters(
					aPrioriAnalysisResult.getConstructorInvocationPattern(),
					calledConstructorParameters);
			if (callPatternToParametersMap == null)
				continue;

			/*
			 * Analyze the constructor with respect to the matching with the
			 * called constructor:
			 */
			final CallingConstructorParameterPatternAnalyzer aPosterioriAnalysisResult = new CallingConstructorParameterPatternAnalyzer(
					calledConstructorParameters, callPatternToParametersMap,
					FIRST_PARAMETER_INDEX, classLoader);
			aPrioriAnalysisResult.getConstructorPattern().accept(
					aPosterioriAnalysisResult);

			final List<Type> parameterTypes = new ArrayList<Type>(
					aPosterioriAnalysisResult.getParameterTypes());
			parameterTypes.add(0, TYPE_CJOBJECTIFC);

			/*
			 * The potential target constructor does indeed match the call
			 * pattern. Thus, create a new constructor which actually calls this
			 * target constructor:
			 */
			final InsnList instructions = composeInstructions(
					calledConstructorParameters, aPrioriAnalysisResult,
					aPosterioriAnalysisResult, callPatternToParametersMap);
			if (instructions == null)
				continue;

			if (!addThisConstructorIfNew(newThisConstructors,
					aPosterioriAnalysisResult))
				continue;

			/*
			 * Set constructorNode properties.
			 */
			constructorNode.instructions = instructions;
			constructorNode.desc = Type.getMethodDescriptor(Type.VOID_TYPE,
					parameterTypes.toArray(new Type[0]));
			constructorNode.localVariables = createLocalVariablesList(
					instructions, parameterTypes,
					(LocalVariableNode) constructorNode.localVariables.get(0));
			constructorNode.maxLocals = aPrioriAnalysisResult.getMaxLocals()
					+ aPosterioriAnalysisResult.getNewVariableIndexShift();
			/*
			 * This is a safe approximation for the necessary stack size because
			 * each new variable as well as $cj$outer ("+1") must be pushed onto
			 * the stack:
			 */
			constructorNode.maxStack = aPrioriAnalysisResult.getMaxStack() + 1
					+ aPosterioriAnalysisResult.getNewVariableIndexShift();
			// TODO save parameter names in annotation
			constructorNode.visibleParameterAnnotations = createParameterNameAnnotations(aPosterioriAnalysisResult);
			createConstructorForNode(constructorNode);
		}
		existingThisConstructors.addAll(newThisConstructors);
	}

	/**
	 * @param parameterPattern
	 * @param calledConstructorParameters
	 * @return a map which maps patterns of the matched pattern to concrete
	 *         matching parameters of the given list, or null if there is no
	 *         such match
	 */
	private Map<ParameterPattern, List<Integer>> matchPatternWithParameters(
			ParameterPattern parameterPattern,
			List<ConcreteParameter> calledConstructorParameters) {
		final PatternToParameterMatcher matcher = new PatternToParameterMatcher(
				calledConstructorParameters, classLoader);
		parameterPattern.accept(matcher);
		if (matcher.hasFailed())
			return null;
		Map<ParameterPattern, List<Integer>> callPatternToParametersMap = matcher
				.getPatternToParametersMap();
		return callPatternToParametersMap;
	}

	/**
	 * @param calledConstructorParameters
	 * @param aPrioriAnalysisResult
	 * @param aPosterioriAnalysisResult
	 * @param callPatternToParametersMap
	 *            Maps patterns of the constructor call to the index of the
	 *            concrete parameters such as defined by the list
	 *            calledConstructorParameters.
	 * @return instructions for a constructor calling the constructor with the
	 *         given parameters, or null if no such constructor can be composed
	 */
	private InsnList composeInstructions(
			List<ConcreteParameter> calledConstructorParameters,
			APrioriConstructorAnalysisResult aPrioriAnalysisResult,
			APosterioriConstructorAnalysisResult aPosterioriAnalysisResult,
			Map<ParameterPattern, List<Integer>> callPatternToParametersMap) {
		InsnList instructions = new InsnList();

		/*
		 * Load this to the stack which will be the return value.
		 */
		instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));

		/*
		 * Load outer class instance to pass it to the other constructor.
		 */
		instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));

		/*
		 * Load argument values.
		 */
		PatternArgumentLoadingInstructionComposer argumentLoadingInstructionComposer = new PatternArgumentLoadingInstructionComposer(
				calledConstructorParameters,
				aPrioriAnalysisResult.getArgumentLoadInstructions(),
				aPosterioriAnalysisResult, callPatternToParametersMap,
				classLoader);
		aPrioriAnalysisResult.getConstructorInvocationPattern().accept(
				argumentLoadingInstructionComposer);
		instructions.add(argumentLoadingInstructionComposer
				.getInstructionList());

		/*
		 * Call referenced constructor.
		 */
		Type[] argumentTypes = new Type[calledConstructorParameters.size() + 1];
		argumentTypes[0] = TYPE_CJOBJECTIFC;
		for (int i = 1; i < argumentTypes.length; i++)
			try {
				argumentTypes[i] = Type.getType(ClassAccess.forName(
						calledConstructorParameters.get(i - 1).getTypeName(),
						false, classLoader));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				return null;
			}
		instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
				aPrioriAnalysisResult.isSuperInvocation() ? superClassName
						: className, CONSTRUCTOR_METHOD_NAME, Type
						.getMethodDescriptor(Type.VOID_TYPE, argumentTypes)));

		/*
		 * Apply the remaining instructions of the constructor.
		 */
		final InsnList effectiveInstructions = Cloner
				.clone(aPrioriAnalysisResult.getEffectiveInstructions());
		new InstructionTransformer(
				aPosterioriAnalysisResult.getVariableIndexMap(),
				aPosterioriAnalysisResult.getNewVariableIndexShift())
				.transformVariableIndexes(effectiveInstructions);
		instructions.add(effectiveInstructions);
		return instructions;
	}

	/**
	 * Adds the constructor with the specified parameter types to
	 * {@link #existingThisConstructors}.
	 * 
	 * @param newThisConstructors
	 *            the set in which new this constructors are collected
	 * @param aPosterioriAnalysisResult
	 *            results of constructor analysis wrt a concrete called
	 *            constructor
	 * @return true if no constructor with the given parameters was existing
	 *         before
	 */
	private boolean addThisConstructorIfNew(
			Set<List<ConcreteParameter>> newThisConstructors,
			APosterioriConstructorAnalysisResult aPosterioriAnalysisResult) {
		final List<ConcreteParameter> parameters = new ArrayList<ConcreteParameter>();
		final List<Type> parameterTypes = aPosterioriAnalysisResult
				.getParameterTypes();
		final List<String> parameterNames = aPosterioriAnalysisResult
				.getParameterNames();
		final int countParameters = parameterTypes.size();
		for (int i = 0; i < countParameters; i++) {
			/*
			 * TODO Test whether this works for primitive types.
			 */
			parameters.add(new ConcreteParameter(parameterNames.get(i),
					parameterTypes.get(i).getClassName()));
		}
		if (existingThisConstructors.contains(parameters))
			return false;
		else
			return newThisConstructors.add(parameters);
	}

	/**
	 * Creates a description of local variables in the scope of the method. If
	 * necessary, start and end labels are added to the instructions to define
	 * the scope.
	 * 
	 * @param instructions
	 * @param parameterTypes
	 * @param thisVariable
	 *            the "this" variable of the constructor which is initially
	 *            always the first local variable
	 * @return a list of local variables such as defined at the beginning of the
	 *         constructor with the given parameter types and instructions
	 */
	private static List<LocalVariableNode> createLocalVariablesList(
			InsnList instructions, List<Type> parameterTypes,
			LocalVariableNode thisVariable) {
		/*
		 * localVariables initially contains only one parameter, namely "this"
		 * with index 0.
		 */
		// TODO Is it right that local variables for arguments always reach
		// from start label to end label?
		final LabelNode startLabel = getOrCreateStartLabel(instructions);
		final LabelNode endLabel = getOrCreateEndLabel(instructions);
		thisVariable.start = startLabel;
		thisVariable.end = endLabel;
		final List<LocalVariableNode> localVariables = new ArrayList<LocalVariableNode>(
				Collections.singleton(thisVariable));
		int index = 1;
		for (Type type : parameterTypes) {
			// TODO assign actual variable names
			localVariables.add(new LocalVariableNode(String
					.format("l%d", index), type.getDescriptor(), null,
					startLabel, endLabel, index));
			index += type.getSize();
		}
		return localVariables;
	}

	/**
	 * @param constructorNode
	 * @return the start LabelNode in the given instruction list which will be
	 *         inserted if it does not exist yet
	 */
	private static LabelNode getOrCreateStartLabel(InsnList instructions) {
		if (instructions.size() == 0
				|| !(instructions.getFirst() instanceof LabelNode))
			instructions.insert(new LabelNode(new Label()));
		return (LabelNode) instructions.getFirst();
	}

	/**
	 * @param constructorNode
	 * @return the end LabelNode in the given instruction list which will be
	 *         added if it does not exist yet
	 */
	private static LabelNode getOrCreateEndLabel(InsnList instructions) {
		if (instructions.size() == 0
				|| !(instructions.getLast() instanceof LabelNode))
			instructions.add(new LabelNode(new Label()));
		return (LabelNode) instructions.getLast();
	}

	/**
	 * @param aPosterioriAnalysisResult
	 * @return an array of parameter annotation lists such that each annotation
	 *         list contains a {@link ParameterName} annotation with the name of
	 *         the associated parameter if the name is available
	 */
	private static List<?>[] createParameterNameAnnotations(
			final APosterioriConstructorAnalysisResult aPosterioriAnalysisResult) {
		final List<List<AnnotationNode>> nodes = new ArrayList<List<AnnotationNode>>();
		final List<AnnotationNode> emptyList = Collections.emptyList();
		nodes.add(emptyList); // no annotation for $cj$outer
		final List<String> parameterNames = aPosterioriAnalysisResult
				.getParameterNames();
		final int size = parameterNames.size();
		for (int i = 0; i < size; i++) {
			final String name = parameterNames.get(i);
			if (name == null)
				continue;
			AnnotationNode annotationNode = new AnnotationNode(
					Type.getDescriptor(ParameterName.class));
			annotationNode.values = Arrays.asList("value", name);
			nodes.add(Collections.singletonList(annotationNode));
		}
		return nodes.toArray(new List[0]);
	}

}
