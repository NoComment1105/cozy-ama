/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.ama.database

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import mu.KotlinLogging
import org.koin.core.component.inject
import org.quiltmc.community.ama.database.collections.MetaCollection
import org.quiltmc.community.ama.database.entities.Meta
import org.quiltmc.community.ama.database.migrations.v1

object Migrator : KordExKoinComponent {
	private val logger = KotlinLogging.logger {  }

	val db: Database by inject()
	val metaColl: MetaCollection by inject()

	suspend fun migrate() {
		var meta = metaColl.get()

		if (meta == null) {
			meta = Meta(0)

			metaColl.set(meta)
		}

		var currentVersion = meta.version

		logger.info { "Current database version: v$currentVersion" }

		while (true) {
			val nextVersion = currentVersion + 1

			@Suppress("TooGenericExceptionCaught")
			try {
				@Suppress("UseIfInsteadOfWhen")
				when (nextVersion) {
					1 -> ::v1

					else -> break
				}(db.mongo)

				logger.info { "Migrated database to v$nextVersion" }
			} catch (t: Throwable) {
				logger.error(t) { "Failed to migrate database to v$nextVersion" }

				throw t
			}

			currentVersion = nextVersion
		}

		if (currentVersion != meta.version) {
			meta = meta.copy(version = currentVersion)

			metaColl.set(meta)

			logger.info { "Finished database migrations." }
		}
	}
}
