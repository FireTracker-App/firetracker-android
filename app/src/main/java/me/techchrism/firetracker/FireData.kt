package me.techchrism.firetracker

import java.util.*

data class FireData (
    val uniqueID: UUID,
    val name: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val active: Boolean
)