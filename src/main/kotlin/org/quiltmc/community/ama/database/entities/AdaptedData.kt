/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.ama.database.entities

import com.kotlindiscord.kord.extensions.storage.StorageType
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.quiltmc.community.ama.database.Entity

@Serializable
@Suppress("ConstructorParameterNaming", "DataClassShouldBeImmutable")
data class AdaptedData(
	override val _id: String,
	val identifier: String,

	val type: StorageType? = null,

	val channel: Snowflake? = null,
	val guild: Snowflake? = null,
	val message: Snowflake? = null,
	val user: Snowflake? = null,

	var data: String
) : Entity<String>
