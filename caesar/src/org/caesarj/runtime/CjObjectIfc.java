package org.caesarj.runtime;

public interface CjObjectIfc {
	public CjObject $cj$init();
	
	public CjObjectIfc cjFamily();
	
	public RuntimeCaesarClass getRuntimeClass();
}
