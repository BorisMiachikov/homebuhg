package ru.homebuhg.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.homebuhg.BuildConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseHolder(): FirebaseHolder {
        if (!BuildConfig.FIREBASE_ENABLED) return FirebaseHolder(null, null)
        val firestore = FirebaseFirestore.getInstance()
        return FirebaseHolder(
            auth = FirebaseAuth.getInstance(),
            firestore = firestore
        )
    }
}
