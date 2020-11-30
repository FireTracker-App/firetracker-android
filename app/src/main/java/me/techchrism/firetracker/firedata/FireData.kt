package me.techchrism.firetracker.firedata

import java.util.*

interface FireData {
    var uniqueID: UUID
    val latitude: Double
    val longitude: Double
    val description: String?
}