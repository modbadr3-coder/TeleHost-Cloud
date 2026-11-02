package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TeleHostRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createBot_validPayload_createsAndEmitsInRealtimeFlow() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = TeleHostRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createBot(
                name = "Cloud Control Bot",
                username = "cloud_control_bot",
                botToken = "123456789:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw",
                welcomeMessage = "اهلا بك في الخادم"
            )
        }
        assertTrue(createResult.isSuccess)
        val botId = createResult.getOrThrow()
        assertTrue(botId.isNotEmpty())

        val bots = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeBots(aliceUid).first { list -> list.any { it.id == botId } }
        }
        assertTrue(bots.any { it.id == botId && it.name == "Cloud Control Bot" })
    }

    @Test
    fun getBotById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = TeleHostRepository(firestore, auth)
        val botId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createBot(
                name = "Alice Private Bot",
                username = "alice_priv_bot",
                botToken = "987654321:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw",
                welcomeMessage = "رسالة ترحيب"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = TeleHostRepository(firestore, auth)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getBotById(botId, targetOwnerUid = aliceUid)
        }
        assertTrue(crossResult.isFailure)
        val ex = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(ex)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun observeBots_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = TeleHostRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeBots(aliceUid).first()
            }
            fail("Expected exception when observing bots unauthenticated")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun createAndUpdateSiteProject_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = TeleHostRepository(firestore, auth)

        val siteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createSiteProject(
                projectName = "Al-Noor Tech",
                subdomainSlug = "alnoor-tech",
                provider = "paste_gateway",
                htmlContent = "<html><body><h1>Al-Noor</h1></body></html>",
                cssContent = "body { color: white; }",
                jsContent = "console.log('ready');"
            ).getOrThrow()
        }

        val emittedSite = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSites(aliceUid).first { list -> list.any { it.id == siteId } }
                .first { it.id == siteId }
        }

        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateSiteProject(
                emittedSite.copy(
                    buildStatus = "deployed",
                    liveDomainUrl = "https://dpaste.com/EXAMPLE.txt"
                )
            )
        }
        assertTrue(updateRes.isSuccess)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@telehost.test"
        const val BOB_EMAIL = "bob@telehost.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
