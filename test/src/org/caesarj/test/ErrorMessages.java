package org.caesarj.test;

import java.io.IOException;
import java.util.Properties;

public class ErrorMessages {
	private static ErrorMessages instance = new ErrorMessages();

	public static ErrorMessages instance() {
		return instance;
	}

	private final Properties props = new Properties();

	private ErrorMessages() {
		try {
			props.load(ErrorMessages.class
					.getResourceAsStream("errors.properties"));
		} catch (IOException e) {
			System.err.println("Could not load error messages.");
		} catch (NullPointerException e) {
			System.err.println("Could not load error messages.");
		}
	}

	/*
	 * Die Fehlermeldungen aus den Testf�llen entsprechen dem alten Compiler.
	 * �ber die properties-Datei werden sie f�r den neuen �bersetzt. Die Strings
	 * sind dabei eine durch Leerzeichen getrennte Liste aus W�rtern, die alle
	 * in der Fehlermeldung vorkommen m�ssen. Mehrere alternative Meldungen
	 * k�nnen durch Komma getrennt werden.
	 */
	public boolean similarError(String expected, String actual) {
		String compare = props.getProperty(expected);
		if (compare == null)
			compare = expected;

		String variants[] = compare.split(",");
		for (String variant : variants) {
			if (checkVariant(actual, variant))
				return true;
		}

		// Keine Meldung passt
		return false;
	}

	private boolean checkVariant(String actual, String variant) {
		String words[] = variant.split("\\s+");
		for (String word : words) {
			if (actual.indexOf(word) == -1)
				return false;
		}
		return true;
	}
}
