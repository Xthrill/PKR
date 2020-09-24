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

package me.moros.bending.ability.fire.sequences;

import me.moros.bending.ability.fire.*;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class FireWave implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private final Queue<WallInfo> walls = new ArrayDeque<>();
	private FireWall wall;
	private long nextTime;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		wall = Game.getAbilityManager(user.getWorld()).getFirstInstance(user, FireWall.class).orElse(null);
		if (wall == null) return false;

		Vector3 origin = user.getEyeLocation().add(user.getDirection().scalarMultiply(wall.getRange()));
		Vector3 direction = user.getDirection();
		double yaw = user.getEntity().getLocation().getYaw();
		double hw = wall.getWidth() / 2.0;
		double hh = wall.getHeight() / 2.0;
		for (double i = 0.5; i <= 2*userConfig.steps; i += 0.5) {
			Vector3 currentPosition = origin.add(direction.scalarMultiply(i));
			if (!Game.getProtectionSystem().canBuild(user, currentPosition.toLocation(user.getWorld()).getBlock())) {
				break;
			}
			hh += 0.2;
			Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.toRadians(yaw), RotationConvention.VECTOR_OPERATOR);
			OBB collider = new OBB(new AABB(new Vector3(-hw, -hh, -0.5), new Vector3(hw, hh, 0.5)), rotation).addPosition(currentPosition);
			double radius = collider.getHalfExtents().maxComponent() + 1;
			List<Block> blocks = WorldMethods.getNearbyBlocks(currentPosition.toLocation(user.getWorld()), radius, b -> collider.contains(new Vector3(b)) && MaterialUtil.isTransparent(b));
			if (blocks.isEmpty()) break;
			walls.offer(new WallInfo(blocks, collider));

		}
		if (walls.isEmpty()) return false;
		wall.updateDuration(userConfig.duration);
		nextTime = 0;
		user.setCooldown(this, userConfig.cooldown);
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		if (wall == null) return UpdateResult.REMOVE;
		long time = System.currentTimeMillis();
		if (time < nextTime) {
			return UpdateResult.CONTINUE;
		}
		nextTime = time + 250;
		WallInfo info = walls.poll();
		if (info == null) return UpdateResult.REMOVE;
		wall.setWall(info.getBlocks(), info.getCollider());
		return walls.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	@Override
	public void destroy() {
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "FireWave";
	}

	@Override
	public List<Collider> getColliders() {
		return Collections.emptyList();
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	private static class WallInfo {
		private final List<Block> blocks;
		private final OBB collider;

		private WallInfo(List<Block> blocks, OBB collider) {
			this.blocks = blocks;
			this.collider = collider;
		}

		private List<Block> getBlocks() {
			return blocks;
		}

		private OBB getCollider() {
			return collider;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.HEIGHT)
		public double maxHeight;
		@Attribute(Attributes.DURATION)
		public long duration;
		@Attribute(Attributes.RANGE)
		public long steps;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "sequences", "firewave");

			cooldown = abilityNode.getNode("cooldown").getLong(11000);
			maxHeight = abilityNode.getNode("max-height").getDouble(10.0);
			duration = abilityNode.getNode("duration").getLong(10000);
			steps = abilityNode.getNode("steps").getInt(8);

			abilityNode.getNode("steps").setComment("The amount of blocks the FireWave will advance.");
		}
	}
}