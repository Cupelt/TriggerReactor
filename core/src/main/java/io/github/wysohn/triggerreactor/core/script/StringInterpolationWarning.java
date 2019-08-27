package io.github.wysohn.triggerreactor.core.script;

public class StringInterpolationWarning extends Warning {

	private int row;
	private String string;
	
	/**
	 * 
	 * @param row the row of the unescaped $
	 * @param string the containing string
	 */
	public StringInterpolationWarning(int row, String string) {
		this.row = row;
		this.string = string;
	}
	
	@Override
	public String[] getMessage() {
		return new String[] {"Unescaped $ found at line " + row + ": ",
	           "\"" + string + "\"",
			   "to ensure compatibility with the upcoming features of trg 3.0, all $ must be escaped: \\$"};
	}
}