package droidar.light.gl;

/**
 * The values for the color channels (eg {@link Color#red} have to be between 0
 * and 1!
 * 
 * @author Spobo
 * 
 */
public class Color {

	/**
	 * has to be between 0 and 1
	 */
	public float red;

	/**
	 * see {@link Color#red}
	 */
	public float green;
	/**
	 * see {@link Color#red}
	 */
	public float blue;
	/**
	 * see {@link Color#red}
	 */
	public float alpha;

	/**
	 * the values should be between 0 and 1, for example new Color(1,0,0,1)
	 * would be red
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 */
	public Color(float red, float green, float blue, float alpha) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.alpha = alpha;
	}

	/**
	 * transforms the color object into its appendant integer value
	 * 
	 * blue will become 0xff0000ff (argb value) for example
	 * 
	 * @return
	 */
	public int toIntARGB() {
		// android.graphics.Color.parseColor("#66000000"); //would be
		// transparent black TODO use somehow
		return android.graphics.Color.argb((int) (alpha * 255),
				(int) (red * 255), (int) (green * 255), (int) (blue * 255));
	}

	public static Color red() {
		return new Color(1f, 0f, 0f, 1f);
	}

	public static Color redTransparent() {
		return new Color(1f, 0f, 0f, 0.5f);
	}

	public static Color blue() {
		return new Color(0f, 0f, 1f, 1f);
	}

	public Color copy() {
		return new Color(red, green, blue, alpha);
	}

	@Override
	public String toString() {
		return "(r:" + this.red + ",g:" + this.green + ",b:" + this.blue
				+ ",a:" + this.alpha + ")";
	}

	public void setTo(Color c) {
		alpha = c.alpha;
		green = c.green;
		blue = c.blue;
		red = c.red;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Color ? this.toIntARGB() == ((Color) other)
				.toIntARGB() : false;
	}

	@Override
	public int hashCode() {
		return this.toIntARGB();
	}

}
