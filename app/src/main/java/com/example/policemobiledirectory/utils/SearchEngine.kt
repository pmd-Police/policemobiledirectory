package com.example.policemobiledirectory.utils

import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer

/**
 * Enhanced search engine with relevance scoring and weighted matching
 */
object SearchEngine {
    
    /**
     * Search result with relevance score
     */
    data class SearchResult<T>(
        val item: T,
        val score: Double,
        val matchedFields: List<String> = emptyList()
    ) {
        val isExactMatch: Boolean get() = score >= 1.0
        val isHighRelevance: Boolean get() = score >= 0.7
    }
    
    /**
     * Field weights for relevance scoring
     */
    private object FieldWeights {
        const val EXACT_MATCH = 1.0
        const val STARTS_WITH = 0.8
        const val CONTAINS = 0.5
        const val FUZZY_MATCH = 0.3
        
        // Field-specific weights
        const val NAME_EXACT = 1.0
        const val NAME_STARTS = 0.9
        const val ID_EXACT = 0.95
        const val ID_STARTS = 0.85
        const val MOBILE_EXACT = 0.9
        const val RANK_EXACT = 0.7
        const val STATION_EXACT = 0.6
        const val DISTRICT_EXACT = 0.5
    }
    
    /**
     * Search employees with relevance scoring
     */
    fun searchEmployees(
        employees: List<Employee>,
        query: String,
        filter: SearchFilter,
        limit: Int = 100
    ): List<SearchResult<Employee>> {
        if (query.isBlank()) return employees.map { SearchResult(it, 1.0) }
        
        val queryLower = query.lowercase().trim()
        val results = employees.mapNotNull { employee ->
            val score = calculateEmployeeScore(employee, queryLower, filter)
            if (score > 0) {
                val matchedFields = getMatchedFields(employee, queryLower, filter)
                SearchResult(employee, score, matchedFields)
            } else null
        }
        
        // Sort by score (descending) and limit results
        return results.sortedByDescending { it.score }.take(limit)
    }
    
    /**
     * Search officers with relevance scoring
     */
    fun searchOfficers(
        officers: List<Officer>,
        query: String,
        filter: String = "all",
        limit: Int = 100
    ): List<SearchResult<Officer>> {
        if (query.isBlank()) return officers.map { SearchResult(it, 1.0) }
        
        val queryLower = query.lowercase().trim()
        val results = officers.mapNotNull { officer ->
            val score = calculateOfficerScore(officer, queryLower, filter)
            if (score > 0) {
                val matchedFields = getMatchedOfficerFields(officer, queryLower, filter)
                SearchResult(officer, score, matchedFields)
            } else null
        }
        
        // Sort by score (descending) and limit results
        return results.sortedByDescending { it.score }.take(limit)
    }
    
    /**
     * Calculate relevance score for employee
     */
    private fun calculateEmployeeScore(
        employee: Employee,
        queryLower: String,
        filter: SearchFilter
    ): Double {
        return when (filter) {
            SearchFilter.NAME -> calculateNameScore(employee.name, queryLower) * FieldWeights.NAME_EXACT
            SearchFilter.KGID -> calculateIdScore(employee.kgid, queryLower) * FieldWeights.ID_EXACT
            SearchFilter.MOBILE -> calculateMobileScore(
                employee.mobile1,
                employee.mobile2,
                queryLower
            ) * FieldWeights.MOBILE_EXACT
            SearchFilter.RANK -> calculateFieldScore(employee.rank, queryLower) * FieldWeights.RANK_EXACT
            SearchFilter.STATION -> calculateFieldScore(employee.station, queryLower) * FieldWeights.STATION_EXACT
            SearchFilter.METAL_NUMBER -> calculateFieldScore(employee.metalNumber, queryLower) * 0.6
        }
    }
    
    /**
     * Calculate relevance score for officer
     */
    private fun calculateOfficerScore(
        officer: Officer,
        queryLower: String,
        filter: String
    ): Double {
        return when (filter.lowercase()) {
            "name" -> calculateNameScore(officer.name, queryLower) * FieldWeights.NAME_EXACT
            "agid" -> calculateIdScore(officer.agid, queryLower) * FieldWeights.ID_EXACT
            "mobile" -> calculateMobileScore(
                officer.mobile,
                officer.landline,
                queryLower
            ) * FieldWeights.MOBILE_EXACT
            "rank" -> calculateFieldScore(officer.rank, queryLower) * FieldWeights.RANK_EXACT
            "station" -> calculateFieldScore(officer.station, queryLower) * FieldWeights.STATION_EXACT
            "district" -> calculateFieldScore(officer.district, queryLower) * FieldWeights.DISTRICT_EXACT
            else -> {
                // Multi-field search - combine scores
                val nameScore = calculateNameScore(officer.name, queryLower) * FieldWeights.NAME_EXACT
                val idScore = calculateIdScore(officer.agid, queryLower) * FieldWeights.ID_EXACT
                val mobileScore = calculateMobileScore(officer.mobile, officer.landline, queryLower) * FieldWeights.MOBILE_EXACT
                val rankScore = calculateFieldScore(officer.rank, queryLower) * FieldWeights.RANK_EXACT
                maxOf(nameScore, idScore, mobileScore, rankScore)
            }
        }
    }
    
