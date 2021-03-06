package org.caesarj.runtime;

public class CjObject implements CjObjectIfc {
	protected CjObjectIfc $cj$family;
	
	public CjObject(CjObjectIfc family) {
		$cj$family = family; 
	}
	
	public CjObject $cj$init() {
		/* just to be available for super calls */
		return this;
	}
	
	public CjObjectIfc cjFamily() {
		return $cj$family;
	}
	
	public RuntimeCaesarClass getRuntimeClass() {
		try {
			return (RuntimeCaesarClass)RuntimeClass.forClass(getClass());
		}
		catch (RuntimeTypeException e) {
			throw new RuntimeInconsistencyError(e.getMessage());
		}
		catch (ClassCastException e) {
			throw new RuntimeInconsistencyError(e.getMessage());
		}
	}
}
