package com.example.policemobiledirectory.utils

import com.example.policemobiledirectory.model.Employee

// Function to filter employees
fun filterEmployees(
    employees: List<Employee>,
    query: String,
    field: SearchFilter
): List<Employee> {
    if (query.isBlank()) return employees

    val lowerQuery = query.lowercase()

    return employees.filter { emp ->
        when (field) {
            SearchFilter.ALL -> {
                emp.name.lowercase().contains(lowerQuery) ||
                emp.kgid.lowercase().contains(lowerQuery) ||
                emp.station?.lowercase()?.contains(lowerQuery) == true ||
                emp.rank?.lowercase()?.contains(lowerQuery) == true ||
                emp.mobile1?.lowercase()?.contains(lowerQuery) == true ||
                emp.mobile2?.lowercase()?.contains(lowerQuery) == true
            }
            SearchFilter.NAME -> emp.name.lowercase().contains(lowerQuery)
            SearchFilter.KGID -> emp.kgid.lowercase().contains(lowerQuery)
            SearchFilter.STATION -> emp.station?.lowercase()?.contains(lowerQuery) == true
            SearchFilter.RANK -> emp.rank?.lowercase()?.contains(lowerQuery) == true
            SearchFilter.MOBILE -> {
                emp.mobile1?.lowercase()?.contains(lowerQuery) == true ||
                emp.mobile2?.lowercase()?.contains(lowerQuery) == true
            }
        }
    }
}
