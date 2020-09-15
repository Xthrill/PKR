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

import org.bukkit.Location;
import org.bukkit.Sound;

import java.util.Objects;

public class SoundEffect {
	private final Sound sound;
	private final float volume;
	private final float pitch;

	public SoundEffect(Sound sound) {
		this(sound, 1, 1);
	}

	public SoundEffect(Sound sound, float volume, float pitch) {
		this.sound = Objects.requireNonNull(sound);
		this.volume = volume;
		this.pitch = pitch;
	}

	public Sound getSound() {
		return sound;
	}

	public float getVolume() {
		return volume;
	}

	public float getPitch() {
		return pitch;
	}

	public void play(Location center) {
		SoundUtil.playSound(center, sound, volume, pitch);
	}
}
