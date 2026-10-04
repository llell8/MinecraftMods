package com.hackclient.render;

/** Meteor-style box drawing: just the outline, just the faces, or both. */
public enum ShapeMode {
	LINES("Lines"), SIDES("Sides"), BOTH("Both");

	private final String display;

	ShapeMode(String display) {
		this.display = display;
	}

	public boolean lines() {
		return this != SIDES;
	}

	public boolean sides() {
		return this != LINES;
	}

	@Override
	public String toString() {
		return display;
	}
}
