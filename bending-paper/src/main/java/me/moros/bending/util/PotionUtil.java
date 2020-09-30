/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import me.moros.bending.model.user.User;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class PotionUtil {
	private static final Set<PotionEffectType> POSITIVE = new HashSet<>(Arrays.asList(
		PotionEffectType.ABSORPTION, PotionEffectType.DAMAGE_RESISTANCE, PotionEffectType.FAST_DIGGING,
		PotionEffectType.FIRE_RESISTANCE, PotionEffectType.HEAL, PotionEffectType.HEALTH_BOOST,
		PotionEffectType.INCREASE_DAMAGE, PotionEffectType.JUMP, PotionEffectType.NIGHT_VISION,
		PotionEffectType.REGENERATION, PotionEffectType.SATURATION, PotionEffectType.SPEED,
		PotionEffectType.WATER_BREATHING
	));
	private static final Set<PotionEffectType> NEUTRAL = new HashSet<>(Collections.singletonList(
		PotionEffectType.INVISIBILITY
	));
	private static final Set<PotionEffectType> NEGATIVE = new HashSet<>(Arrays.asList(
		PotionEffectType.POISON, PotionEffectType.BLINDNESS, PotionEffectType.CONFUSION,
		PotionEffectType.HARM, PotionEffectType.HUNGER, PotionEffectType.SLOW,
		PotionEffectType.SLOW_DIGGING, PotionEffectType.WEAKNESS, PotionEffectType.WITHER
	));

	public static boolean isPositive(PotionEffectType type) {
		return POSITIVE.contains(type);
	}

	public static boolean isNeutral(PotionEffectType type) {
		return NEUTRAL.contains(type);
	}

	public static boolean isNegative(PotionEffectType type) {
		return NEGATIVE.contains(type);
	}

	public static boolean canAddPotion(User user, PotionEffectType type, int minDuration, int minAmplifier) {
		PotionEffect effect = user.getEntity().getPotionEffect(type);
		return effect == null || effect.getDuration() < minDuration || effect.getAmplifier() < minAmplifier;
	}
}
