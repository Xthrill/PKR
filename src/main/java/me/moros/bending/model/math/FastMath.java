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

package me.moros.bending.model.math;

public final class FastMath {
  private FastMath() {
  }

  public static int floor(double num) {
    int y = (int) num;
    if (num < y) {
      return y - 1;
    }
    return y;
  }

  public static int ceil(double num) {
    int y = (int) num;
    if (num > y) {
      return y + 1;
    }
    return y;
  }

  public static int round(double num) {
    return floor(num + 0.5);
  }
}