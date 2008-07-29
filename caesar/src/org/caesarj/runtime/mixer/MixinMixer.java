package org.caesarj.runtime.mixer;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MixinMixer extends ClassAdapter {
	private final String newName;
	private final String newSuper;
	private final String newOut;
	private final String newSuperOut;
	private String oldName;
	private String oldSuper;
	private String oldOut;
	private String oldSuperOut;
	
	private class MethodMixer extends MethodAdapter {
		public MethodMixer(MethodVisitor mv) {
			super(mv);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			super.visitFieldInsn(opcode, update(owner), name, desc);
		}
		
		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			owner = update(owner);
			
			// fix descriptor of the call to the super constructor
			super.visitMethodInsn(opcode, owner, name, desc);
		}
	}

	public MixinMixer(ClassVisitor cv, String newName, String newSuper, String newOut, String newSuperOut) {
		super(cv);
		this.newName = newName.replace('.', '/');
		this.newSuper = newSuper.replace('.', '/');
		this.newOut = newOut != null ? newOut.replace('.', '/') : null;
		this.newSuperOut = newSuperOut != null ? newSuperOut.replace('.', '/') : null;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.oldName = name;
		this.oldSuper = superName;
		this.oldSuperOut = oldSuper.contains("$") ? oldSuper.substring(0, oldSuper.lastIndexOf('$')) : null;
		super.visit(version, access, update(name), signature, update(superName), interfaces);
	}
	
	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (name.equals(oldName))
			oldOut = MixinInformation.getOutName(name);
		super.visitInnerClass(update(name), update(outerName), update(innerName), access);
	}
		
	public String update(String name) {
		if (name == null) 
			return null;
		if (name.equals(oldName))
			return newName;
		else if (oldSuper != null && name.equals(oldSuper))
			return newSuper;
		else if (oldOut != null && name.equals(oldOut))
			return newOut;
		else return name;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		// ignore static methods (except static initializers)
		if ((access & Opcodes.ACC_STATIC) != 0 && !name.equals("<clinit>"))
			return null;

		// general method handling
		MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
		return new MethodMixer(next);
	}	
}
