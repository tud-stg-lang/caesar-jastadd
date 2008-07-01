package org.caesarj.runtime.mixer;

/**
 * Information about a mixin copy. This information is sufficient to construct
 * the mixin class by copying and transforming the mentioned classes.
 * 
 * @author Tillmann Rendel
 */
public class MixinInformation {
	/**
	 * the base class to be copied and transformed to form this mixin.
	 */
	private String baseClass;

	/**
	 * the new super class of {@link #baseClass}.
	 */
	private String superClass;
	
	/**
	 * the new outer class of {@link #baseClass}. this field is currently
	 * unused since inner classes are not yet implemented.
	 */
	private String outClass = null;

	/**
	 * the new outer class of the new super class of {@link #baseClass}. this
	 * field is currently unused since inner classes are not yet implemented.
	 */
	private String superOut = null;
	
	/**
	 * Creates new mixin information with the given data.
	 * 
	 * @param baseClass the base class to be copied and transformed
	 * @param superClass the new super class ofbaseClass
	 */
	public MixinInformation(String baseClass, String superClass, String outClass, String outSuper) {
		super();
		
		this.baseClass = baseClass;
		this.superClass = superClass;
		this.outClass = outClass;
		this.superOut = outSuper;
	}
	
	public static String getOutName(String className) {
		int sepPos = className.lastIndexOf('$');
		if (sepPos == -1)
			return null;
		return className.substring(0, sepPos);		
	}
	
	/**
	 * Returns the base class to be copied and transformed.
	 * 
	 * @return base class to be copied and transformed
	 */
	public String getBaseClass() {
		return baseClass;
	}
	
	/** 
	 * Returns the new outer class of {@link #baseClass}.
	 *  
	 * @return new outer class of baseClass.
	 */
	public String getOutClass() {
		return outClass;
	}
	
	/** 
	 * Returns the new super class of {@link #baseClass}.
	 *  
	 * @return new super class of baseClass.
	 */
	public String getSuperClass() {
		return superClass;
	}
	
	/**
	 * Returns the new outer class of the new super class of {@link #baseClass}.
	 * 
	 * @return new outer class of the new super class of baseClass
	 */
	public String getSuperOut() {
		return superOut;
	}

	/**
	 * Helper method wich allows <tt>null</tt>-sensitive comparisions of
	 * objects.
	 * 
	 * @param one
	 *            first object to compare
	 * @param other
	 *            second object to compare
	 * 
	 * @return <tt>true</tt> if both parameters are <tt>null</tt>, or if
	 *         they are both not <tt>null</tt> and equal.
	 */
	private static boolean equals(Object one, Object other) {
		// TODO should this be in a library of helpful functions?
		return ((one == other) || ((one != null) && (other != null) && one.equals(other))); 
	}
	
	/**
	 * Indicates whether some other object is "equal to" this one.
	 * 
	 * @param obj
	 *            the object with which to compare
	 * @return <tt>true</tt> if both mixin informations denote the same mixin
	 */
 	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MixinInformation) {
			MixinInformation that = (MixinInformation) obj;
			return equals(this.baseClass, that.baseClass) && equals(this.superClass, that.superClass);
		} 
		else { 
			return false;
		}
	}
	
	/**
	 * Helper method wich allows <tt>null</tt>-sensitive computation of hash
	 * codes. the hash code of <tt>null</tt> is 0.
	 * 
	 * @param o
	 *            the object to compute a hash code from.
	 * 
	 * @return hash code
	 */
	private static int hash(Object o) {
		// TODO should this be in a library of helpful functions?
		// TODO is 0 a good choice for hash(null)?
		if (o == null)
			return 0;
		else
			return o.hashCode();
	}
	/**
	 * Returns a hash code.
	 * @return hash code
	 */
	@Override
	public int hashCode() {
		return hash(baseClass) + hash(superClass);
	}
}
