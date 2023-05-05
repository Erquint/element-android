/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingKeyRequestEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

/**
 * This migration make OutgoingKeyRequestEntity.requestStateStr required. Seems to be due to the upgrade to Kotlin 1.8.21.
 */
internal class MigrateCryptoTo022(realm: DynamicRealm) : RealmMigrator(realm, 22) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("OutgoingKeyRequestEntity")
                ?.setRequired(OutgoingKeyRequestEntityFields.REQUEST_STATE_STR, true)
    }
}
