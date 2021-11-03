package me.brook.tasgenetics.tools;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.Serializable;

public class Vector2 implements Serializable {

	private static final long serialVersionUID = -5346411102465369657L;

	public double x, y, z;

	public Vector2() {
	}

	public Vector2(Point2D point) {
		this(point.getX(), point.getY());
	}

	public Vector2(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double distanceToSq(Vector2 loc) {
		return Math.sqrt(Math.pow(this.x - loc.x, 2) + Math.pow(this.y - loc.y, 2) + Math.pow(this.z - loc.z, 2));
	}

	public double distanceToRaw(Vector2 loc) {
		return (Math.pow(this.x - loc.x, 2) + Math.pow(this.y - loc.y, 2) + Math.pow(this.z - loc.z, 2));
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public Vector2 add(Vector2 loc) {
		return new Vector2(this.x + loc.x, this.y + loc.y);
	}

	public Vector2 add(double x, double y) {
		return new Vector2(this.x + x, this.y + y);
	}

	public Vector2 subtract(Vector2 loc) {
		return add(loc.multiply(-1));
	}

	public Vector2 multiply(double multiple) {
		return new Vector2(this.x * multiple, this.y * multiple);
	}

	public Vector2 multiply(Vector2 vector) {
		return new Vector2(this.x * vector.x, this.y * vector.y);
	}

	public Vector2 divide(double d) {
		return multiply(1 / d);
	}

	public Vector2 divide(Vector2 vector) {
		return new Vector2(this.x / vector.x, this.y / vector.y);
	}

	public Vector2 normalize() {
		double highest = x;

		if(y > highest) {
			highest = y;
		}

		if(z > highest) {
			highest = z;
		}

		if(highest == 0) {
			return this;
		}

		return divide(highest);
	}

	@Override
	public String toString() {
		return "Vector2[x=" + x + ", y=" + y + "]";
	}

	public Vector2 copy() {
		return new Vector2(x, y);
	}

	public Point toPoint() {
		return new Point((int) x, (int) y);
	}

	public double manhattanDistance(Vector2 loc) {
		return Math.abs(this.x - loc.x) + Math.abs(this.y - loc.y);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Vector2 other = (Vector2) obj;
		if(Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if(Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		if(Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
			return false;
		return true;
	}

	public boolean equals(double x, double y) {
		return this.x == x && this.y == y;
	}

}