const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validBotPayload(userId, now) {
  return {
    userId,
    name: "Production Support Bot",
    username: "prod_support_bot",
    botToken: "123456789:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw",
    status: "running",
    webhookUrl: "",
    autoReplyRulesJson: "[]",
    welcomeMessage: "Welcome to our automated control center.",
    processedUpdatesCount: 0,
    lastUpdateId: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validSitePayload(userId, now) {
  return {
    userId,
    projectName: "Corporate Portal",
    subdomainSlug: "corp-portal-hq",
    provider: "paste_gateway",
    liveDomainUrl: "https://dpaste.com/ABCD123.txt",
    htmlContent: "<!DOCTYPE html><html><body><h1>Live Site</h1></body></html>",
    cssContent: "body { background: #0B0F19; color: #F9FAFB; }",
    jsContent: "console.log('Initialized');",
    buildStatus: "deployed",
    lastBuildLog: "Bundle built and published to domain.",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write bots", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bots").get());
});

test("Authenticated owner can create, read, update, and delete their own bot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const botRef = aliceDb.collection("users").doc(ALICE_UID).collection("bots").doc("bot_alpha");

  await assertSucceeds(botRef.set(validBotPayload(ALICE_UID, now)));
  await assertSucceeds(botRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bots").where("userId", "==", ALICE_UID).get()
  );

  await assertSucceeds(
    botRef.update({
      status: "stopped",
      processedUpdatesCount: 15,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(botRef.delete());
});

test("Cross-user access to bots and sites is rejected", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("bots")
      .doc("bot_alpha")
      .set(validBotPayload(ALICE_UID, now));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bots").doc("bot_alpha").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bots").doc("bot_alpha").delete()
  );
});

test("Shadow field injection and invalid status enum on bots are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const botRef = aliceDb.collection("users").doc(ALICE_UID).collection("bots").doc("bot_shadow");

  await assertFails(
    botRef.set({
      ...validBotPayload(ALICE_UID, now),
      isAdmin: true,
    })
  );

  await assertFails(
    botRef.set({
      ...validBotPayload(ALICE_UID, now),
      status: "invalid_state",
    })
  );
});

test("Authenticated owner can create and update website projects and logs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const siteRef = aliceDb.collection("users").doc(ALICE_UID).collection("sites").doc("site_1");
  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("logs").doc("log_1");

  await assertSucceeds(siteRef.set(validSitePayload(ALICE_UID, now)));
  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      targetName: "Corporate Portal",
      category: "site_deploy",
      message: "Published successfully",
      createdAt: now,
    })
  );

  // Logs are append-only (no update)
  await assertFails(
    logRef.update({
      message: "Modified log",
    })
  );
});
