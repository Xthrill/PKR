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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.game.temporal.TempFallingBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Explosive;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingExplosion;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.RayTrace;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class EarthShot extends AbilityInstance implements Explosive {
  private static final AABB BOX = AABB.BLOCK_BOUNDS.grow(new Vector3d(0.25, 0.25, 0.25));

  private enum Mode {ROCK, METAL, MAGMA}

  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Mode mode;
  private Block source;
  private Block readySource;
  private BlockData data;
  private Vector3d location;
  private Vector3d lastVelocity;
  private TempFallingBlock projectile;

  private boolean ready = false;
  private boolean launched = false;
  private boolean canConvert = false;
  private boolean exploded = false;
  private double damage;
  private int targetY;
  private long magmaStartTime = 0;

  public EarthShot(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (method == Activation.ATTACK) {
      Bending.game().abilityManager(user.world()).userInstances(user, EarthShot.class)
        .filter(e -> !e.launched).forEach(EarthShot::launch);
      return false;
    }

    this.user = user;
    loadConfig();

    long count = Bending.game().abilityManager(user.world()).userInstances(user, EarthShot.class).filter(e -> !e.launched).count();
    if (count >= userConfig.maxAmount) {
      return false;
    }

    canConvert = userConfig.allowConvertMagma && user.hasPermission("bending.lava");

    return prepare();
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  private boolean prepare() {
    source = user.find(userConfig.selectRange, b -> EarthMaterials.isEarthbendable(user, b));
    if (source == null) {
      return false;
    }
    mode = getType(source);
    int deltaY = 3;
    if (source.getY() >= user.headBlock().getY()) {
      targetY = source.getY() + 2;
    } else {
      targetY = user.locBlock().getY() + 2;
      deltaY = 1 + targetY - source.getY();
    }

    for (int i = 1; i <= deltaY; i++) {
      Block temp = source.getRelative(BlockFace.UP, i);
      if (!MaterialUtil.isTransparent(temp)) {
        return false;
      }
      BlockMethods.tryBreakPlant(temp);
    }

    data = source.getBlockData().clone();
    BlockData solidData;
    if (mode == Mode.MAGMA) {
      solidData = Material.MAGMA_BLOCK.createBlockData();
      canConvert = false;
    } else {
      solidData = MaterialUtil.solidType(source.getBlockData());
    }
    if (mode == Mode.METAL) {
      SoundUtil.METAL.play(source.getLocation());
      canConvert = false;
    } else {
      SoundUtil.EARTH.play(source.getLocation());
    }

    projectile = new TempFallingBlock(source, solidData, new Vector3d(0, 0.65, 0), false, 6000);
    if (!MaterialUtil.isLava(source)) {
      TempBlock.createAir(source, BendingProperties.EARTHBENDING_REVERT_TIME);
    }
    location = projectile.center();
    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(OutOfRangeRemovalPolicy.of(userConfig.selectRange + 10, () -> location))
      .build();

    return true;
  }

  @Override
  public @NonNull UpdateResult update() {
    if (exploded || removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    if (launched) {
      if (projectile == null || !projectile.fallingBlock().isValid()) {
        return UpdateResult.REMOVE;
      }

      Vector3d velocity = new Vector3d(projectile.fallingBlock().getVelocity());
      double minLength = userConfig.speed * 0.85;
      if (lastVelocity.angle(velocity) > Math.PI / 4 || velocity.lengthSq() < minLength * minLength) {
        return UpdateResult.REMOVE;
      }
      if (user.sneaking()) {
        Vector3d dir = user.direction().multiply(0.2);
        velocity = velocity.add(dir.setY(0));
      }
      EntityMethods.applyVelocity(this, projectile.fallingBlock(), velocity.normalize().multiply(userConfig.speed));
      lastVelocity = new Vector3d(projectile.fallingBlock().getVelocity());
      Collider c = BOX.at(projectile.center());
      boolean magma = mode == Mode.MAGMA;
      if (CollisionUtil.handleEntityCollisions(user, c, this::onEntityHit, true, false, magma)) {
        return UpdateResult.REMOVE;
      }
    } else {
      if (!ready) {
        handleSource();
      } else {
        handleMagma();
      }
    }

    return UpdateResult.CONTINUE;
  }

  private boolean onEntityHit(Entity entity) {
    if (mode == Mode.MAGMA) {
      explode();
      return false;
    }
    DamageUtil.damageEntity(entity, user, damage, description());
    Vector3d velocity = new Vector3d(projectile.fallingBlock().getVelocity()).normalize().multiply(0.4);
    EntityMethods.applyVelocity(this, entity, velocity);
    return true;
  }

  private void handleSource() {
    Block block = projectile.fallingBlock().getLocation().getBlock();
    if (block.getY() >= targetY) {
      TempBlock.create(block, projectile.fallingBlock().getBlockData());
      projectile.revert();
      location = new Vector3d(block);
      readySource = block;
      ready = true;
    } else {
      location = projectile.center();
    }
  }

  private void handleMagma() {
    if (!canConvert) {
      return;
    }
    Block check = RayTrace.of(user).range(userConfig.selectRange * 2).ignoreLiquids(false).result(user.world()).block();
    if (user.sneaking() && readySource.equals(check)) {
      if (magmaStartTime == 0) {
        magmaStartTime = System.currentTimeMillis();
        if (userConfig.chargeTime > 0) {
          SoundUtil.LAVA.play(readySource.getLocation());
        }
      }
      Location spawnLoc = readySource.getLocation().add(0.5, 0.5, 0.5);
      ParticleUtil.create(Particle.LAVA, spawnLoc).count(2).offset(0.5, 0.5, 0.5).spawn();
      ParticleUtil.create(Particle.SMOKE_NORMAL, spawnLoc).count(2).offset(0.5, 0.5, 0.5).spawn();
      ParticleUtil.createRGB(spawnLoc, "FFA400").count(2).offset(0.5, 0.5, 0.5).spawn();
      ParticleUtil.createRGB(spawnLoc, "FF8C00").count(4).offset(0.5, 0.5, 0.5).spawn();
      if (userConfig.chargeTime <= 0 || System.currentTimeMillis() > magmaStartTime + userConfig.chargeTime) {
        mode = Mode.MAGMA;
        TempBlock.create(readySource, Material.MAGMA_BLOCK.createBlockData());
        canConvert = false;
      }
    } else {
      if (magmaStartTime != 0 && ThreadLocalRandom.current().nextInt(6) == 0) {
        removalPolicy = (u, d) -> true; // Remove in next tick
        return;
      }
      magmaStartTime = 0;
    }
  }

  private Mode getType(Block block) {
    if (EarthMaterials.isLavaBendable(block)) {
      return Mode.MAGMA;
    } else if (EarthMaterials.isMetalBendable(block)) {
      return Mode.METAL;
    } else {
      return Mode.ROCK;
    }
  }

  private void launch() {
    if (launched) {
      return;
    }

    boolean prematureLaunch = false;
    if (!ready) {
      if (!userConfig.allowQuickLaunch) {
        return;
      }
      prematureLaunch = true;
    }

    Vector3d origin;
    if (prematureLaunch) {
      origin = projectile.center();
      Vector3d dir = getTarget(null).subtract(origin).normalize().multiply(userConfig.speed);
      projectile.fallingBlock().setGravity(true);
      EntityMethods.applyVelocity(this, projectile.fallingBlock(), dir.add(new Vector3d(0, 0.2, 0)));
    } else {
      origin = Vector3d.center(readySource);
      Vector3d dir = getTarget(readySource).subtract(origin).normalize().multiply(userConfig.speed);
      projectile = new TempFallingBlock(readySource, readySource.getBlockData(), dir.add(new Vector3d(0, 0.2, 0)), true, 30000);
      TempBlock.createAir(readySource);
    }
    location = projectile.center();
    lastVelocity = new Vector3d(projectile.fallingBlock().getVelocity());

    removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(userConfig.range, origin, () -> location))
      .build();

    user.addCooldown(description(), userConfig.cooldown);

    double dmgFactor = switch (mode) {
      case METAL -> BendingProperties.METAL_MODIFIER;
      case MAGMA -> BendingProperties.MAGMA_MODIFIER;
      default -> 1;
    };
    damage = userConfig.damage * dmgFactor;
    launched = true;
  }

  private Vector3d getTarget(Block source) {
    return user.compositeRayTrace(userConfig.range).result(user.world(), b -> b.equals(source)).entityCenterOrPosition();
  }


  @Override
  public void explode() {
    if (exploded || mode != Mode.MAGMA) {
      return;
    }
    exploded = true;
    Vector3d center = projectile.center();
    Location spawnLoc = center.toLocation(user.world());
    ParticleUtil.create(Particle.SMOKE_LARGE, spawnLoc).count(12).offset(1, 1, 1).extra(0.05).spawn();
    ParticleUtil.create(Particle.FIREWORKS_SPARK, spawnLoc).count(8).offset(1, 1, 1).extra(0.07).spawn();
    BendingExplosion.builder()
      .size(userConfig.explosionRadius)
      .damage(damage)
      .fireTicks(0)
      .particles(false)
      .soundEffect(SoundUtil.EXPLOSION)
      .buildAndExplode(this, center);
  }

  @Override
  public void onDestroy() {
    if (projectile != null) {
      if (launched) {
        Location spawnLoc = projectile.center().toLocation(user.world());
        BlockData data = projectile.fallingBlock().getBlockData();
        ParticleUtil.create(Particle.BLOCK_CRACK, spawnLoc).count(6).offset(1, 1, 1).data(data).spawn();
        ParticleUtil.create(Particle.BLOCK_DUST, spawnLoc).count(4).offset(1, 1, 1).data(data).spawn();
        explode();
        Block projected = projectile.center().add(lastVelocity.normalize().multiply(0.75)).toBlock(user.world());
        FragileStructure.tryDamageStructure(List.of(projected), mode == Mode.MAGMA ? 6 : 4);
      }
      projectile.revert();
    }
    if (!launched) {
      TempBlock.create(source, data, BendingProperties.EARTHBENDING_REVERT_TIME, true);
      if (readySource != null) {
        TempBlock.createAir(readySource);
      }
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return (!launched || projectile == null) ? List.of() : List.of(BOX.at(projectile.center()));
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.SELECTION)
    public double selectRange;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.CHARGE_TIME)
    public long chargeTime;
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.AMOUNT)
    public int maxAmount;

    public boolean allowQuickLaunch;

    public boolean allowConvertMagma;
    @Modifiable(Attribute.RADIUS)
    public double explosionRadius;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthshot");

      cooldown = abilityNode.node("cooldown").getLong(2000);
      selectRange = abilityNode.node("select-range").getDouble(6.0);
      range = abilityNode.node("range").getDouble(48.0);
      damage = abilityNode.node("damage").getDouble(3.0);
      chargeTime = abilityNode.node("charge-time").getLong(1000);
      speed = abilityNode.node("speed").getDouble(1.6);
      maxAmount = abilityNode.node("max-sources").getInt(1);
      allowQuickLaunch = abilityNode.node("allow-quick-launch").getBoolean(true);

      CommentedConfigurationNode magmaNode = abilityNode.node("magma");

      allowConvertMagma = magmaNode.node("allow-convert").getBoolean(true);
      explosionRadius = magmaNode.node("radius").getDouble(1.5);
    }
  }
}
