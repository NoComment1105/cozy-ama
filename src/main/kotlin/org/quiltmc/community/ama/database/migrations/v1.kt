/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.ama.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.quiltmc.community.ama.database.collections.AmaConfigCollection

suspend fun v1(db: CoroutineDatabase) {
	db.createCollection(AmaConfigCollection.name)
}
