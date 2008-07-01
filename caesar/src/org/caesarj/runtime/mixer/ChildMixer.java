package org.caesarj.runtime.mixer;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * This class adapter allows caesar classes to extend mixins. It's job is to
 * read the "de.tud.caesarj.mixerconfig" attribute and replace the special
 * identifier "$$parent" by the name of an approbiate mixin to be generated
 * on-the-fly.
 * 
 * <p>
 * This class adapter is called "ChildMixer" because it must be applied to every
 * caesar class wich may be the child of a mixin.
 * 
 * <p>
 * The ASM Framework provides an event-based class file parser and generator.
 * ChildMixer extends ClassAdapter, wich forwards all events to the class reader
 * specified in the constructor. This class adds special handling to some event
 * handlers before calling the super class method.
 * 
 * @author Tillmann Rendel
 */

public class ChildMixer extends ClassAdapter {
	
	
	

	/**
	 * This method adapter transforms the bytecode of the methods of transformed
	 * classes. It's instances are created in the
	 * {@link #visitMethodInsn(int, String, String, String) visitMethod} method
	 * for every method of the transformed class.
	 */
	private class MethodMixer extends MethodAdapter {
		/**
		 * Creates a new method mixer forwarding events to the given method visitor.
		 * @param mv the method visitor forwarding events to
		 */
		public MethodMixer(MethodVisitor mv) {
			super(mv);			
		}

		/**
		 * Visits a method instruction. A method instruction is an instruction
		 * that invokes a method.
		 * 
		 * <p>
		 * This handler updates the owner to the new superclass (if needed)
		 * before forwarding the event.
		 * 
		 * @param opcode
		 *            the opcode of the type instruction to be visited. This
		 *            opcode is either INVOKEVIRTUAL, INVOKESPECIAL,
		 *            INVOKESTATIC or INVOKEINTERFACE.
		 * @param owner
		 *            the internal name of the method's owner class (see {@link
		 *            Type#getInternalName() getInternalName}).
		 * @param name
		 *            the method's name.
		 * @param desc
		 *            the method's descriptor (see {@link Type Type}).
		 */
		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			// fix reference to the owner type
			owner = update(owner);
			// fix descriptor of the call to the super constructor
			if (name.equals("<init>") && owner.equals(info.newSuper)) {
				if (info.newSuperOut != null) {
					desc = "(Ljava/lang/Object;)V"; 					
				}				
			}
			super.visitMethodInsn(opcode, owner, name, desc);			
		}
	}
	
	/**
	 * Information about transformation computed from the Caesar attribute.
	 * null - if the attribute is not found
	 */
	private ChildMixerInfo info;

	/**
	 * Creates a new child mixer forwarding events to the given class visitor,
	 * and relying upon the provided mixin registry for mixin management.
	 * 
	 * @param cv
	 *            class visitor to forward events to
	 * @param mixins
	 *            mixin registry managing the mixins for this application
	 */
	public ChildMixer(ClassVisitor cv, ChildMixerInfo info) {
		super(cv);
		this.info = info;
	}

    /**
	 * Visits the header of the class.
	 * 
	 * <p>
	 * This handler updates the superName from $$super to a newly created mixin
	 * id before forwarding the event.
	 * 
	 * @param version
	 *            the class version.
	 * @param access
	 *            the class's access flags (see {@link Opcodes}). This
	 *            parameter also indicates if the class is deprecated.
	 * @param name
	 *            the internal name of the class (see
	 *            {@link Type#getInternalName() getInternalName}).
	 * @param signature
	 *            the signature of this class. May be <tt>null</tt> if the
	 *            class is not a generic one, and does not extend or implement
	 *            generic classes or interfaces.
	 * @param superName
	 *            the internal of name of the super class (see
	 *            {@link Type#getInternalName() getInternalName}). For
	 *            interfaces, the super class is {@link Object}. May be
	 *            <tt>null</tt>, but only for the {@link Object} class.
	 * @param interfaces
	 *            the internal names of the class's interfaces (see
	 *            {@link Type#getInternalName() getInternalName}). May be
	 *            <tt>null</tt>.
	 */
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, update(superName), interfaces);
	}

    /**
	 * Visits a method of the class. This method <i>must</i> return a new
	 * {@link MethodVisitor} instance (or <tt>null</tt>) each time it is
	 * called, i.e., it should not return a previously returned visitor.
	 * 
	 * <p>
	 * This handler creates a new {@link MethodMixer} on top of the method
	 * visitor returned by the next transformer in the chain.
	 * 
	 * @param access
	 *            the method's access flags (see {@link Opcodes}). This
	 *            parameter also indicates if the method is synthetic and/or
	 *            deprecated.
	 * @param name
	 *            the method's name.
	 * @param desc
	 *            the method's descriptor (see {@link Type Type}).
	 * @param signature
	 *            the method's signature. May be <tt>null</tt> if the method
	 *            parameters, return type and exceptions do not use generic
	 *            types.
	 * @param exceptions
	 *            the internal names of the method's exception classes (see
	 *            {@link Type#getInternalName() getInternalName}). May be
	 *            <tt>null</tt>.
	 * @return an object to visit the byte code of the method, or <tt>null</tt>
	 *         if this class visitor is not interested in visiting the code of
	 *         this method.
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		// if the next transformer in the chain ignores this method, we don't
		// bother to transform it. otherwhise, we create a method transformer
		// chain similar to the class transformer chain already running
		
		// once again, a Maybe monad for Java would be useful to handle this :)
		MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
		if (next == null) 
			return null;
		else
			return new MethodMixer(next);
	}

	/**
	 * Transforms a class name by replacing the special name "$$super" by the
	 * name of an approbiate mixin. The mixin id to be used is lazily created
	 * when update is first called with name = "$$super".
	 * 
	 * @param name
	 *            old class name
	 * @return transformed class name
	 */
	private String update(String name) {
		// substitute "$$super" by a lazily created mixin name
		if (name.equals("$$super")) {
			return info.newSuper;
		}

		// substitute names of superclasses by the approbiate mixin names
		if (info.mapping.containsKey(name)) 
			return info.mapping.get(name);

		// return other names unchanged
		return name;
	}
}
