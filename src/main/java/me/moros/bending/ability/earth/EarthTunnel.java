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

package me.moros.bending.ability.earth;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class EarthTunnel extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Predicate<Block> predicate;
  private Vector3d center;

  private double distance = 0;
  private int radius = 0;
  private int angle = 0;

  public EarthTunnel(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, EarthTunnel.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    predicate = b -> EarthMaterials.isEarthNotLava(user, b);
    Block block = user.find(userConfig.range, predicate);
    if (block == null) {
      return false;
    }

    center = Vector3d.center(block);
    removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();

    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    for (int i = 0; i < 2; i++) {
      if (distance > userConfig.range) {
        return UpdateResult.REMOVE;
      }
      Vector3d offset = VectorMethods.orthogonal(user.direction(), Math.toRadians(angle), radius);
      Block current = center.add(offset).toBlock(user.world());
      if (!user.canBuild(current)) {
        return UpdateResult.REMOVE;
      }
      if (predicate.test(current)) {
        if (userConfig.extractOres) {
          extract(current);
        }
        TempBlock.createAir(current, BendingProperties.EARTHBENDING_REVERT_TIME);
      }
      if (angle >= 360) {
        angle = 0;
        Block block = user.find(userConfig.range, predicate);
        if (block == null) {
          return UpdateResult.REMOVE;
        }
        center = Vector3d.center(block);

        if (++radius > userConfig.radius) {
          radius = 0;
          if (++distance > userConfig.range) {
            return UpdateResult.REMOVE;
          }
        }
      } else {
        if (radius <= 0) {
          radius++;
        } else {
          angle += 360 / (radius * 16);
        }
      }
    }
    return UpdateResult.CONTINUE;
  }

  // TODO tweak drop rates
  private void extract(Block block) {
    if (TempBlock.MANAGER.isTemp(block)) {
      return;
    }
    Material type = block.getType();
    if (!MaterialUtil.ORES.containsKey(type)) {
      return;
    }
    Material drop = MaterialUtil.ORES.get(type);
    int amount = getAmount(drop);
    if (amount == 0) {
      return;
    }

    Material newType;
    if (type.name().contains("NETHER")) {
      newType = Material.NETHERRACK;
    } else if (type.name().contains("DEEPSLATE")) {
      newType = Material.DEEPSLATE;
    } else {
      newType = Material.STONE;
    }
    block.setType(newType);

    int rand = ThreadLocalRandom.current().nextInt(100);
    int factor = rand >= 75 ? 3 : rand >= 50 ? 2 : 1;
    block.getWorld().dropItem(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(drop, factor * amount));
  }

  private int getAmount(Material type) {
    return switch (type) {
      case COAL, DIAMOND, EMERALD, QUARTZ, IRON_INGOT, GOLD_INGOT -> 1;
      case REDSTONE -> 5;
      case GOLD_NUGGET -> 6;
      case LAPIS_LAZULI -> 9;
      default -> 0;
    };
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.RADIUS)
    public double radius;
    public boolean extractOres;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthtunnel");

      cooldown = abilityNode.node("cooldown").getLong(2000);
      range = abilityNode.node("range").getDouble(10.0);
      radius = abilityNode.node("radius").getDouble(1.0);
      extractOres = abilityNode.node("extract-ores").getBoolean(true);
    }
  }
}
