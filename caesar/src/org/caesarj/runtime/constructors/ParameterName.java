package org.caesarj.runtime.constructors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Saves the name of a constructor parameter at runtime.
 * 
 * @author Marko Martin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ParameterName {

	String value();

}
