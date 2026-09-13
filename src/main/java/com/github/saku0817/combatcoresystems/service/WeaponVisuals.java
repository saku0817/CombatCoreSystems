package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.WeaponOptions;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

final class WeaponVisuals {
    private WeaponVisuals() {}
    static void play(LivingEntity target, WeaponOptions.Visual visual) {
        var location = target.getLocation().add(0, target.getHeight() * .5, 0);
        if (!visual.particle().isBlank() && visual.count() > 0)
            target.getWorld().spawnParticle(Particle.valueOf(visual.particle()), location, visual.count(), visual.spread(), visual.spread(), visual.spread(), 0);
        if (!visual.sound().isBlank()) target.getWorld().playSound(location, visual.sound(), visual.volume(), visual.pitch());
    }
}
