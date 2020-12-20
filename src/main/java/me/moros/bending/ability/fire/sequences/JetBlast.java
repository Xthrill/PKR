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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.fire.*;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class JetBlast extends AbilityInstance implements Ability {
	private static final Config config = new Config();
	private static AbilityDescription jetDesc;

	private Config userConfig;

	private FireJet jet;

	public JetBlast(@NonNull AbilityDescription desc) {
		super(desc);
		if (jetDesc == null) {
			jetDesc = Bending.getGame().getAbilityRegistry()
				.getAbilityDescription("FireJet").orElseThrow(RuntimeException::new);
		}
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		jet = new FireJet(jetDesc);
		if (user.isOnCooldown(jet.getDescription()) || !jet.activate(user, ActivationMethod.PUNCH)) return false;

		recalculateConfig();

		jet.setDuration(userConfig.duration);
		jet.setSpeed(userConfig.speed);

		user.getWorld().playSound(user.getEntity().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 4, 0);

		user.setCooldown(getDescription(), userConfig.cooldown);
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		ParticleUtil.create(Particle.SMOKE_NORMAL, jet.getUser().getEntity().getLocation()).count(5)
			.offset(0.3, 0.3, 0.3).spawn();
		return jet.update();
	}

	@Override
	public void onDestroy() {
		jet.onDestroy();
	}

	@Override
	public @NonNull User getUser() {
		return jet.getUser();
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.SPEED)
		public double speed;
		@Attribute(Attribute.DURATION)
		private long duration;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "sequences", "jetblast");

			cooldown = abilityNode.getNode("cooldown").getLong(6000);
			speed = abilityNode.getNode("speed").getDouble(1.2);
			duration = abilityNode.getNode("duration").getLong(5000);
		}
	}
}
