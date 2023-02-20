/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.ama

import com.kotlindiscord.kord.extensions.ExtensibleBot
import org.quiltmc.community.ama.database.storage.MongoDBDataAdapter
import org.quiltmc.community.ama.extensions.AmaExtension

suspend fun main() {
	val bot = ExtensibleBot(BOT_TOKEN) {
		database(true)
		dataAdapter(::MongoDBDataAdapter)

		extensions {
			add(::AmaExtension)

			sentry {
				enableIfDSN(SENTRY_DSN)
			}
		}
	}

	bot.start()
}
