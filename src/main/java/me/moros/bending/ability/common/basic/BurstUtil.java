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

import java.util.ArrayList;
import java.util.Collection;

import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.methods.EntityMethods;
import org.apache.commons.math3.util.FastMath;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class BurstUtil {
  public static final double ANGLE_STEP = FastMath.toRadians(10);
  public static final double ANGLE = FastMath.toRadians(30);
  public static final double FALL_MIN_ANGLE = FastMath.toRadians(60);
  public static final double FALL_MAX_ANGLE = FastMath.toRadians(105);

  public static @NonNull Collection<@NonNull Ray> cone(@NonNull User user, double range) {
    return createBurst(user, range, ANGLE_STEP, ANGLE);
  }

  public static @NonNull Collection<@NonNull Ray> sphere(@NonNull User user, double range) {
    return createBurst(user, range, ANGLE_STEP, 0);
  }

  public static @NonNull Collection<@NonNull Ray> fall(@NonNull User user, double range) {
    return createBurst(user, range, ANGLE_STEP, -1);
  }

  // Negative angle for fall burst
  public static @NonNull Collection<@NonNull Ray> createBurst(@NonNull User user, double range, double angleStep, double angle) {
    Vector3 center = EntityMethods.entityCenter(user.entity());
    Vector3 userDIr = user.direction();
    Collection<Ray> rays = new ArrayList<>();
    for (double theta = 0; theta < FastMath.PI; theta += angleStep) {
      double z = FastMath.cos(theta);
      double sinTheta = FastMath.sin(theta);
      for (double phi = 0; phi < 2 * FastMath.PI; phi += angleStep) {
        double x = FastMath.cos(phi) * sinTheta;
        double y = FastMath.sin(phi) * sinTheta;
        Vector3 direction = new Vector3(x, y, z);
        if (angle > 0 && Vector3.angle(direction, userDIr) > angle) {
          continue;
        }
        if (angle < 0) {
          double vectorAngle = Vector3.angle(direction, Vector3.PLUS_J);
          if (vectorAngle < FALL_MIN_ANGLE || vectorAngle > FALL_MAX_ANGLE) {
            continue;
          }
        }
        rays.add(new Ray(center, direction.scalarMultiply(range)));
      }
    }
    return rays;
  }
}
