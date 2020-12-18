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

package me.moros.bending.ability.earth;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.Pillar;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

public class RaiseEarth extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Block origin;
	private Predicate<Block> predicate;
	private final Collection<Pillar> pillars = new ArrayList<>();

	public RaiseEarth(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		predicate = b -> EarthMaterials.isEarthbendable(user, b) && !EarthMaterials.isLavaBendable(b);
		Optional<Block> source = SourceUtil.getSource(user, userConfig.selectRange, predicate);
		if (!source.isPresent()) return false;
		origin = source.get();

		boolean wall = method == ActivationMethod.SNEAK;
		if (wall) {
			raiseWall();
		} else {
			getTopValid(origin, userConfig.columnMaxHeight).ifPresent(b -> createPillar(b, userConfig.columnMaxHeight));
		}
		if (!pillars.isEmpty()) {
			user.setCooldown(getDescription(), wall ? userConfig.wallCooldown : userConfig.columnCooldown);
			removalPolicy = Policies.builder().build();
			return true;
		}
		return false;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}
		pillars.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		return pillars.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	private void createPillar(Block block, int height) {
		for (Block b : new Block[]{ block, block.getRelative(BlockFace.DOWN) }) {
			if (!predicate.test(b) || !TempBlock.isBendable(b)) return;
		}
		Pillar.buildPillar(user, block, BlockFace.UP, height, BendingProperties.EARTHBENDING_REVERT_TIME, predicate)
			.ifPresent(pillars::add);
	}

	private void raiseWall() {
		double w = (userConfig.wallWidth - 1) / 2.0;
		int height = userConfig.wallMaxHeight;
		Vector3 side = user.getDirection().crossProduct(Vector3.PLUS_J).normalize();
		Vector3 center = new Vector3(origin).add(Vector3.HALF);
		for (int i = -NumberConversions.ceil(w); i <= NumberConversions.floor(w); i++) {
			Block check = center.add(side.scalarMultiply(i)).toBlock(user.getWorld());
			if (MaterialUtil.isTransparent(check)) {
				for (int j = 1; j < height; j++) {
					Block block = check.getRelative(BlockFace.DOWN, j);
					if (predicate.test(block)) {
						createPillar(block, height);
						break;
					} else if (!MaterialUtil.isTransparent(block)) {
						break;
					}
				}
			} else {
				getTopValid(check, height).ifPresent(b -> createPillar(b, height));
			}
		}
	}

	private Optional<Block> getTopValid(Block block, int height) {
		for (int i = 1; i <= height; i++) {
			Block check = block.getRelative(BlockFace.UP, i);
			if (!predicate.test(check)) return Optional.of(check.getRelative(BlockFace.DOWN));
		}
		return Optional.empty();
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.SELECTION)
		public double selectRange;
		@Attribute(Attribute.COOLDOWN)
		public long columnCooldown;
		@Attribute(Attribute.HEIGHT)
		public int columnMaxHeight;
		@Attribute(Attribute.COOLDOWN)
		public long wallCooldown;
		@Attribute(Attribute.HEIGHT)
		public int wallMaxHeight;
		@Attribute(Attribute.RADIUS)
		public int wallWidth;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "earth", "raiseearth");

			selectRange = abilityNode.getNode("select-range").getDouble(20.0);

			CommentedConfigurationNode columnNode = abilityNode.getNode("column");
			columnCooldown = columnNode.getNode("cooldown").getLong(500);
			columnMaxHeight = columnNode.getNode("max-height").getInt(6);

			CommentedConfigurationNode wallNode = abilityNode.getNode("wall");
			wallCooldown = wallNode.getNode("cooldown").getLong(500);
			wallMaxHeight = wallNode.getNode("max-height").getInt(6);
			wallWidth = wallNode.getNode("width").getInt(6);
		}
	}
}