package com.github.saku0817.combatcoresystems.model.trigger;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/** Segment against the target's expanded box: continuous hit detection, not endpoint-only checks. */
public final class MovementPath {
    private MovementPath() {}
    public static boolean intersects(Vector from,Vector to,BoundingBox target,double width,double height) {
        double[] a={from.getX(),from.getY(),from.getZ()},b={to.getX(),to.getY(),to.getZ()};
        double[] low={target.getMinX()-width/2,target.getMinY()-height,target.getMinZ()-width/2};
        double[] high={target.getMaxX()+width/2,target.getMaxY(),target.getMaxZ()+width/2};
        double first=0,last=1;
        for(int axis=0;axis<3;axis++) {
            double delta=b[axis]-a[axis];
            if(Math.abs(delta)<1e-10) {if(a[axis]<low[axis]||a[axis]>high[axis])return false;continue;}
            double entry=(low[axis]-a[axis])/delta,exit=(high[axis]-a[axis])/delta;
            if(entry>exit){double temp=entry;entry=exit;exit=temp;}
            first=Math.max(first,entry);last=Math.min(last,exit);if(first>last)return false;
        }
        return true;
    }
}
