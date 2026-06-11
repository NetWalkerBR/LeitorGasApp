package com.ildefrance.gasleitor.util

object ApartmentHelper {

    // Generates apartments from floor 11 down to floor 1
    // Each floor has 4 apartments: X4, X3, X2, X1 (e.g. 114, 113, 112, 111)
    fun getAllApartments(): List<Pair<String, Int>> {
        val list = mutableListOf<Pair<String, Int>>()
        for (floor in 11 downTo 1) {
            for (unit in 4 downTo 1) {
                val aptNumber = "${floor}${unit}"
                list.add(Pair(aptNumber, floor))
            }
        }
        return list
    }

    fun getTotalCount(): Int = 44
}
