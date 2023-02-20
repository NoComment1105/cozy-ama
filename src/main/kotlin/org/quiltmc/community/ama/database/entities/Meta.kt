/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.ama.database.entities

import kotlinx.serialization.Serializable
import org.quiltmc.community.ama.database.Entity

@Serializable
@Suppress("ConstructorParameterNaming")
data class Meta(
	val version: Int,

	override val _id: String = "meta"
) : Entity<String>
