package org.caesarj.runtime.mixer;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ListDelegationMethodVisitor extends MethodVisitor {

	private final MethodVisitor[] delegates;

	public ListDelegationMethodVisitor(MethodVisitor[] delegates) {
		super(Opcodes.ASM4);
		this.delegates = delegates;
	}

	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		if (delegates != null) {

			for (MethodVisitor visitor : delegates)
				visitor.visitAnnotationDefault();
		}
		return super.visitAnnotationDefault();
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (delegates == null)
			return null;
		List<AnnotationVisitor> subDelegates = new ArrayList<AnnotationVisitor>();
		for (MethodVisitor visitor : delegates) {
			AnnotationVisitor subDelegate = visitor.visitAnnotation(desc,
					visible);
			if (subDelegate != null)
				subDelegates.add(subDelegate);
		}
		return new ListDelegationAnnotationVisitor(
				subDelegates.toArray(new AnnotationVisitor[0]));
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter,
			String desc, boolean visible) {
		if (delegates == null)
			return null;
		List<AnnotationVisitor> subDelegates = new ArrayList<AnnotationVisitor>();
		for (MethodVisitor visitor : delegates) {
			AnnotationVisitor subDelegate = visitor.visitParameterAnnotation(
					parameter, desc, visible);
			if (subDelegate != null)
				subDelegates.add(subDelegate);
		}
		return new ListDelegationAnnotationVisitor(
				subDelegates.toArray(new AnnotationVisitor[0]));
	}

	@Override
	public void visitAttribute(Attribute attr) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitAttribute(attr);
	}

	@Override
	public void visitCode() {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitCode();
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack,
			Object[] stack) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitFrame(type, nLocal, local, nStack, stack);
	}

	@Override
	public void visitInsn(int opcode) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitInsn(opcode);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitIntInsn(opcode, operand);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitVarInsn(opcode, var);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitFieldInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name,
			String desc) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitMethodInsn(opcode, owner, name, desc);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
			Object... bsmArgs) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitLabel(Label label) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitLabel(label);
	}

	@Override
	public void visitLdcInsn(Object cst) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitLdcInsn(cst);
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitIincInsn(var, increment);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt,
			Label... labels) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitMultiANewArrayInsn(desc, dims);
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler,
			String type) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitTryCatchBlock(start, end, handler, type);
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature,
			Label start, Label end, int index) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitLocalVariable(name, desc, signature, start, end, index);
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitLineNumber(line, start);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitMaxs(maxStack, maxLocals);
	}

	@Override
	public void visitEnd() {
		if (delegates == null)
			return;
		for (MethodVisitor visitor : delegates)
			visitor.visitEnd();
	}

}
