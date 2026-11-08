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

function validShiftPayload(userId, overrides = {}) {
  const now = new Date();
  return {
    userId,
    staffName: "Dra. Mariana Costa",
    roleTitle: "Médica Intensivista",
    crmOrRegistry: "CRM-SP 186.420",
    department: "UTI Adulto",
    category: "MEDICAL",
    shiftStatus: "CURRENT",
    shiftTimeLabel: "07:00 – 19:00 (12h)",
    dateLabel: "Hoje • Em Plantão",
    locationRoom: "Bloco A • Leitos 01 a 12",
    permissionLevel: "Acesso Clínico Plantonista",
    notes: "Plantão criado via comando de voz.",
    voiceTranscript: "Dra. Mariana Costa intensivista na UTI Adulto plantão hoje",
    isOnCall: true,
    createdViaVoice: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("Unauthenticated user: cannot read or create shifts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("shifts").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("shifts")
      .doc("shift_1")
      .set(validShiftPayload(ALICE_UID))
  );
});

test("Authenticated owner: can create, get, list, update, and delete their own shift", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("shifts")
    .doc("shift_alice_1");

  await assertSucceeds(docRef.set(validShiftPayload(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("shifts")
      .where("userId", "==", ALICE_UID)
      .get()
  );
  await assertSucceeds(
    docRef.update({
      shiftStatus: "COMPLETED",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(docRef.delete());
});

test("Cross-user isolation: Bob cannot read, update, or delete Alice's shift", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("shifts")
      .doc("shift_alice_1")
      .set(validShiftPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceShiftAsBob = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("shifts")
    .doc("shift_alice_1");

  await assertFails(aliceShiftAsBob.get());
  await assertFails(
    aliceShiftAsBob.update({
      shiftStatus: "COMPLETED",
      updatedAt: new Date(),
    })
  );
  await assertFails(aliceShiftAsBob.delete());
});

test("Shadow update & value poisoning: rejects ghost fields and invalid enum values", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("shifts")
    .doc("shift_alice_2");

  // Rejects extra ghost field on create
  await assertFails(
    docRef.set(validShiftPayload(ALICE_UID, { isSuperAdmin: true }))
  );

  // Rejects invalid category enum
  await assertFails(
    docRef.set(validShiftPayload(ALICE_UID, { category: "INVALID_CAT" }))
  );
});
