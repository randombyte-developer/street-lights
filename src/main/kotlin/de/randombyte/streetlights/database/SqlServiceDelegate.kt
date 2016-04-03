package de.randombyte.streetlights.database

import org.spongepowered.api.Sponge
import org.spongepowered.api.service.sql.SqlService
import kotlin.reflect.KProperty

class SqlServiceDelegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = Sponge.getServiceManager().provide(SqlService::class.java).get()
}