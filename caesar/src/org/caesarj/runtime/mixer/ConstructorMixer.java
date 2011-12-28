package org.caesarj.runtime.mixer;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.caesarj.runtime.constructors.ConcreteParameter;
import org.caesarj.runtime.constructors.ConstructorAnalysisResult;
import org.caesarj.runtime.constructors.ConstructorPatternMatcher;
import org.caesarj.runtime.constructors.PatternArgumentLoadingInstructionComposer;
import org.caesarj.util.ClassAccess;
import org.caesarj.util.Cloner;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ConstructorMixer extends ClassVisitor {

	private static final Type TYPE_CJOBJECTIFC = Type
			.getType("Lorg/caesarj/runtime/CjObjectIfc;");

	private static final String CONSTRUCTOR_METHOD_NAME = "<init>";

	private final List<ConstructorAnalysisResult> constructorAnalysisResults;

	private int nextConstructor = 0;

	private final List<List<ConcreteParameter>> existingSuperConstructors = new ArrayList<List<ConcreteParameter>>();

	private final Set<List<ConcreteParameter>> existingThisConstructors = new HashSet<List<ConcreteParameter>>();

	private final ClassLoader classLoader;

	private String superClassName;

	private String className;

	/**
	 * @param constructorAnalysisResults
	 *            the list of constructor calls appearing in the constructors of
	 *            the visited class in the order in which the constructors are
	 *            visited
	 * @param classLoader
	 *            the class loader to be used to load related (super) classes
	 * @param cv
	 *            next visitor in chain
	 * @throws IllegalArgumentException
	 *             if constructorCalls or classLoader is null
	 */
	public ConstructorMixer(
			List<ConstructorAnalysisResult> constructorAnalysisResults,
			ClassLoader classLoader, ClassVisitor cv)
			throws IllegalArgumentException {
		super(Opcodes.ASM4, cv);
		if (constructorAnalysisResults == null || classLoader == null)
			throw new IllegalArgumentException(
					"constructorCalls and classLoader must not be null.");
		this.constructorAnalysisResults = constructorAnalysisResults;
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
		if (!isConstructor(name))
			return super.visitMethod(access, name, desc, signature, exceptions);
		if (nextConstructor >= constructorAnalysisResults.size()) {
			System.err
					.format("ConstructorMixer.visitMethod: No ConstructorAnalysisResult found for method %s%n",
							name);
			return null;
		}

		final ConstructorAnalysisResult analysisResult = constructorAnalysisResults
				.get(nextConstructor++);
		/*
		 * TODO Actual arguments of this constructor might later depend on the
		 * matched constructor.
		 */
		Type[] argumentTypes = Type.getArgumentTypes(desc);
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
		if (existingThisConstructors.contains(parameters))
			return null;

		if (createConstructors(access, name, desc, signature, exceptions,
				analysisResult))
			existingThisConstructors.add(parameters);

		return null;
	}

	/**
	 * Creates all instances of the given constructor by inserting instructions
	 * which call appropriate reference constructors.<br>
	 * Calls {@link #visitMethod(int, String, String, String, String[])} on
	 * super for every constructor to be created.
	 * 
	 * @param exceptions
	 * @param signature
	 * @param desc
	 * @param name
	 * @param access
	 * @param analysisResult
	 * @return true if at least one constructor was created
	 */
	private boolean createConstructors(int access, String name, String desc,
			String signature, String[] exceptions,
			ConstructorAnalysisResult analysisResult) {
		boolean createdAtLeastOneConstructor = false;
		final Collection<List<ConcreteParameter>> constructorPool = analysisResult
				.isSuperInvocation() ? existingSuperConstructors
				: existingThisConstructors;
		// Loop over potential targets of the constructor call:
		for (List<ConcreteParameter> constructorParameters : constructorPool) {
			ConstructorPatternMatcher matcher = new ConstructorPatternMatcher(
					constructorParameters, classLoader);
			analysisResult.getConstructorInvocationPattern().accept(matcher);
			if (matcher.hasFailed())
				continue;

			/*
			 * The potential target constructor does indeed match the call
			 * pattern. Thus, create a new constructor which actually calls this
			 * target constructor:
			 */
			InsnList instructions = composeInstructions(constructorParameters,
					analysisResult);
			if (instructions == null)
				continue;
			MethodNode constructorNode = analysisResult.getConstructorNode();
			constructorNode.instructions = instructions;
			constructorNode.accept(super.visitMethod(access, name, desc,
					signature, exceptions));
			createdAtLeastOneConstructor = true;
		}
		return createdAtLeastOneConstructor;
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

	private static boolean isConstructor(String name) {
		return CONSTRUCTOR_METHOD_NAME.equals(name);
	}

}
