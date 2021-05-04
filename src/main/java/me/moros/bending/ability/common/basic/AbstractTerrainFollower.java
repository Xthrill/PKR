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

package me.moros.bending.ability.common.basic;

import java.util.function.Predicate;

import me.moros.bending.model.math.Vector3;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractTerrainFollower {
  protected Predicate<Block> diagonalsPredicate = MaterialUtil::isTransparent;

  protected AbstractTerrainFollower() {
  }

  protected @Nullable Vector3 resolveMovement(@NonNull World world, @NonNull Vector3 origin, @NonNull Vector3 direction) {
    Block original = origin.toBlock(world);
    Block destination = origin.add(direction).toBlock(world);
    int offset = 0;
    if (!isValidBlock(destination)) {
      if (isValidBlock(destination.getRelative(BlockFace.UP)) && diagonalsPredicate.test(original.getRelative(BlockFace.UP))) {
        offset = 1;
      } else if (isValidBlock(destination.getRelative(BlockFace.DOWN)) && diagonalsPredicate.test(destination)) {
        offset = -1;
      } else {
        return null;
      }
    }

    int diagonalCollisions = 0;
    for (Vector3 v : VectorMethods.decomposeDiagonals(origin, direction)) {
      int x = NumberConversions.floor(v.getX());
      int y = NumberConversions.floor(v.getY()) + offset;
      int z = NumberConversions.floor(v.getZ());
      Block block = original.getRelative(x, y, z);
      if (!isValidBlock(block)) {
        if (++diagonalCollisions > 1) {
          return null;
        }
      }
    }

    return origin.add(direction).add(new Vector3(0, offset, 0));
  }

  protected abstract boolean isValidBlock(@NonNull Block block);
}