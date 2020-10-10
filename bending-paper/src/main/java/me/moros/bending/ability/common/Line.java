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

package me.moros.bending.ability.common;

import me.moros.bending.game.Game;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;

import java.util.Collections;
import java.util.Optional;

public abstract class Line implements Updatable {
	protected final User user;

	protected Vector3 location;
	protected Vector3 targetLocation;
	protected Vector3 direction;
	protected Vector3 origin;
	protected Collider collider;
	protected LivingEntity target;

	protected final double range;
	protected final double speed;

	protected boolean locked = false;
	protected boolean controllable = false;

	public Line(User user, Block source, double range, double speed, boolean followTarget) {
		this.user = user;
		this.location = new Vector3(source.getLocation().add(0.5, 1.25, 0.5));
		this.origin = location;
		this.range = range;
		this.speed = speed;
		Optional<LivingEntity> entity = WorldMethods.getTargetEntity(user, range);
		if (followTarget && entity.isPresent()) {
			target = entity.get();
			locked = true;
		}
		targetLocation = new Vector3(entity.map(Entity::getLocation).orElseGet(() ->
			WorldMethods.getTarget(user.getWorld(), user.getRay(range), Collections.singleton(Material.WATER)))
		);
		direction = targetLocation.subtract(location).setY(0).normalize();

		collider = new Sphere(location, 1);
	}

	@Override
	public UpdateResult update() {
		if (locked) {
			if (!isValidTarget() || targetLocation.distanceSq(new Vector3(target.getLocation())) > 5 * 5) {
				locked = false;
			} else {
				targetLocation = new Vector3(target.getLocation());
				direction = targetLocation.subtract(location).setY(0).normalize();
			}
		}

		if (onBlockHit(location.toBlock(user.getWorld()).getRelative(BlockFace.DOWN))) {
			return UpdateResult.REMOVE;
		}

		render();
		postRender();

		location = location.add(direction.scalarMultiply(speed));
		Block baseBlock = location.toBlock(user.getWorld()).getRelative(BlockFace.DOWN);

		int y1 = NumberConversions.floor(targetLocation.getY());
		int y2 = NumberConversions.floor(location.getY());
		if (!isValidBlock(baseBlock)) {
			if (isValidBlock(baseBlock.getRelative(BlockFace.UP))) {
				location = location.add(Vector3.PLUS_J);
			} else if (isValidBlock(baseBlock.getRelative(BlockFace.DOWN))) {
				location = location.add(Vector3.MINUS_J);
			} else {
				return UpdateResult.REMOVE;
			}
		} else if (y1 != y2) { // Advance location vertically if possible to match target height
			if (y1 > y2 && isValidBlock(baseBlock.getRelative(BlockFace.UP))) {
				location = location.add(Vector3.PLUS_J);
			} else if (y1 < y2 && isValidBlock(baseBlock.getRelative(BlockFace.DOWN))) {
				location = location.add(Vector3.MINUS_J);
			}
		}

		if (location.distanceSq(origin) > range * range) {
			return UpdateResult.REMOVE;
		}
		if (!Game.getProtectionSystem().canBuild(user, location.toBlock(user.getWorld()))) {
			return UpdateResult.REMOVE;
		}
		collider = new Sphere(location, 1);

		boolean hit = CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, true);
		return hit ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	public abstract void render();

	public void postRender() {
	}

	public abstract boolean onEntityHit(Entity entity);

	public abstract boolean onBlockHit(Block block);

	public Collider getCollider() {
		return collider;
	}

	private boolean isValidBlock(Block block) {
		if (!MaterialUtil.isTransparent(block.getRelative(BlockFace.UP))) return false;
		return MaterialUtil.isWater(block) || MaterialUtil.isIce(block) || !block.isPassable();
	}

	private boolean isValidTarget() {
		if (target == null || !target.isValid()) return false;
		if (target instanceof Player && !((Player) target).isOnline()) return false;
		return target.getWorld().equals(user.getWorld());
	}
}
