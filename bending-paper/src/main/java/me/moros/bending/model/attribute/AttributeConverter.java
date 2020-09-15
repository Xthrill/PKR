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

package me.moros.bending.model.attribute;

import java.util.function.DoubleFunction;

public enum AttributeConverter {
	DOUBLE(x -> x),
	INT(x -> (int) x),
	LONG(x -> (long) x);

	private final Converter converter;

	AttributeConverter(Converter converter) {
		this.converter = converter;
	}

	public Number apply(double input) {
		return converter.apply(input);
	}

	@FunctionalInterface
	private interface Converter extends DoubleFunction<Number> {
		Number apply(double input);
	}
}
