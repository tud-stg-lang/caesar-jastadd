package org.caesarj.runtime.mixer;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class ListDelegationAnnotationVisitor extends AnnotationVisitor {

	private final AnnotationVisitor[] delegates;

	/**
	 * @param delegates
	 *            delegate visitors to which to forward method calls<br>
	 *            May be null.
	 */
	public ListDelegationAnnotationVisitor(AnnotationVisitor[] delegates) {
		super(Opcodes.ASM4);
		this.delegates = delegates;
	}

	@Override
	public void visit(String name, Object value) {
		if (delegates != null)
			for (AnnotationVisitor visitor : delegates)
				visitor.visit(name, value);
		super.visit(name, value);
	}

	@Override
	public void visitEnum(String name, String desc, String value) {
		if (delegates == null)
			return;
		for (AnnotationVisitor visitor : delegates)
			visitor.visitEnum(name, desc, value);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String desc) {
		if (delegates == null)
			return null;
		List<AnnotationVisitor> subDelegates = new ArrayList<AnnotationVisitor>();
		for (AnnotationVisitor visitor : delegates) {
			AnnotationVisitor subDelegate = visitor.visitAnnotation(name, desc);
			if (subDelegate != null)
				subDelegates.add(subDelegate);
		}
		return new ListDelegationAnnotationVisitor(
				subDelegates.toArray(new AnnotationVisitor[0]));
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		if (delegates == null)
			return null;
		List<AnnotationVisitor> subDelegates = new ArrayList<AnnotationVisitor>();
		for (AnnotationVisitor visitor : delegates) {
			AnnotationVisitor subDelegate = visitor.visitArray(name);
			if (subDelegate != null)
				subDelegates.add(subDelegate);
		}
		return new ListDelegationAnnotationVisitor(
				subDelegates.toArray(new AnnotationVisitor[0]));
	}

	@Override
	public void visitEnd() {
		if (delegates == null)
			return;
		for (AnnotationVisitor visitor : delegates)
			visitor.visitEnd();
	}

}
