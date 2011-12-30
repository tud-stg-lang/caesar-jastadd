package org.caesarj.runtime.mixer;

import org.caesarj.runtime.constructors.CompositeParameterPattern;
import org.caesarj.runtime.constructors.ConcreteParameter;
import org.caesarj.runtime.constructors.ParameterListPattern;
import org.caesarj.runtime.constructors.ParameterPattern;
import org.caesarj.runtime.constructors.ParameterPatternList;
import org.caesarj.runtime.constructors.ParameterPatternVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * This attribute is used with constructor methods in order to provide
 * information on the constructor call inside this method. This is necessary
 * because the patterns we use for constructor calls cannot be encoded in the
 * default code of the method.
 * 
 * @author Marko Martin
 */
public class ConstructorCallAttribute extends Attribute {

	public static final String ATTRIBUTE_NAME = "de.tud.caesarj.constructorcall";

	public static final byte PARAMETER_PATTERN_CONCRETE = 0;

	public static final byte PARAMETER_PATTERN_LIST = 1;

	public static final byte PARAMETER_LIST_PATTERN = 2;

	private static class ParameterPatternReader implements
			ParameterPatternVisitor {

		private final ClassReader classReader;

		private int offset;

		private final char[] buf;

		public ParameterPatternReader(ClassReader classReader, int offset,
				char[] buf) {
			this.classReader = classReader;
			this.offset = offset;
			this.buf = buf;
		}

		@Override
		public void visit(ConcreteParameter parameter) {
			parameter.setName(classReader.readUTF8(offset, buf));
			offset += 2;
			parameter.setTypeName(classReader.readUTF8(offset, buf));
			offset += 2;
		}

		@Override
		public void visit(CompositeParameterPattern compositePattern) {
		}

		@Override
		public void visit(ParameterPatternList list) {
			int count = classReader.readByte(offset++);
			list.getComponents().clear();
			for (int i = 0; i < count; i++)
				list.getComponents().add(readPattern());
		}

		@Override
		public void visit(ParameterListPattern pattern) {
			pattern.setReferencedListIndex(classReader.readByte(offset++));
		}

		public ParameterPattern readPattern() {
			final ParameterPattern result;
			int type = classReader.readByte(offset++);
			switch (type) {
			case PARAMETER_PATTERN_CONCRETE:
				result = new ConcreteParameter();
				break;
			case PARAMETER_PATTERN_LIST:
				result = new ParameterPatternList();
				break;
			case PARAMETER_LIST_PATTERN:
				result = new ParameterListPattern();
				break;
			default:
				throw new IllegalArgumentException(String.format(
						"Parameter pattern type %d is unknown.", type));
			}
			result.accept(this);
			return result;
		}

		/**
		 * @return the offset where to read the next data from the class reader
		 */
		public int getOffset() {
			return offset;
		}

	}

	private final ParameterPattern pattern;

	private final ParameterPattern calledPattern;

	private final boolean isSuperConstructorCall;

	/**
	 * Constructor for instances which should represent the attribute
	 * 
	 * @param pattern
	 *            the parameter pattern of the calling constructor
	 * @param calledPattern
	 *            the pattern which should be written
	 * @param isSuperConstructorCall
	 *            true indicates that this call refers a constructor of the
	 *            super class (and not of "this")
	 */
	public ConstructorCallAttribute(ParameterPattern pattern,
			ParameterPattern calledPattern, boolean isSuperConstructorCall) {
		super(ATTRIBUTE_NAME);
		this.pattern = pattern;
		this.calledPattern = calledPattern;
		this.isSuperConstructorCall = isSuperConstructorCall;
	}

	/**
	 * Constructor for instances which should read the attribute
	 */
	public ConstructorCallAttribute() {
		this(null, null, true);
	}

	public ParameterPattern getPattern() {
		return pattern;
	}

	public ParameterPattern getCalledPattern() {
		return calledPattern;
	}

	public boolean isSuperConstructorCall() {
		return isSuperConstructorCall;
	}

	@Override
	public boolean isUnknown() {
		return false;
	}

	@Override
	protected Attribute read(ClassReader cr, int off, int len, char[] buf,
			int codeOff, Label[] labels) {
		final ParameterPatternReader patternReader = new ParameterPatternReader(
				cr, off, buf);
		return new ConstructorCallAttribute(patternReader.readPattern(),
				patternReader.readPattern(), cr.readByte(patternReader
						.getOffset()) != 0);
	}

	@Override
	protected ByteVector write(ClassWriter cw, byte[] code, int len,
			int maxStack, int maxLocals) {
		throw new UnsupportedOperationException(
				"Writing ConstructorCallAttributes is currently not supported.");
	}

}
