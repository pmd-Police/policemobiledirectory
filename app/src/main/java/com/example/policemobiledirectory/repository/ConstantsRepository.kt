package com.example.policemobiledirectory.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.api.ConstantsApiService
import com.example.policemobiledirectory.api.ConstantsData
import com.example.policemobiledirectory.utils.SecurityConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

/**
 * ConstantsRepository - Manages dynamic constants synchronization
 * 
 * Features:
 * - Fetches constants from Google Sheets via Apps Script API
 * - Caches constants locally in SharedPreferences
 * - 30-day cache expiration (configurable)
 * - Automatic fallback to hardcoded Constants if cache is empty or expired
 * - Non-blocking background refresh
 */
@Singleton
class ConstantsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ConstantsApiService,
    private val securityConfig: SecurityConfig
) {

    private val prefs = context.getSharedPreferences("constants_cache", Context.MODE_PRIVATE)
    
    // Cache expiration: 30 days in milliseconds
    private val CACHE_EXPIRY_MS = TimeUnit.DAYS.toMillis(30)
    private val CACHE_KEY = "remote_constants"
    private val CACHE_TIMESTAMP_KEY = "cache_timestamp"

    /**
     * Check if cache needs refresh (expired or doesn't exist)
     */
    fun shouldRefreshCache(): Boolean {
        val timestamp = prefs.getLong(CACHE_TIMESTAMP_KEY, 0)
        if (timestamp == 0L) return true // No cache exists
        
        val now = System.currentTimeMillis()
        val age = now - timestamp
        return age >= CACHE_EXPIRY_MS
    }

    /**
     * Clear the cache (force next refresh to fetch from API)
     */
    fun clearCache() {
        prefs.edit()
            .remove(CACHE_KEY)
            .remove(CACHE_TIMESTAMP_KEY)
            .apply()
        Log.d("ConstantsRepository", "✅ Cache cleared - next refresh will fetch from API")
    }

    /**
     * Fetch remote constants and cache locally
     * Returns true if successful, false otherwise
     * Checks version and shows Toast if server version doesn't match local version
     */
    suspend fun refreshConstants(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConstants(token = securityConfig.getSecretToken())
            
            if (response.success && response.data != null) {
                // Check version
                val serverVersion = response.data.version
                if (serverVersion != Constants.LOCAL_CONSTANTS_VERSION) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "New constant update available. Please Sync.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Log.d("ConstantsRepository", "⚠️ Version mismatch: Server=$serverVersion, Local=${Constants.LOCAL_CONSTANTS_VERSION}")
                }
                
                val json = Gson().toJson(response.data)
                val now = System.currentTimeMillis()
                
                prefs.edit()
                    .putString(CACHE_KEY, json)
                    .putLong(CACHE_TIMESTAMP_KEY, now)
                    .apply()
                
                Log.d("ConstantsRepository", "✅ Constants refreshed from Google Sheet. Last updated: ${response.data.lastupdated}")
                Log.d("ConstantsRepository", "Stations count by district: ${response.data.stationsbydistrict.mapValues { it.value.size }}")
                true
            } else {
                Log.e("ConstantsRepository", "❌ API returned success=false or null data")
                false
            }
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch constants: ${e.message}", e)
            false
        }
    }
    
    /**
     * Fetch constants from API (alias for refreshConstants with explicit version checking)
     * This method is kept for backward compatibility and explicit version checking
     */
    suspend fun fetchConstants(): ConstantsData? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConstants(token = securityConfig.getSecretToken())
            
            if (response.success && response.data != null) {
                val serverVersion = response.data.version
                if (serverVersion != Constants.LOCAL_CONSTANTS_VERSION) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "New constant update available. Please Sync.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                response.data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch constants: ${e.message}", e)
            null
        }
    }

    /**
     * Get cached constants data, or null if cache is empty/invalid
     */
    private fun getCachedData(): ConstantsData? {
        val json = prefs.getString(CACHE_KEY, null)
        if (json.isNullOrEmpty()) return null

        return try {
            Gson().fromJson(json, ConstantsData::class.java)
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "Failed to parse cached constants", e)
            null
        }
    }

    /**
     * Get ranks with fallback to hardcoded constants
     */
    fun getRanks(): List<String> {
        val cached = getCachedData()
        return cached?.ranks?.sorted() ?: Constants.allRanksList
    }

    /**
     * Get districts with fallback to hardcoded constants
     * Always returns districts in the same order as Constants.districtsList for consistency
     */
    fun getDistricts(): List<String> {
        val cached = getCachedData()
        val apiDistricts = cached?.districts?.sorted() ?: emptyList()
        // Always start with hardcoded districts and merge API districts
        val hardcodedDistricts = Constants.districtsList
        val mergedDistricts = (hardcodedDistricts + apiDistricts).distinct().sorted()
        return mergedDistricts
    }

    /**
     * Get stations by district with fallback to hardcoded constants
     * ✅ CRITICAL FIX: Always use hardcoded stations (which includes all common units) as the base.
     * If API/cached data exists, merge it with hardcoded to add any new stations, but always keep
     * all hardcoded stations (including common units) in the result.
     * ✅ FIXED: Case-insensitive district matching to handle mismatches between API and hardcoded names.
     */
    fun getStationsByDistrict(): Map<String, List<String>> {
        val cached = getCachedData()
        val hardcodedStations = Constants.stationsByDistrictMap
        
        // DEBUG: Verify Chikkaballapura exists in hardcoded map
        val chikkaInHardcoded = hardcodedStations.containsKey("Chikkaballapura")
        val chikkaStationsCount = hardcodedStations["Chikkaballapura"]?.size ?: 0
        val chikkaStationsList = hardcodedStations["Chikkaballapura"] ?: emptyList()
        
        Log.d("ConstantsRepository", "🔍 DEBUG: Chikkaballapura in hardcoded map: $chikkaInHardcoded, Stations: $chikkaStationsCount")
        if (!chikkaInHardcoded) {
            Log.e("ConstantsRepository", "❌ CRITICAL: Chikkaballapura NOT in Constants.stationsByDistrictMap!")
            Log.e("ConstantsRepository", "   Available keys (first 10): ${hardcodedStations.keys.sorted().take(10).joinToString(", ")}")
            // Try to find it case-insensitively
            val caseInsensitiveMatch = hardcodedStations.keys.find { it.equals("Chikkaballapura", ignoreCase = true) }
            Log.e("ConstantsRepository", "   Case-insensitive match: $caseInsensitiveMatch")
        } else {
            Log.d("ConstantsRepository", "✅ Chikkaballapura found in hardcoded! First 5 stations: ${chikkaStationsList.take(5).joinToString(", ")}")
        }
        
        // Verify Constants.districtsList contains Chikkaballapura
        val inDistrictsList = Constants.districtsList.contains("Chikkaballapura")
        Log.d("ConstantsRepository", "🔍 Chikkaballapura in Constants.districtsList: $inDistrictsList")
        if (!inDistrictsList) {
            val caseInsensitiveInList = Constants.districtsList.find { it.equals("Chikkaballapura", ignoreCase = true) }
            Log.e("ConstantsRepository", "❌ Chikkaballapura NOT in districtsList! Case-insensitive match: $caseInsensitiveInList")
        }
        
        // Create case-insensitive lookup for districts
        val districtLookup = hardcodedStations.keys.associateBy { it.lowercase() }
        
        // Always start with hardcoded stations as base (includes all common units for all districts)
        val resultMap = hardcodedStations.mapValues { (district, hardcodedList) ->
            // For each district, start with hardcoded list (includes common units)
            val baseStations = hardcodedList.toMutableList()
            
            // Find matching API district (case-insensitive)
            val matchingApiDistrict = cached?.stationsbydistrict?.keys?.find { 
                it.lowercase().trim() == district.lowercase().trim() 
            }
            
            // If API has data for this district, merge it in
            val apiStations = matchingApiDistrict?.let { 
                val stations = cached.stationsbydistrict[it] ?: emptyList()
                Log.d("ConstantsRepository", "District: $district -> API match: $it, Stations count: ${stations.size}")
                stations
            } ?: emptyList()
            
            if (apiStations.isNotEmpty()) {
                // Add API stations that aren't already in hardcoded list (case-insensitive check)
                apiStations.forEach { apiStation ->
                    val exists = baseStations.any { 
                        it.lowercase().trim() == apiStation.lowercase().trim() 
                    }
                    if (!exists) {
                        baseStations.add(apiStation)
                    }
                }
            } else if (cached != null) {
                // Debug: Check if district exists in API with different spelling
                val apiDistrictKeys = cached.stationsbydistrict.keys
                val possibleMatches = apiDistrictKeys.filter { 
                    it.lowercase().trim().contains(district.lowercase().trim()) || 
                    district.lowercase().trim().contains(it.lowercase().trim())
                }
                if (possibleMatches.isNotEmpty()) {
                    Log.w("ConstantsRepository", "⚠️ District '$district' not found in API, but found possible matches: $possibleMatches")
                }
            }
            
            // Log for Chikkaballapura specifically
            if (district.equals("Chikkaballapura", ignoreCase = true)) {
                Log.d("ConstantsRepository", "🔍 Chikkaballapura stations: ${baseStations.size} hardcoded, ${apiStations.size} from API, Final: ${(baseStations + district).distinct().sorted().size}")
            }
            
            // Ensure district name is included, remove duplicates, sort
            (baseStations + district).distinct().sorted()
        }.toMutableMap()
        
        // Also handle any districts that exist in API but not in hardcoded (case-insensitive check)
        cached?.stationsbydistrict?.forEach { (apiDistrict, apiStations) ->
            val exists = resultMap.keys.any { 
                it.lowercase().trim() == apiDistrict.lowercase().trim() 
            }
            if (!exists) {
                // District exists in API but not in hardcoded - use API data + district name
                resultMap[apiDistrict] = (apiStations + apiDistrict).distinct().sorted()
                Log.d("ConstantsRepository", "Added new district from API: $apiDistrict")
            }
        }
        
        // Final verification: Ensure all hardcoded districts have stations
        hardcodedStations.keys.forEach { district ->
            if (!resultMap.containsKey(district) || resultMap[district].isNullOrEmpty()) {
                Log.w("ConstantsRepository", "⚠️ WARNING: District '$district' has no stations in result! Using hardcoded fallback.")
                val fallbackStations = (hardcodedStations[district]?.toList() ?: emptyList<String>()) + district
                resultMap[district] = fallbackStations.distinct().sorted()
            }
        }
        
        // CRITICAL FIX: Normalize keys to match exactly with Constants.districtsList
        // Use districtsList as the source of truth for exact district names
        val normalizedMap = mutableMapOf<String, List<String>>()
        Constants.districtsList.forEach { exactDistrictName ->
            // Find matching entry in resultMap (case-insensitive)
            val matchingKey = resultMap.keys.find { it.equals(exactDistrictName, ignoreCase = true) }
            val stations = matchingKey?.let { resultMap[it] } 
                ?: (hardcodedStations[exactDistrictName]?.toList() ?: emptyList()) + exactDistrictName
            
            // Ensure we always have stations (at least district name + hardcoded stations)
            normalizedMap[exactDistrictName] = stations.distinct().sorted()
        }
        
        // Debug log for Chikkaballapura - verify it exists
        val chikkaKey = normalizedMap.keys.find { it.equals("Chikkaballapura", ignoreCase = true) }
        if (chikkaKey != null) {
            normalizedMap[chikkaKey]?.let { stations ->
                Log.d("ConstantsRepository", "✅ Chikkaballapura found! Key: '$chikkaKey', Stations count: ${stations.size}")
                Log.d("ConstantsRepository", "   First 5 stations: ${stations.take(5).joinToString(", ")}")
            }
        } else {
            Log.e("ConstantsRepository", "❌ ERROR: Chikkaballapura NOT found in normalized map!")
            Log.e("ConstantsRepository", "   Available keys: ${normalizedMap.keys.sorted().joinToString(", ")}")
            // Force add Chikkaballapura from hardcoded
            val fallbackStations = (hardcodedStations["Chikkaballapura"]?.toList() ?: emptyList()) + "Chikkaballapura"
            normalizedMap["Chikkaballapura"] = fallbackStations.distinct().sorted()
            Log.w("ConstantsRepository", "⚠️ Added Chikkaballapura as fallback with ${fallbackStations.size} stations")
        }
        
        // Verify all districts from Constants.districtsList are in the map
        Constants.districtsList.forEach { district ->
            if (!normalizedMap.containsKey(district)) {
                Log.e("ConstantsRepository", "❌ CRITICAL: District '$district' from Constants.districtsList missing from map!")
            }
        }
        
        Log.d("ConstantsRepository", "📋 Final map has ${normalizedMap.size} districts")
        
        return normalizedMap
    }

    /**
     * Get blood groups with fallback to hardcoded constants
     */
    fun getBloodGroups(): List<String> {
        val cached = getCachedData()
        return cached?.bloodgroups?.sorted() ?: Constants.bloodGroupsList
    }

    /**
     * Get ranks requiring metal number (still uses hardcoded list)
     * This could be extended to support dynamic configuration in the future
     */
    fun getRanksRequiringMetalNumber(): List<String> {
        return Constants.ranksRequiringMetalNumber
    }

    /**
     * Get cache age in days, or null if no cache exists
     */
    fun getCacheAgeDays(): Long? {
        val timestamp = prefs.getLong(CACHE_TIMESTAMP_KEY, 0)
        if (timestamp == 0L) return null
        
        val age = System.currentTimeMillis() - timestamp
        return TimeUnit.MILLISECONDS.toDays(age)
    }
}
