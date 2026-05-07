package ru.homebuhg.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class FirebaseHolder(
    val auth: FirebaseAuth?,
    val firestore: FirebaseFirestore?
) {
    val isAvailable: Boolean get() = auth != null && firestore != null
}
