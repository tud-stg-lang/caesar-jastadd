package org.caesarj.runtime.mixer;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.caesarj.runtime.constructors.ConcreteParameter;
import org.caesarj.runtime.constructors.ConstructorPatternMatcher;
import org.caesarj.runtime.mixer.ConstructorAnalyzer.ConstructorCall;
import org.caesarj.util.ClassAccess;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ConstructorMixer extends ClassVisitor {

	private class SingleConstructorMixer extends MethodVisitor {

		private String desc;

		private boolean done = false;

		/**
		 * A mixer which replaces the constructor invocation within a method by
		 * a call to the constructor with the given name, descriptor and owner.
		 * 
		 * @param desc
		 * @param delegateVisitor
		 */
		public SingleConstructorMixer(String desc, MethodVisitor delegateVisitor) {
			super(Opcodes.ASM4, delegateVisitor);
			this.desc = desc;
		}

		@Override
		public void visitAttribute(Attribute attr) {
			/*
			 * ConstructorCallAttributes do not have to be kept after loading
			 * the class.
			 */
			if (!(attr instanceof ConstructorCallAttribute))
				super.visitAttribute(attr);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name,
				String desc) {
			if (done || opcode != Opcodes.INVOKESPECIAL || !isConstructor(name)) {
				super.visitMethodInsn(opcode, owner, name, desc);
				return;
			}
			done = true;
			super.visitMethodInsn(opcode, superClassName, name, this.desc);
		}

	}

	private static final String CONSTRUCTOR_METHOD_NAME = "<init>";

	private final List<ConstructorCall> constructorCalls;

	private int nextConstructor = 0;

	private final List<List<ConcreteParameter>> existingSuperConstructors = new ArrayList<List<ConcreteParameter>>();

	private final Set<List<ConcreteParameter>> existingThisConstructors = new HashSet<List<ConcreteParameter>>();

	private final ClassLoader classLoader;

	private String superClassName;

	/**
	 * @param constructorCalls
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
	public ConstructorMixer(List<ConstructorCall> constructorCalls,
			ClassLoader classLoader, ClassVisitor cv)
			throws IllegalArgumentException {
		super(Opcodes.ASM4, cv);
		if (constructorCalls == null || classLoader == null)
			throw new IllegalArgumentException(
					"constructorCalls and classLoader must not be null.");
		this.constructorCalls = constructorCalls;
		this.classLoader = classLoader;
	}

	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
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
		ConstructorCall call = constructorCalls.get(nextConstructor++);
		if (call == null) {
			System.err
					.format("ConstructorMixer.visitMethod: Expected to have a ConstructorCall object for method %s%n",
							name);
			return null;
		}

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

		SingleConstructorMixer constructorMixers[] = createSingleConstructorMixers(
				access, name, desc, signature, exceptions, call);
		if (constructorMixers == null)
			return null;

		existingThisConstructors.add(parameters);
		return new ListDelegationMethodVisitor(constructorMixers);
	}

	/**
	 * @param exceptions
	 * @param signature
	 * @param desc
	 * @param name
	 * @param access
	 * @param call
	 * @return one {@link SingleConstructorMixer} for each matching constructor
	 *         with respect to the given constructor call, or null if there is
	 *         no matching called constructor
	 */
	private SingleConstructorMixer[] createSingleConstructorMixers(int access,
			String name, String desc, String signature, String[] exceptions,
			ConstructorCall call) {
		List<SingleConstructorMixer> constructorMixers = new ArrayList<ConstructorMixer.SingleConstructorMixer>();
		Collection<List<ConcreteParameter>> constructorPool = call.isSuper() ? existingSuperConstructors
				: existingThisConstructors;
		constructorLoop: for (List<ConcreteParameter> constructorParameters : constructorPool) {
			ConstructorPatternMatcher matcher = new ConstructorPatternMatcher(
					constructorParameters, classLoader);
			call.getParameterPattern().accept(matcher);
			if (matcher.hasFailed())
				continue;
			Type[] argumentTypes = new Type[constructorParameters.size() + 1];
			argumentTypes[0] = Type
					.getType("Lorg/caesarj/runtime/CjObjectIfc;");
			for (int i = 1; i < argumentTypes.length; i++)
				try {
					argumentTypes[i] = Type.getType(ClassAccess
							.forName(constructorParameters.get(i - 1)
									.getTypeSignature(), false, classLoader));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					continue constructorLoop;
				}
			constructorMixers.add(new SingleConstructorMixer(Type
					.getMethodDescriptor(Type.VOID_TYPE, argumentTypes), super
					.visitMethod(access, name, desc, signature, exceptions)));
		}
		return constructorMixers.isEmpty() ? null : constructorMixers
				.toArray(new SingleConstructorMixer[0]);
	}

	private static boolean isConstructor(String name) {
		return CONSTRUCTOR_METHOD_NAME.equals(name);
	}

}
