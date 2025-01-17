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

package me.moros.bending.ability.air;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.RayTrace;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AirBlast extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private AirStream stream;
  private Vector3d origin;

  private boolean launched;
  private boolean selectedOrigin;

  public AirBlast(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();

    if (Policies.IN_LIQUID.test(user, description())) {
      return false;
    }

    removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(userConfig.selectRange * 2, () -> origin))
      .add(Policies.IN_LIQUID)
      .build();

    for (AirBlast blast : Bending.game().abilityManager(user.world()).userInstances(user, AirBlast.class).collect(Collectors.toList())) {
      if (!blast.launched) {
        if (method == Activation.SNEAK_RELEASE) {
          if (!blast.selectOrigin()) {
            Bending.game().abilityManager(user.world()).destroyInstance(blast);
          }
        } else {
          blast.launch();
        }
        return false;
      }
    }

    if (method == Activation.SNEAK_RELEASE) {
      return selectOrigin();
    }
    origin = user.eyeLocation();
    launch();
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

    if (!launched) {
      if (!description().equals(user.selectedAbility())) {
        return UpdateResult.REMOVE;
      }
      ParticleUtil.createAir(origin.toLocation(user.world())).count(4).offset(0.5, 0.5, 0.5).spawn();
    }

    return (!launched || stream.update() == UpdateResult.CONTINUE) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
  }

  private boolean selectOrigin() {
    origin = RayTrace.of(user).range(userConfig.selectRange).result(user.world()).position().subtract(user.direction().multiply(0.5));
    selectedOrigin = true;
    return user.canBuild(origin.toBlock(user.world()));
  }

  private void launch() {
    launched = true;
    Vector3d target = user.compositeRayTrace(userConfig.range).result(user.world()).entityCenterOrPosition();
    if (user.sneaking()) {
      Vector3d temp = new Vector3d(origin.toArray());
      origin = new Vector3d(target.toArray());
      target = temp;
    }
    Vector3d direction = target.subtract(origin).normalize();
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
    stream = new AirStream(new Ray(origin, direction.multiply(userConfig.range)));
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return stream == null ? List.of() : List.of(stream.collider());
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class AirStream extends ParticleStream {
    public AirStream(Ray ray) {
      super(user, ray, userConfig.speed, 1.3);
      canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b);
      livingOnly = false;
    }

    @Override
    public void render() {
      ParticleUtil.createAir(bukkitLocation()).count(6)
        .offset(0.275, 0.275, 0.275)
        .spawn();
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.AIR.play(bukkitLocation());
      }

      // Handle user separately from the general entity collision.
      if (selectedOrigin) {
        if (AABBUtils.entityBounds(user.entity()).intersects(collider)) {
          onEntityHit(user.entity());
        }
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      boolean isUser = entity.equals(user.entity());
      double factor = isUser ? userConfig.self : userConfig.other;
      BendingEffect.FIRE_TICK.reset(entity);
      if (factor == 0) {
        return false;
      }

      Vector3d push = ray.direction.normalize();
      if (!isUser) {
        // Cap vertical push
        push = push.setY(Math.max(-0.3, Math.min(0.3, push.getY())));
      }

      factor *= 1 - (location.distance(ray.origin) / (2 * maxRange));
      // Reduce the push if the player is on the ground.
      if (isUser && user.isOnGround()) {
        factor *= 0.5;
      }
      Vector3d velocity = new Vector3d(entity.getVelocity());
      // The strength of the entity's velocity in the direction of the blast.
      double strength = velocity.dot(push.normalize());
      if (strength > factor) {
        double f = velocity.normalize().dot(push.normalize());
        velocity = velocity.multiply(0.5).add(push.normalize().multiply(f));
      } else if (strength + factor * 0.5 > factor) {
        velocity = velocity.add(push.multiply(factor - strength));
      } else {
        velocity = velocity.add(push.multiply(factor * 0.5));
      }
      EntityMethods.applyVelocity(AirBlast.this, entity, velocity);
      entity.setFallDistance(0);
      return false;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      if (BlockMethods.tryExtinguishFire(user, block)) {
        return false;
      }
      BlockMethods.tryCoolLava(user, block);
      return true;
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.STRENGTH)
    public double self;
    @Modifiable(Attribute.STRENGTH)
    public double other;
    @Modifiable(Attribute.SELECTION)
    public double selectRange;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airblast");

      cooldown = abilityNode.node("cooldown").getLong(1250);
      range = abilityNode.node("range").getDouble(20.0);
      speed = abilityNode.node("speed").getDouble(1.2);
      self = abilityNode.node("power-self").getDouble(2.1);
      other = abilityNode.node("power-other").getDouble(2.1);
      selectRange = abilityNode.node("select-range").getDouble(8.0);
    }
  }
}
