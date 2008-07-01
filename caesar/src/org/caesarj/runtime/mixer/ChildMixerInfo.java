package org.caesarj.runtime.mixer;

import java.util.HashMap;
import java.util.Map;

public class ChildMixerInfo {
	/**
	 * The mapping of original to new class names 
	 */
	public final Map<String, String> mapping = new HashMap<String, String>();

	/**
	 * The name of the new super class to replace "$$super". The value of this
	 * field is lazily created in the <code>update</code> method when
	 * "$$super" is encountered as the superclass.
	 * 
	 * <p>
	 * If "$$super" isn't used as superclass name, this field remains
	 * <code>null</code>.
	 */
	public String newSuper;
	
	/**
	 * The enclosing type of the new super class
	 * 
	 * <p>
	 * If "$$super" isn't used as superclass name, this field remains
	 * <code>null</code>.
	 */
	public String newSuperOut;
}
