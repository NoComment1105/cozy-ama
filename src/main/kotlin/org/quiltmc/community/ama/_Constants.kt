/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.ama

import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull

/** The Bot Token. */
val BOT_TOKEN = env("TOKEN")

/** The string for connection to the database, defaults to local host. */
val MONGO_URI = envOrNull("MONGO_URI") ?: "mongodb://localhost:27017"

/** Sentry connection DSN. If null, Sentry won't be used. */
val SENTRY_DSN = envOrNull("SENTRY_DSN")
