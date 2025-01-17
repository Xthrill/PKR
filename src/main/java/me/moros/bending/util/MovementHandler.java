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

package me.moros.bending.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.moros.bending.Bending;
import me.moros.bending.events.BendingRestrictEvent;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.user.User;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MovementHandler {
  public static final Map<UUID, MovementHandler> instances = new HashMap<>();
  private static DummyMovementHandler DUMMY;

  private Set<ActionType> disabled = EnumSet.noneOf(ActionType.class);
  private BarInfo info;

  private final LivingEntity entity;
  private final boolean hadAI;

  private MovementHandler() {
    entity = null;
    hadAI = true;
  }

  private MovementHandler(LivingEntity entity, long duration) {
    this.entity = entity;
    hadAI = entity.hasAI();
    if (entity instanceof Player player) {
      info = new BarInfo(player, duration);
    } else {
      entity.setAI(false);
    }
    entity.setMetadata(Metadata.NO_MOVEMENT, Metadata.customMetadata(this));
    Tasker.sync(this::reset, FastMath.ceil(duration / 50.0));
  }

  private void reset() {
    resetWithoutRemoving();
    instances.remove(entity.getUniqueId());
  }

  private void resetWithoutRemoving() {
    if (info != null) {
      info.remove();
    }
    entity.setAI(hadAI);
    if (entity.hasMetadata(Metadata.NO_MOVEMENT)) {
      entity.removeMetadata(Metadata.NO_MOVEMENT, Bending.plugin());
    }
  }

  public @NonNull MovementHandler disableActions(@NonNull Collection<@NonNull ActionType> methods) {
    disabled = EnumSet.copyOf(methods);
    return this;
  }

  public @NonNull MovementHandler disableActions(@NonNull ActionType method, @Nullable ActionType... methods) {
    Collection<ActionType> c = new ArrayList<>();
    c.add(method);
    if (methods != null) {
      c.addAll(List.of(methods));
    }
    return disableActions(c);
  }

  public static @NonNull MovementHandler restrictEntity(@NonNull User user, @NonNull LivingEntity entity, long duration) {
    BendingRestrictEvent event = Bending.eventBus().postRestrictEvent(user, entity, duration);
    if (event.isCancelled() || event.duration() <= 0) {
      if (DUMMY == null) {
        DUMMY = new DummyMovementHandler();
      }
      return DUMMY;
    }
    return instances.computeIfAbsent(entity.getUniqueId(), e -> new MovementHandler(entity, event.duration()));
  }

  public static boolean isRestricted(@NonNull Entity entity) {
    return isRestricted(entity, null);
  }

  public static boolean isRestricted(@NonNull Entity entity, @Nullable ActionType method) {
    if (entity.hasMetadata(Metadata.NO_MOVEMENT)) {
      if (method == null) {
        return true;
      }
      MovementHandler handler = (MovementHandler) entity.getMetadata(Metadata.NO_MOVEMENT).get(0).value();
      if (handler != null) {
        return handler.disabled.contains(method);
      }
    }
    return false;
  }

  /**
   * Resets all instances of MovementHandler
   */
  public static void resetAll() {
    instances.values().forEach(MovementHandler::resetWithoutRemoving);
    instances.clear();
  }

  private static class BarInfo {
    private final Player player;
    private final BossBar bar;
    private final BukkitTask barTask;

    private final long endTime;
    private final long duration;

    private BarInfo(Player player, long duration) {
      this.player = player;
      this.duration = duration;
      endTime = System.currentTimeMillis() + duration;
      Component name = Component.text("Restricted");
      bar = BossBar.bossBar(name, 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
      barTask = Tasker.repeatingTask(this::updateBar, 1);
    }

    private void updateBar() {
      long time = System.currentTimeMillis();
      if (time > endTime) {
        remove();
      } else {
        float factor = Math.max(0, Math.min(1, (endTime - time) / (float) duration));
        player.showBossBar(bar.progress(factor));
      }
    }

    private void remove() {
      player.hideBossBar(bar);
      if (barTask != null) {
        barTask.cancel();
      }
    }
  }

  private static class DummyMovementHandler extends MovementHandler {
    private DummyMovementHandler() {
      super();
    }

    @Override
    public @NonNull MovementHandler disableActions(@NonNull Collection<@NonNull ActionType> methods) {
      return this;
    }

    @Override
    public @NonNull MovementHandler disableActions(@NonNull ActionType method, @Nullable ActionType... methods) {
      return this;
    }
  }
}
