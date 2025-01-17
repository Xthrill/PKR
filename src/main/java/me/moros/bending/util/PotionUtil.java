/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util;

import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class to handle potion effects on entities.
 */
public final class PotionUtil {
  private static final Set<PotionEffectType> POSITIVE = Set.of(
    PotionEffectType.ABSORPTION, PotionEffectType.DAMAGE_RESISTANCE, PotionEffectType.FAST_DIGGING,
    PotionEffectType.FIRE_RESISTANCE, PotionEffectType.HEAL, PotionEffectType.HEALTH_BOOST,
    PotionEffectType.INCREASE_DAMAGE, PotionEffectType.JUMP, PotionEffectType.NIGHT_VISION,
    PotionEffectType.REGENERATION, PotionEffectType.SATURATION, PotionEffectType.SPEED,
    PotionEffectType.WATER_BREATHING
  );
  private static final Set<PotionEffectType> NEUTRAL = Set.of(
    PotionEffectType.INVISIBILITY
  );
  private static final Set<PotionEffectType> NEGATIVE = Set.of(
    PotionEffectType.POISON, PotionEffectType.BLINDNESS, PotionEffectType.CONFUSION,
    PotionEffectType.HARM, PotionEffectType.HUNGER, PotionEffectType.SLOW,
    PotionEffectType.SLOW_DIGGING, PotionEffectType.WEAKNESS, PotionEffectType.WITHER
  );

  private PotionUtil() {
  }

  public static boolean isPositive(@NonNull PotionEffectType type) {
    return POSITIVE.contains(type);
  }

  public static boolean isNeutral(@NonNull PotionEffectType type) {
    return NEUTRAL.contains(type);
  }

  public static boolean isNegative(@NonNull PotionEffectType type) {
    return NEGATIVE.contains(type);
  }

  public static boolean tryAddPotion(@NonNull Entity entity, @NonNull PotionEffectType type, int duration, int amplifier) {
    if (entity.isValid() && entity instanceof LivingEntity livingEntity) {
      int minDuration = isPositive(type) ? 20 : duration;
      PotionEffect effect = livingEntity.getPotionEffect(type);
      if (effect == null || effect.getDuration() < minDuration || effect.getAmplifier() < amplifier) {
        livingEntity.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false));
        return true;
      }
    }
    return false;
  }
}
