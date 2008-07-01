package org.caesarj.runtime.mixer;

public class MixinName {
	private String out;

	private String parent;

	private String simple;
	
	private String basename;

	public MixinName(String simple, String out, String parent, String basename) {
		this.out = out;
		this.parent = parent;
		this.simple = simple;
		this.basename = basename;
	}

	public MixinName(String name) {
		super();

		int dollar = -1;
		int lbracket = name.length();
		int rbracket = name.length();
		int at = name.length();
		int level = 0;
		
		StringBuilder basename = new StringBuilder();

		// search the position of the last $, and the first @ following it.
		// ignore $s and @s inside brackets.
		for (int i = 0; i < name.length(); i++)
			switch (name.charAt(i)) {
			case '$':
				if (level == 0) {
					dollar = i;
					lbracket = name.length();
					rbracket = name.length();
					at = name.length();
					basename.append('$');
				}
				break;

			case '@':
				if (level == 0 && at > i)
					at = i;
				break;

			case '[':
				if (level == 0 && lbracket > i)
					lbracket = i;
				level++;
				break;

			case ']':
				level--;
				if (level == 0 && rbracket > i)
					rbracket = i;
				break;
				
			default:
				if (level == 0)
					basename.append(name.charAt(i));
			}

		this.out = dollar > 0 ? name.substring(0, dollar) : null;
		this.simple = name.substring(dollar + 1, at);
		this.parent = rbracket > lbracket ? name.substring(lbracket + 1, rbracket) : null;
		this.basename = basename.toString();
	}

	public String getOut() {
		return out;
	}

	public String getParent() {
		return parent;
	}

	public String getSimpleName() {
		return simple;
	}

	public String toString() {
		StringBuilder result = new StringBuilder();

		if (out != null)
			result.append(out).append('$');
		result.append(simple);
		if (parent != null)
			result.append("@[").append(parent).append(']');

		return result.toString();
	}
	
	public static void main(String[] args) {
		MixinName x = new MixinName("xxx@[a$b@[c$d]]$e");
		System.out.println(x);
	}

	public String getBasename() {
		return basename;
	}

	public static boolean isMixin(String name) {
		return name != null && name.indexOf('@') >= 0;
	}
	
	public static String getSimpleName(String name) {
		return new MixinName(name).getSimpleName();
	}
}
