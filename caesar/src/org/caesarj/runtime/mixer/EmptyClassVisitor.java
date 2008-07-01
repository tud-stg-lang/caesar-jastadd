package org.caesarj.runtime.mixer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

// A class visitor that consumes input and does nothing
// Intended to be overridden by visitors that are interested only in particular data

public class EmptyClassVisitor implements ClassVisitor {
   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) { }
   public void visitSource(String source, String debug) { }
   public void visitOuterClass(String owner, String name, String desc) { }
   public AnnotationVisitor visitAnnotation(String desc, boolean visible) { return null; }
   public void visitAttribute(Attribute attr) {  }
   public void visitInnerClass(String name, String outerName, String innerName, int access) { }
   public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) { return null; }
   public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) { return null; }
   public void visitEnd() { }
}