    /**
     * Calculate name score (highest weight for names)
     */
    private fun calculateNameScore(name: String, queryLower: String): Double {
        val nameLower = name.lowercase()
        return when {
            nameLower == queryLower -> FieldWeights.EXACT_MATCH
            nameLower.startsWith(queryLower) -> FieldWeights.STARTS_WITH
            nameLower.contains(queryLower) -> FieldWeights.CONTAINS
            else -> 0.0
        }
    }
    
    /**
     * Calculate ID score
     */
    private fun calculateIdScore(id: String, queryLower: String): Double {
        val idLower = id.lowercase()
        return when {
            idLower == queryLower -> FieldWeights.EXACT_MATCH
            idLower.startsWith(queryLower) -> FieldWeights.STARTS_WITH
            idLower.contains(queryLower) -> FieldWeights.CONTAINS
            else -> 0.0
        }
    }
    
    /**
     * Calculate mobile score
     */
    private fun calculateMobileScore(mobile1: String?, mobile2: String?, queryLower: String): Double {
        val mobile1Score = mobile1?.let { calculateFieldScore(it, queryLower) } ?: 0.0
        val mobile2Score = mobile2?.let { calculateFieldScore(it, queryLower) } ?: 0.0
        return maxOf(mobile1Score, mobile2Score)
    }
    
    /**
     * Calculate generic field score
     */
    private fun calculateFieldScore(field: String?, queryLower: String): Double {
        if (field == null) return 0.0
        val fieldLower = field.lowercase()
        return when {
            fieldLower == queryLower -> FieldWeights.EXACT_MATCH
            fieldLower.startsWith(queryLower) -> FieldWeights.STARTS_WITH
            fieldLower.contains(queryLower) -> FieldWeights.CONTAINS
            else -> 0.0
        }
    }
    
    /**
     * Get matched fields for employee
     */
    private fun getMatchedFields(employee: Employee, queryLower: String, filter: SearchFilter): List<String> {
        val matched = mutableListOf<String>()
        
        when (filter) {
            SearchFilter.NAME -> if (employee.name.lowercase().contains(queryLower)) matched.add("name")
            SearchFilter.KGID -> if (employee.kgid.lowercase().contains(queryLower)) matched.add("kgid")
            SearchFilter.MOBILE -> {
                if (employee.mobile1?.contains(queryLower) == true) matched.add("mobile1")
                if (employee.mobile2?.contains(queryLower) == true) matched.add("mobile2")
            }
            SearchFilter.RANK -> if (employee.rank?.lowercase()?.contains(queryLower) == true) matched.add("rank")
            SearchFilter.STATION -> if (employee.station?.lowercase()?.contains(queryLower) == true) matched.add("station")
            SearchFilter.METAL_NUMBER -> if (employee.metalNumber?.lowercase()?.contains(queryLower) == true) matched.add("metalNumber")
        }
        
        return matched
    }
    
    /**
     * Get matched fields for officer
     */
    private fun getMatchedOfficerFields(officer: Officer, queryLower: String, filter: String): List<String> {
        val matched = mutableListOf<String>()
        
        when (filter.lowercase()) {
            "name" -> if (officer.name.lowercase().contains(queryLower)) matched.add("name")
            "agid" -> if (officer.agid.lowercase().contains(queryLower)) matched.add("agid")
            "mobile" -> {
                if (officer.mobile?.contains(queryLower) == true) matched.add("mobile")
                if (officer.landline?.contains(queryLower) == true) matched.add("landline")
            }
            "rank" -> if (officer.rank?.lowercase()?.contains(queryLower) == true) matched.add("rank")
            "station" -> if (officer.station?.lowercase()?.contains(queryLower) == true) matched.add("station")
            "district" -> if (officer.district?.lowercase()?.contains(queryLower) == true) matched.add("district")
            else -> {
                // Multi-field search
                if (officer.name.lowercase().contains(queryLower)) matched.add("name")
                if (officer.agid.lowercase().contains(queryLower)) matched.add("agid")
                if (officer.mobile?.contains(queryLower) == true) matched.add("mobile")
                if (officer.rank?.lowercase()?.contains(queryLower) == true) matched.add("rank")
            }
        }
        
        return matched
    }
    
    /**
     * Filter results by minimum relevance score
     */
    fun filterByRelevance(
        results: List<SearchResult<*>>,
        minScore: Double = 0.3
    ): List<SearchResult<*>> {
        return results.filter { it.score >= minScore }
    }
}



