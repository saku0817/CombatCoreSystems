package com.github.saku0817.combatcoresystems.model;

import com.github.saku0817.combatcoresystems.model.trigger.MovementPath;
import com.github.saku0817.combatcoresystems.service.MovementService;
import java.util.Map;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementPathTest {
    @Test void validatesMovementSafetyLimits() {
        assertDoesNotThrow(()->MovementService.validate(Map.of("type","DASH","distance",5,"duration",.25)));
        assertThrows(IllegalArgumentException.class,()->MovementService.validate(Map.of("type","DASH","distance",64,"duration",0)));
        assertThrows(IllegalArgumentException.class,()->MovementService.validate(Map.of("type","DASH","collision","IGNORE")));
        assertThrows(IllegalArgumentException.class,()->MovementService.validate(Map.of("type","DASH","hit-policy","UNKNOWN")));
        assertThrows(IllegalArgumentException.class,()->MovementService.validate(Map.of("type","DASH","path-area",Map.of("shape","SPHERE"))));
    }
    private final BoundingBox enemy = new BoundingBox(2, 0, -.3, 2.6, 1.8, .3);

    @Test void detectsEnemyBetweenEndpoints() {
        assertTrue(MovementPath.intersects(new Vector(0,0,0),new Vector(5,0,0),enemy,1,2));
        assertTrue(MovementPath.intersects(new Vector(5,0,0),new Vector(0,0,0),enemy,1,2));
    }

    @Test void rejectsParallelPathOutsideWidth() {
        assertFalse(MovementPath.intersects(new Vector(0,0,2),new Vector(5,0,2),enemy,1,2));
    }

    @Test void respectsVerticalReachAndStationaryOverlap() {
        assertFalse(MovementPath.intersects(new Vector(0,3,0),new Vector(5,3,0),enemy,1,2));
        assertTrue(MovementPath.intersects(new Vector(2,0,0),new Vector(2,0,0),enemy,1,2));
        assertFalse(MovementPath.intersects(new Vector(0,0,0),new Vector(0,0,0),enemy,1,2));
    }
}
