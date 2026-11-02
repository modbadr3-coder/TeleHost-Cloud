package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DeploymentLog
import com.example.data.model.TelegramBot
import com.example.data.model.WebSiteProject
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TeleHostRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً للوصول إلى الخادم السحابي.")
    }

    fun observeBots(userId: String = requireUserId()): Flow<List<TelegramBot>> = flow {
        val path = "users/$userId/bots"
        emitAll(
            db.collection("users").document(userId).collection("bots")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TelegramBot::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createBot(
        name: String,
        username: String,
        botToken: String,
        welcomeMessage: String,
        autoReplyRulesJson: String = "[]"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val botsRef = db.collection("users").document(uid).collection("bots")
        val docRef = botsRef.document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().take(100),
            "username" to username.trim().take(100),
            "botToken" to botToken.trim().take(256),
            "status" to "stopped",
            "webhookUrl" to "",
            "autoReplyRulesJson" to autoReplyRulesJson.take(20000),
            "welcomeMessage" to welcomeMessage.trim().take(4096),
            "processedUpdatesCount" to 0L,
            "lastUpdateId" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getBotById(botId: String, targetOwnerUid: String = requireUserId()): Result<TelegramBot?> = runCatching {
        val docRef = db.collection("users").document(targetOwnerUid).collection("bots").document(botId)
        try {
            val snap = docRef.get().await()
            snap.toObject(TelegramBot::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateBot(bot: TelegramBot): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(bot.id.isNotBlank()) { "Bot ID must not be blank" }
        requireNotNull(bot.createdAt) { "Bot createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("bots").document(bot.id)
        try {
            docRef.set(bot.copy(userId = uid, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBot(botId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("bots").document(botId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeSites(userId: String = requireUserId()): Flow<List<WebSiteProject>> = flow {
        val path = "users/$userId/sites"
        emitAll(
            db.collection("users").document(userId).collection("sites")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WebSiteProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createSiteProject(
        projectName: String,
        subdomainSlug: String,
        provider: String,
        htmlContent: String,
        cssContent: String,
        jsContent: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val sitesRef = db.collection("users").document(uid).collection("sites")
        val docRef = sitesRef.document()
        val payload = mapOf(
            "userId" to uid,
            "projectName" to projectName.trim().take(100),
            "subdomainSlug" to subdomainSlug.trim().lowercase().take(100),
            "provider" to provider,
            "liveDomainUrl" to "",
            "htmlContent" to htmlContent.take(200000),
            "cssContent" to cssContent.take(150000),
            "jsContent" to jsContent.take(150000),
            "buildStatus" to "draft",
            "lastBuildLog" to "تم إنشاء المشروع في الخادم السحابي",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateSiteProject(site: WebSiteProject): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(site.id.isNotBlank()) { "Site ID must not be blank" }
        requireNotNull(site.createdAt) { "Site createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("sites").document(site.id)
        try {
            docRef.set(site.copy(userId = uid, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSiteProject(siteId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("sites").document(siteId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeLogs(userId: String = requireUserId()): Flow<List<DeploymentLog>> = flow {
        val path = "users/$userId/logs"
        emitAll(
            db.collection("users").document(userId).collection("logs")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DeploymentLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun appendLog(
        targetName: String,
        category: String,
        message: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val logsRef = db.collection("users").document(uid).collection("logs")
        val docRef = logsRef.document()
        val payload = mapOf(
            "userId" to uid,
            "targetName" to targetName.trim().take(120),
            "category" to category,
            "message" to message.trim().take(5000),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
