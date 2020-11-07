package me.techchrism.firetracker.firedata

import java.util.*

data class CalFireData (
    override var uniqueID: UUID,
    override val latitude: Double,
    override val longitude: Double,
    val name: String,
    val location: String,
    val active: Boolean,
    val started: Date,
    val percentContained: Int?,
    val acresBurned: Int?,
    val searchDescription: String
) : FireData