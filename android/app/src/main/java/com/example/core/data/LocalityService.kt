package com.example.core.data

import com.example.core.model.Locality

object LocalityService {
    val POPULAR_LOCALITIES = listOf(
        Locality("560034", "Koramangala", "Bengaluru", "Karnataka", activeGroupsCount = 14, activeDemandsCount = 28),
        Locality("560102", "HSR Layout", "Bengaluru", "Karnataka", activeGroupsCount = 19, activeDemandsCount = 35),
        Locality("560038", "Indiranagar", "Bengaluru", "Karnataka", activeGroupsCount = 11, activeDemandsCount = 22),
        Locality("560100", "Electronic City", "Bengaluru", "Karnataka", activeGroupsCount = 16, activeDemandsCount = 31),
        Locality("560066", "Whitefield", "Bengaluru", "Karnataka", activeGroupsCount = 22, activeDemandsCount = 40),
        Locality("110001", "Connaught Place", "New Delhi", "Delhi", activeGroupsCount = 8, activeDemandsCount = 19),
        Locality("122002", "DLF Cyber City", "Gurugram", "Haryana", activeGroupsCount = 15, activeDemandsCount = 29),
        Locality("201301", "Sector 18", "Noida", "Uttar Pradesh", activeGroupsCount = 12, activeDemandsCount = 24),
        Locality("400050", "Bandra West", "Mumbai", "Maharashtra", activeGroupsCount = 18, activeDemandsCount = 37),
        Locality("400076", "Powai", "Mumbai", "Maharashtra", activeGroupsCount = 20, activeDemandsCount = 42),
        Locality("400053", "Andheri West", "Mumbai", "Maharashtra", activeGroupsCount = 14, activeDemandsCount = 26),
        Locality("500081", "HITEC City", "Hyderabad", "Telangana", activeGroupsCount = 17, activeDemandsCount = 33),
        Locality("500032", "Gachibowli", "Hyderabad", "Telangana", activeGroupsCount = 13, activeDemandsCount = 25),
        Locality("411057", "Hinjewadi", "Pune", "Maharashtra", activeGroupsCount = 15, activeDemandsCount = 30),
        Locality("600096", "Perungudi OMR", "Chennai", "Tamil Nadu", activeGroupsCount = 10, activeDemandsCount = 21)
    )

    fun lookupPincode(pincode: String): Locality? {
        val trimmed = pincode.trim()
        val found = POPULAR_LOCALITIES.firstOrNull { it.pincode == trimmed }
        if (found != null) return found

        // Dynamic fallback for any valid 6-digit Indian pincode
        if (trimmed.length == 6 && trimmed.all { it.isDigit() }) {
            val prefix = trimmed.substring(0, 2)
            val (city, state) = when (prefix) {
                "11" -> "Delhi" to "Delhi"
                "12", "13" -> "Gurugram" to "Haryana"
                "20", "21" -> "Noida" to "Uttar Pradesh"
                "40" -> "Mumbai" to "Maharashtra"
                "41" -> "Pune" to "Maharashtra"
                "50" -> "Hyderabad" to "Telangana"
                "56", "57" -> "Bengaluru" to "Karnataka"
                "60", "61" -> "Chennai" to "Tamil Nadu"
                "70" -> "Kolkata" to "West Bengal"
                else -> "Metro City" to "India"
            }
            return Locality(
                pincode = trimmed,
                localityName = "Sector / Ward $trimmed",
                city = city,
                state = state,
                activeGroupsCount = (5..18).random(),
                activeDemandsCount = (10..30).random()
            )
        }
        return null
    }

    fun searchLocalities(query: String): List<Locality> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return POPULAR_LOCALITIES
        return POPULAR_LOCALITIES.filter {
            it.localityName.lowercase().contains(q) ||
            it.city.lowercase().contains(q) ||
            it.pincode.contains(q) ||
            it.state.lowercase().contains(q)
        }
    }
}
