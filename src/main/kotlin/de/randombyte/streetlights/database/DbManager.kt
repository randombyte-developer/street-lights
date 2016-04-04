package de.randombyte.streetlights.database

import org.jetbrains.exposed.sql.*
import org.spongepowered.api.service.sql.SqlService
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World

object DbManager {

    var path = "."
    val sqlService: SqlService by SqlServiceDelegate()

    fun getDataSource() = sqlService.getDataSource("jdbc:h2:$path")

    fun Transaction.ensureExistence(table: Table) {
        if (!table.exists()) create(table)
    }

    /**
     * Gets all Lights from loaded worlds.
     * @return All Lights from loaded worlds
     */
    fun getAllLights(): Map<Int, Light> = Database.connect(getDataSource()).transaction {
            ensureExistence(Lights)
            Lights.fromQuery(Lights.selectAll()).associateBy { it.id }
        }

    /**
     * Gets all Lights from world with specified worldUuid.
     * @worldUuid String of UUID of world that should be queried
     * @return All Lights from provided world
     */
    fun getAllLights(worldUuid: String): Array<Light> = Database.connect(getDataSource()).transaction {
            ensureExistence(Lights)
            Lights.fromQuery(Lights.select { Lights.worldUUID eq worldUuid })
        }

    /**
     * Adds a Light to database.
     * @location Location where the Light is placed
     * @return The id the database assigned to it
     */
    fun addLight(location: Location<World>): Int {
        var id = -1
        Database.connect(getDataSource()).transaction {
            ensureExistence(Lights)
            id = Lights.insert {
                it[worldUUID] = location.extent.uniqueId.toString()
                it[x] = location.blockX
                it[y] = location.blockY
                it[z] = location.blockZ
            } get Lights.id
        }
        return id
    }

    /**
     * Gets the Light object from its Location.
     * @return Nullable Light object, may be null when there is no Light in given Location
     * @throws IllegalStateException when there are two Lights with same Location
     */
    fun getLight(location: Location<World>): Light? {
        return Database.connect(getDataSource()).transaction {
            ensureExistence(Lights)
            val results = Lights.fromQuery(Lights.select {
                Lights.worldUUID.eq(location.extent.uniqueId.toString()) and
                Lights.x.eq(location.blockX) and
                Lights.y.eq(location.blockY) and
                Lights.z.eq(location.blockZ)
            })
            when {
                results.size == 0 -> null
                results.size == 1 -> results[0]
                results.size >= 2 -> throw IllegalStateException("Corrupted database: More than one Light with same Location!")
                else -> throw IllegalStateException("Got negative result set size while querying database!")
            }
        }
    }

    fun removeLight(id: Int) {
        Database.connect(getDataSource()).transaction {
            Lights.deleteWhere { Lights.id eq id }
        }
    }

}