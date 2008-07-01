package org.caesarj.runtime.mixer;

import java.util.Vector;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * This Attribute subclass represents mixer config attributes and handles their
 * serialisation from and to class files. A mixer config attribute stores the
 * linearised list of superclasses of a class.
 * 
 * <p>
 * Each instances of this class play exactly one of two roles:
 * <ol>
 * <li>it represents a mixer config attribute in a class</li>
 * <li>it enables the ASM framework to read and write mixer config attributes</li>
 * </ol>
 * 
 * Instances of the latter kind are called factory instances in this file. This
 * design is explicitly specified by the ASM framework.
 * 
 * @author Tillmann Rendel
 */
public class MixerConfigAttribute extends Attribute {
	// ====== real instances ====== 
	
	/**
	 * The list of superclasses. This attribute is <tt>null</tt> if this is an
	 * factory instance.
	 */		
	public final Vector<String> classes;

	/**
	 * Creates a new real instance.
	 * 
	 * @param classes the list of classes to represent.
	 */
	public MixerConfigAttribute(Vector<String> classes) {
		super("de.tud.caesarj.mixerconfig");
		this.classes = classes;
	}

	/**
	 * Since we provided an implementation of this attribute, it is no longer
	 * unknown.
	 */
	@Override
	public boolean isUnknown() {
		// TODO is this the Right Thing (tm)? -- Tillmann
		return false;
	}

    /**
	 * Returns the byte array form of this attribute.
	 * 
	 * <p>
	 * This implementation writes the list of superclasses to the class file.
	 * 
	 * @param cw
	 *            the class to which this attribute must be added. This
	 *            parameter can be used to add to the constant pool of this
	 *            class the items that corresponds to this attribute.
	 * @param code
	 *            unused for non-code attributes
	 * @param len
	 *            unused for non-code attributes
	 * @param maxStackunused
	 *            for non-code attributes
	 * @param maxLocals
	 *            unused for non-code attributes
	 * @return the byte array form of this attribute.
	 */
	@Override
	protected ByteVector write(ClassWriter cw, byte[] code, int len, int maxStack, int maxLocals) {
		ByteVector result = new ByteVector();

		result.putShort(classes.size());

		for (String name : classes)
			result.putShort(cw.newClass(name));

		return result;
	}
	
	// ====== factory instances ====== 

	/**
	 * Creates a factory instance.
	 *
	 */
	public MixerConfigAttribute() {
		super("de.tud.caesarj.mixerconfig");
		classes = null;
	}

    /**
	 * Reads a {@link #type type} attribute. This method must return a <i>new</i>
	 * {@link Attribute} object, of type {@link #type type}, corresponding to
	 * the <tt>len</tt> bytes starting at the given offset, in the given class
	 * reader.
	 * 
	 * <p>
	 * This implementation reads the list of superclasses from the classfile and
	 * returns a new mixer config attribute representing it.
	 * 
	 * @param cr
	 *            the class that contains the attribute to be read.
	 * @param off
	 *            index of the first byte of the attribute's content in {@link
	 *            ClassReader#b cr.b}. The 6 attribute header bytes, containing
	 *            the type and the length of the attribute, are not taken into
	 *            account here.
	 * @param len
	 *            the length of the attribute's content.
	 * @param buf
	 *            buffer to be used to call
	 *            {@link ClassReader#readUTF8 readUTF8},
	 *            {@link ClassReader#readClass(int,char[]) readClass} or
	 *            {@link ClassReader#readConst readConst}.
	 * @param codeOff
	 *            not used for non-code attributes
	 * @param labels
	 *            not used for non-code attributes
	 * @return a <i>new</i> {@link MixerConfigAttribute} real instance
	 */
	@Override
	protected Attribute read(ClassReader cr, int off, int len, char[] buf, int codeOff, Label[] labels) {
		// TODO error handling?
		int count = cr.readShort(off);
		off += 2;
		Vector<String> classes = new Vector<String>(count);

		for (int i = 0; i < count; i++) {
			classes.add(cr.readClass(off, buf));
			off += 2;
		}

		return new MixerConfigAttribute(classes);
	}

}
