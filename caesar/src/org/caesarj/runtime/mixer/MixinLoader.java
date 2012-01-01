package org.caesarj.runtime.mixer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

public class MixinLoader extends ClassLoader {
	private boolean trace = false;

	private static final Attribute[] attributes = new Attribute[] {
			new MixerConfigAttribute(), new ConditionalConstructorAttribute() };

	private final MixinRegistry mixins = new MixinRegistry();

	public void setTrace(boolean on) {
		trace = on;
	}

	public static final String systemClassDomains[] = { "java.", "javax.",
			"sun.", "sunw.", "com.sun.", "org.ietf.", "org.jcp.", "org.omg.",
			"org.w3c.", "org.xml." };

	boolean isSystemClass(String name) {
		for (String prefix : systemClassDomains) {
			if (name.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {

		if (trace)
			System.err.format("\nloadClass(%s, %s)\n", name, resolve);
		Class<?> result = null;

		if (isSystemClass(name)) {

			// try to load as a system class
			if (trace)
				System.err.format("system class ~~~> ", name);
			try {
				result = findSystemClass(name);
			} catch (ClassNotFoundException e) {
				if (trace)
					System.err.format("%s\n", e);
				throw e;
			}
		} else {
			// try to load as a normal class
			if (trace)
				System.err.format("normal class ~~~> ", name);
			result = loadNormalClass(name);

			// try to load as a mixin class
			if (result == null) {
				if (trace)
					System.err.format("mixin  class ~~~> ", name);
				try {
					result = loadMixinClass(name);
				} catch (ClassNotFoundException e) {
					if (trace)
						System.err.format("%s\n", e);
					throw e;
				}
			}
		}

		if (resolve)
			resolveClass(result);

		return result;
	}

	private Class<?> loadNormalClass(String name) {
		InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
		if (in == null) {
			return null;
		}

		try {
			byte[] b = readBytes(in);
			return defineClass(name, b, 0, b.length);
		} catch (IOException e) {
			if (trace)
				System.err.format("%s\n", e);
			return null;
		}
	}

	private byte[] readBytes(InputStream in) throws IOException {
		// set up a list of buffers ...
		final int bufferSize = 1024;
		List<byte[]> buffers = new ArrayList<byte[]>();
		byte[] buffer = new byte[bufferSize];

		// .. fill them with data from the InputStream ...
		int offset = 0;
		int n = in.read(buffer, offset, bufferSize - offset);
		while (n > 0) {
			offset += n;
			if (offset == bufferSize) {
				buffers.add(buffer);
				buffer = new byte[bufferSize];
				offset = 0;
			}

			n = in.read(buffer, offset, bufferSize - offset);
		}

		// ... and finally copy them together to form a single big buffer
		int len = buffers.size() * bufferSize + offset;
		byte[] result = new byte[len];
		for (int i = 0; i < buffers.size(); i++)
			System.arraycopy(buffers.get(i), 0, result, i * bufferSize,
					bufferSize);
		System.arraycopy(buffer, 0, result, buffers.size() * bufferSize, offset);

		return result;
	}

	private Class<?> findMixinCopyDecl(String name)
			throws ClassNotFoundException {
		// internal id
		int index = name.lastIndexOf("$$");
		int id = Integer.parseInt(name.substring(index + 2));

		// already cached?
		if (mixins.isCached(id))
			return mixins.fetch(id);

		// fetch prepared mixin information
		String baseClass = mixins.getBaseClass(id);
		String superClass = mixins.getSuperClass(id);
		String outClass = mixins.getOutClass(id);
		String superOut = mixins.getSuperOut(id);

		// load binary data of base class
		final ClassReader reader = createClassReader(name,
				getResourceAsStream(baseClass.replace('.', '/') + ".cjclass"));

		// transform classes and write them
		// TODO compute frames if necessary
		//final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		final ClassWriter writer = new ClassWriter(0);
		ClassVisitor target = writer;
		if (trace)
			target = new TraceClassVisitor(writer, new PrintWriter(System.err));
		target = new ConstructorMixer(this, target);
		target = new MixinMixer(target, name, superClass, outClass, superOut);

		// transform mixin data
		reader.accept(target, attributes, 0);
		final byte[] data = writer.toByteArray();

		// process transformed mixin data
		final Class<?> result = defineClass(name, data, 0, data.length);
		mixins.cache(id, result);
		return result;
	}

	private Class<?> findMixinClassDecl(String name)
			throws ClassNotFoundException {
		// load binary class data
		final InputStream in = getResourceAsStream(name.replace('.', '/')
				+ ".cjclass");

		final ClassReader reader = createClassReader(name, in);

		// FIRST PASS
		// prepare transformation chain
		final CaesarAttributeVisitor attrVis = new CaesarAttributeVisitor(
				mixins);
		reader.accept(attrVis, attributes, 0);

		// SECOND PASS
		// transform classes and write them
		// TODO compute frames if necessary
		//final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		final ClassWriter writer = new ClassWriter(0);
		ClassVisitor target = writer;
		if (trace)
			target = new TraceClassVisitor(target, new PrintWriter(System.err));

		target = new ConstructorMixer(this, target);

		// classes without Caesar attribute do not need transformation
		if (attrVis.getInfo() != null)
			target = new ChildMixer(target, attrVis.getInfo());

		reader.accept(target, attributes, 0);
		final byte[] data = writer.toByteArray();

		// process transformed mixin data
		return defineClass(name, data, 0, data.length);
	}

	private Class<?> loadMixinClass(String name) throws ClassNotFoundException {
		if (name.indexOf("$$") != -1)
			return findMixinCopyDecl(name);
		else
			return findMixinClassDecl(name);
	}

	private ClassReader createClassReader(String name, InputStream in)
			throws ClassNotFoundException {
		if (in == null)
			throw new ClassNotFoundException("i/o problems while loading "
					+ name);

		try {
			return new ClassReader(in);
		} catch (IOException e) {
			throw new ClassNotFoundException("i/o problems while loading "
					+ name, e);
		}
	}

	public static void main(String args[]) throws Exception {
		if (args.length < 1) {
			System.err.println("usage: java MixinLoader <MainClass>");
			return;
		}

		String mainClass = args[0];
		String[] mainArgs = new String[args.length - 1];
		System.arraycopy(args, 1, mainArgs, 0, mainArgs.length);

		MixinLoader loader = new MixinLoader();
		Class<?> main = loader.loadClass(mainClass);
		Method mainMethod = main.getMethod("main",
				new Class[] { mainArgs.getClass() });
		try {
			mainMethod.invoke(null, new Object[] { mainArgs });
		} catch (InvocationTargetException e) {
			e.getCause().printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
