package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = try {
        Firebase.auth.currentUser
    } catch (_: Exception) {
        null
    }

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun friendlyFirestoreMessage(exception: Throwable, fallback: String): String {
    val causes = generateSequence(exception) { it.cause }
    val firestoreErr = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    val rawMsg = firestoreErr?.message ?: exception.localizedMessage.orEmpty()
    return when {
        rawMsg.contains("Quota exceeded", ignoreCase = true) ->
            "Cloud storage daily quota reached. Quota resets tomorrow, or export a JSON backup."
        firestoreErr?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Cloud sync permission denied. Please verify you are signed in with Google."
        firestoreErr?.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Offline — changes will sync automatically when you reconnect."
        else -> fallback
    }
}
