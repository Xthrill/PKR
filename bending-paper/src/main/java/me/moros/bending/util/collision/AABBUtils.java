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

package me.moros.bending.util.collision;

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.DummyCollider;
import me.moros.bending.model.math.Vector3;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public final class AABBUtils {
	public static final DummyCollider dummy = new DummyCollider();

	public static AABB getBlockBounds(Block block) {
		if (block.isPassable()) return dummy;
		return new AABB(new Vector3(block.getBoundingBox().getMin()), new Vector3(block.getBoundingBox().getMax()));
	}

	public static AABB getEntityBounds(Entity entity) {
		return new AABB(new Vector3(entity.getBoundingBox().getMin()), new Vector3(entity.getBoundingBox().getMax()));
	}
}
