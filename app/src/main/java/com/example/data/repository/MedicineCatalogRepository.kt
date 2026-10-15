package com.example.data.repository

import android.content.Context
import com.example.data.model.MedicineSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MedicineCatalogRepository(private val context: Context) {
    private var cachedList: List<MedicineSuggestion>? = null

    suspend fun getMedicineCatalog(): List<MedicineSuggestion> = withContext(Dispatchers.IO) {
        cachedList?.let { return@withContext it }

        val list = mutableListOf<MedicineSuggestion>()
        try {
            val jsonString = context.assets.open("bd_medicines.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MedicineSuggestion(
                        name = obj.getString("name"),
                        generic = obj.optString("generic", ""),
                        company = obj.optString("company", ""),
                        form = obj.optString("form", ""),
                        defaultDose = obj.optString("defaultDose", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedList = list
        list
    }

    suspend fun searchMedicines(query: String): List<MedicineSuggestion> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext emptyList()
        val all = getMedicineCatalog()
        val cleanQuery = query.trim().lowercase()
        all.filter {
            it.name.lowercase().contains(cleanQuery) ||
                it.generic.lowercase().contains(cleanQuery) ||
                it.company.lowercase().contains(cleanQuery)
        }
    }
}
