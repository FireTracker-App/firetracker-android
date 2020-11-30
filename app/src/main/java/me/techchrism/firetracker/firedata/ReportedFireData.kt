package me.techchrism.firetracker.firedata

import java.util.*

data class ReportedFireData (
    override var uniqueID: UUID,
    override val latitude: Double,
    override val longitude: Double,
    override val description: String?,
    val reported: Date,
    val canRemove: Boolean,
    val internalID: String
) : FireData