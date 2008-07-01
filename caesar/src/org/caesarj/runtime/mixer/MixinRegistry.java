package org.caesarj.runtime.mixer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MixinRegistry {

	/**
	 * Source of numeric ids
	 */
	private final List<MixinInformation> mixins = new ArrayList<MixinInformation>();
	private final Map<MixinInformation, Integer> infoToId = new HashMap<MixinInformation, Integer>();
	private final Map<Integer, Class<?>> idToClass = new HashMap<Integer, Class<?>>(); 
		
	public int newMixin() {
		int id = mixins.size();
		mixins.add(null);		
		return id;
	}
	
	public String newMixin(MixinInformation mixinInfo) {
		int id;
		// do we already have the necessary mixin copy?
		if (infoToId.containsKey(mixinInfo)) {
			// if yes, then reuse it
			id = infoToId.get(mixinInfo);
		}
		else {
			// otherwise create a new copy
			id = newMixin();
			mixins.set(id, mixinInfo);
			infoToId.put(mixinInfo, id);
		}
		return toString(id, mixinInfo.getBaseClass());
	}
	
	public String getBaseClass(int id) {
		return mixins.get(id).getBaseClass();
	}
	
	public String getSuperClass(int id) {
		return mixins.get(id).getSuperClass();
	}
	
	public String getOutClass(int id) {
		return mixins.get(id).getOutClass();
	}
	
	public String getSuperOut(int id) {
		return mixins.get(id).getSuperOut();		
	}
		
	public void cache(int id, Class<?> mixin) {
		idToClass.put(id, mixin);
	}
	
	public Class<?> fetch(int id) {
		return idToClass.get(id);
	}
	
	public boolean isCached(int id) {
		return idToClass.containsKey(mixins.get(id));
	}
	
	public static String toString(int id, String baseName) {
		return baseName + "$$" + id;
	}
}
