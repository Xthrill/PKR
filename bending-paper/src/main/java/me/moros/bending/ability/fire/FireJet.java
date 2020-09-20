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

package me.moros.bending.ability.fire;

import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import me.moros.bending.util.Flight;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.material.MaterialUtil;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;

public class FireJet implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private Flight flight;
	private long startTime;
	private double speed;
	private long duration;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		startTime = System.currentTimeMillis();

		Block block = user.getLocation().toLocation(user.getWorld()).getBlock();

		boolean ignitable = MaterialUtil.isIgnitable(block);
		if (!ignitable && !MaterialUtil.isAir(block.getType())) {
			return false;
		}

		if (!Game.getProtectionSystem().canBuild(user, block)) {
			return false;
		}

		speed = userConfig.speed;
		duration = userConfig.duration;

		flight = Flight.get(user);
		user.setCooldown(this, userConfig.cooldown);
		if (ignitable) TempBlock.create(block, Material.FIRE, 3000);
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		long time = System.currentTimeMillis();

		if (System.currentTimeMillis() > startTime + duration) {
			return UpdateResult.REMOVE;
		}

		if (user.getLocation().toLocation(user.getWorld()).getBlock().isLiquid()) {
			return UpdateResult.REMOVE;
		}

		// scale down to 0.5 speed near the end
		double factor = 1 - ((time - startTime) / (2.0 * duration));

		user.getEntity().setVelocity(user.getDirection().scalarMultiply(speed * factor).toVector());
		user.getEntity().setFallDistance(0);
		ParticleUtil.createFire(user, user.getLocation().toLocation(user.getWorld())).count(10)
			.offset(0.3, 0.3, 0.3).spawn();
		ParticleUtil.create(Particle.SMOKE_NORMAL, user.getLocation().toLocation(user.getWorld())).count(5)
			.offset(0.3, 0.3, 0.3).spawn();

		return UpdateResult.CONTINUE;
	}

	@Override
	public void destroy() {
		flight.release();
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "FireJet";
	}

	@Override
	public void handleCollision(Collision collision) {
	}

	public void setSpeed(double newSpeed) {
		this.speed = newSpeed;
	}

	public void setDuration(long newDuration) {
		this.duration = newDuration;
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.SPEED)
		public double speed;
		@Attribute(Attributes.DURATION)
		private long duration;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "firejet");

			cooldown = abilityNode.getNode("cooldown").getLong(7000);
			speed = abilityNode.getNode("speed").getDouble(0.8);
			duration = abilityNode.getNode("duration").getLong(2000);
		}
	}
}