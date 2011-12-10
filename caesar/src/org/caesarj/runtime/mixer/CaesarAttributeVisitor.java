package org.caesarj.runtime.mixer;

import java.util.Vector;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class CaesarAttributeVisitor extends ClassVisitor {
	
	/**
	 * The MixinRegistry managing the mixins for this application.
	 */
	private final MixinRegistry mixins;
	
	/**
	 * Information about transformation computed from the Caesar attribute.
	 * null - if the attribute is not found
	 */
	private ChildMixerInfo info = null;
	
	/**
	 * Creates a new child mixer 
	 * relying upon the provided mixin registry for mixin management.
	 * 
	 * @param cv
	 *            class visitor to forward events to
	 * @param mixins
	 *            mixin registry managing the mixins for this application
	 */
	public CaesarAttributeVisitor(MixinRegistry mixins) {
		super(Opcodes.ASM4);
		this.mixins = mixins;
	}
	
	public ChildMixerInfo getInfo() {
		return info;
	}
	
	/**
	 * Visits a non standard attribute of the class.
	 * 
	 * <p>
	 * This handler processes the information stored in the mixer config
	 * attribute. other attributes are ignored.
	 * 
	 * <p>
	 * The information stored in the mixer config attribute is fed to the mixin
	 * registry, and the newly created mixin names are used to set up the
	 * mappings of original to new class names.
	 * 
	 * @param attr
	 *            an attribute.
	 */
	public void visitAttribute(Attribute attr) {  
		if (attr instanceof MixerConfigAttribute) {
			info = new ChildMixerInfo();
			
			MixerConfigAttribute config = (MixerConfigAttribute) attr;
			Vector<String> classes = config.classes;

			if (!classes.isEmpty()) {
				String next = classes.lastElement();
				String nextOut = MixinInformation.getOutName(next);
				for (int i = classes.size() - 2; i > 0; i--) {
					String base = classes.get(i);
					String baseOut = MixinInformation.getOutName(base);
					next = mixins.newMixin(new MixinInformation(base, next, baseOut, nextOut));
					nextOut = baseOut;							
					info.mapping.put(base, next);
				}
				
				String first = classes.get(0);
				String firstOut = MixinInformation.getOutName(first);
				
				info.newSuper = mixins.newMixin(new MixinInformation(first, next, firstOut, nextOut));
				info.mapping.put(first, info.newSuper);
				info.newSuperOut = firstOut;
			}
		}
	}
}
