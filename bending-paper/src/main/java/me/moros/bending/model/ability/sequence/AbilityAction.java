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

package me.moros.bending.model.ability.sequence;

import me.moros.bending.model.ability.description.AbilityDescription;

public final class AbilityAction {
	private final AbilityDescription desc;
	private final Action action;

	public AbilityAction(AbilityDescription desc, Action action) {
		this.desc = desc;
		this.action = action;
	}

	public AbilityDescription getAbilityDescription() {
		return desc;
	}

	public Action getAction() {
		return action;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof AbilityAction) {
			AbilityAction otherAction = ((AbilityAction) other);
			return action == otherAction.action && desc.equals(otherAction.desc);
		}
		return false;
	}
}
