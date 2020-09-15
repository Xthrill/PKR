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

package me.moros.bending.model.predicates.conditionals;

import me.moros.bending.game.Game;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;

public enum BendingConditions implements BendingConditional {
	COOLDOWN((u, d) -> (d.canBypassCooldown() || !u.isOnCooldown(d))),
	ELEMENT((u, d) -> u.hasElement(d.getElement())),
	PERMISSION((u, d) -> u.hasPermission(d)),
	WORLD((u, d) -> !Game.isDisabledWorld(u.getWorld().getUID())),
	TOGGLED((u, d) -> false),
	GAMEMODE((u, d) -> !u.isSpectator());

	private final BendingConditional predicate;

	BendingConditions(BendingConditional predicate) {
		this.predicate = predicate;
	}

	@Override
	public boolean canBend(User user, AbilityDescription desc) {
		return predicate.canBend(user, desc);
	}
}
