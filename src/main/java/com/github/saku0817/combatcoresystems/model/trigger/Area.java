package com.github.saku0817.combatcoresystems.model.trigger;

/** Geometry in local coordinates: x=right, y=up, z=forward. */
public record Area(Shape shape, double radius, double width, double height, double length, double angle, Origin origin) {
    public enum Shape { CIRCLE, SPHERE, BOX, FORWARD_BOX, CONE, CYLINDER }
    public enum Origin { SELF, EVENT_TARGET, LOCATION }
    public Area {
        for (double n : new double[]{radius, width, height, length, angle})
            if (!Double.isFinite(n) || n < 0) throw new IllegalArgumentException("Invalid area dimension");
        if (angle > 360) throw new IllegalArgumentException("Area angle exceeds 360");
    }
    public boolean contains(double x, double y, double z) {
        return switch (shape) {
            case SPHERE -> x*x+y*y+z*z <= radius*radius;
            case CIRCLE, CYLINDER -> x*x+z*z <= radius*radius && Math.abs(y) <= height/2;
            case BOX -> Math.abs(x) <= width/2 && Math.abs(y) <= height/2 && Math.abs(z) <= length/2;
            case FORWARD_BOX -> Math.abs(x) <= width/2 && Math.abs(y) <= height/2 && z >= 0 && z <= length;
            case CONE -> x*x+z*z <= radius*radius && Math.abs(y) <= height/2
                    && (x*x+z*z == 0 || z / Math.sqrt(x*x+z*z) >= Math.cos(Math.toRadians(angle/2)) - 1e-12);
        };
    }
    public double searchRadius() {
        return switch(shape) {
            case SPHERE -> radius;
            case CIRCLE, CYLINDER, CONE -> Math.sqrt(radius*radius+height*height/4);
            case BOX -> Math.sqrt(width*width+height*height+length*length)/2;
            case FORWARD_BOX -> Math.sqrt(width*width/4+height*height/4+length*length);
        };
    }
}
