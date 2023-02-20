/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.ama.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import org.quiltmc.community.ama.database.Collection
import org.quiltmc.community.ama.database.Database
import org.quiltmc.community.ama.database.entities.AmaConfig

class AmaConfigCollection : KordExKoinComponent {
	private val database: Database by inject()
	private val col = database.mongo.getCollection<AmaConfig>(name)

	suspend fun getConfig(guildId: Snowflake): AmaConfig? =
		col.findOne(AmaConfig::guildId eq guildId)

	suspend fun isButtonEnabled(guildId: Snowflake): Boolean? =
		col.findOne(AmaConfig::guildId eq guildId)?.enabled

	suspend fun modifyButton(guildId: Snowflake, enabled: Boolean) =
		col.findOneAndUpdate(AmaConfig::guildId eq guildId, setValue(AmaConfig::enabled, enabled))

	suspend fun setConfig(config: AmaConfig) =
		col.save(config)

	companion object : Collection("ama_config")
}
