package de.randombyte.streetlights.database

import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.spongepowered.api.Sponge
import org.spongepowered.api.block.BlockTypes
import org.spongepowered.api.world.Location
import java.util.*

/**
 * Internal representation of Light object in database.
 */
object Lights : Table() {
    val id = integer("id").primaryKey().autoIncrement()
    val worldUUID = varchar("world_uuid", 36)
    val x = integer("x")
    val y = integer("y")
    val z = integer("z")

    /**
     * Parses the Query to Light objects. Ignores Light objects from unloaded worlds.
     * @query Query the Lights will be parsed from
     * @return Parsed Light objects from given query mapped to id
     */
    fun fromQuery(query: Query): Array<Light> = query.map { row -> fromRow(row) }.filterNotNull().toTypedArray()

    /**
     * Parses a ResultRow from a Query to a Light object.
     * @row ResultRow the Light will be parsed from
     * @return Nullable Light object, is null when world it belongs to is not loaded
     */
    fun fromRow(row: ResultRow): Light? {
        val worldOpt = Sponge.getServer().getWorld(UUID.fromString(row[worldUUID]))
        return if (worldOpt.isPresent) Light(row[id], Location(worldOpt.get(), row[x], row[y], row[z])) else null //world not available
    }

    /**
     * Sets provided Lights to the specified state: ON/OFF.
     * @lights Array of Lights to power/unpower
     * @powered Whether the Lights should be on or off
     */
    fun setLightsState(lights: Array<Light>, powered: Boolean) =
            lights.forEach {
                val extent = it.location.extent
                val x = it.location.blockX
                val y = it.location.blockY
                val z = it.location.blockZ
                val newBlockState = if (powered) BlockTypes.LIT_REDSTONE_LAMP.defaultState else BlockTypes.REDSTONE_LAMP.defaultState
                extent.setBlock(x, y, z, newBlockState, true)
            }
}