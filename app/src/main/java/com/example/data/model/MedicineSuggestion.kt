package com.example.data.model

data class MedicineSuggestion(
    val name: String,
    val generic: String,
    val company: String,
    val form: String,
    val defaultDose: String
)

data class DoseItem(
    val doseLog: DoseLog,
    val medication: Medication
)
