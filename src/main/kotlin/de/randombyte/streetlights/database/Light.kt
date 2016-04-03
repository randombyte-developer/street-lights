package de.randombyte.streetlights.database

import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World

data class Light(val id: Int, val location: Location<World>)