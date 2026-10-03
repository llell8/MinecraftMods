package com.hackclient.setting;

public class NumberSetting extends Setting<Double> {
	private final double min, max;
	private final int decimals;

	public NumberSetting(String name, double defaultValue, double min, double max, int decimals) {
		super(name, defaultValue);
		this.min = min;
		this.max = max;
		this.decimals = decimals;
	}

	@Override
	public void set(Double value) {
		double scale = Math.pow(10, decimals);
		this.value = Math.round(Math.clamp(value, min, max) * scale) / scale;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public float getFloat() {
		return value.floatValue();
	}

	public String format() {
		return decimals == 0 ? String.valueOf(value.intValue()) : String.format("%." + decimals + "f", value);
	}
}
