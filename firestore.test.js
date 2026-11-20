const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");
const { describe, it, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";

function pastTimestamp() {
  return new Date(Date.now() - 1000);
}

function validAccountPayload(uid) {
  const now = pastTimestamp();
  return {
    userId: uid,
    name: "Primary Account",
    broker: "IBKR",
    startingBalance: 25000.0,
    currency: "USD",
    createdAt: now,
    updatedAt: now,
  };
}

function validPlaybookPayload(uid) {
  const now = pastTimestamp();
  return {
    userId: uid,
    name: "Opening Range Breakout",
    description: "15m breakout continuation on high relative volume.",
    marketCondition: "Trending",
    rules: ["RVOL > 1.5x", "Clean 15m range"],
    targetRiskReward: 2.5,
    colorHex: 4278243460,
    demoRecord: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validTradePayload(uid, accountId, overrides = {}) {
  const now = pastTimestamp();
  return {
    userId: uid,
    accountId,
    symbol: "NVDA",
    assetClass: "STOCK",
    side: "LONG",
    status: "CLOSED",
    entryPrice: 138.4,
    exitPrice: 143.9,
    quantity: 100,
    contractMultiplier: 1,
    fees: 4.2,
    stopLossPrice: 136.2,
    takeProfitPrice: 144.0,
    entryTimeEpochMs: 1763500000000,
    exitTimeEpochMs: 1763503600000,
    playbookId: "pb_orb",
    playbookAdherencePct: 100,
    checkedRules: ["RVOL > 1.5x"],
    mistakeTags: [],
    setupTags: ["A+ Setup"],
    emotionBefore: "CALM",
    rating: 5,
    notes: "Clean 15m breakout above resistance",
    demoRecord: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validJournalPayload(uid, accountId, dateIso = "2026-11-20", overrides = {}) {
  const now = pastTimestamp();
  return {
    userId: uid,
    accountId,
    dateIso,
    preMarketPlan: "Watch NVDA and ES open.",
    postMarketReview: "Executed plan cleanly.",
    lessonsLearned: "Patience pays.",
    mood: "DISCIPLINED",
    marketRegime: "TRENDING_UP",
    followedPlan: true,
    demoRecord: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

describe("EdgeLog Firestore Security Rules", () => {
  let testEnv;

  before(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        host: "127.0.0.1",
        port: 8085,
        rules: fs.readFileSync("firestore.rules", "utf8"),
      },
    });
  });

  after(async () => {
    await testEnv.cleanup();
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  it("denies unauthenticated reads and writes across all user collections", async () => {
    const anonDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(anonDb.collection("users").doc("alice").collection("accounts").get());
    await assertFails(
      anonDb.collection("users").doc("alice").collection("accounts").doc("acc_1").set(validAccountPayload("alice"))
    );
    await assertFails(anonDb.collection("users").doc("alice").collection("playbooks").get());
    await assertFails(anonDb.collection("users").doc("alice").collection("trades").get());
    await assertFails(anonDb.collection("users").doc("alice").collection("journals").get());
  });

  it("enforces strict per-user isolation between Alice and Bob", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    const bobDb = testEnv.authenticatedContext("bob").firestore();

    await assertSucceeds(
      aliceDb.collection("users").doc("alice").collection("accounts").doc("acc_1").set(validAccountPayload("alice"))
    );
    await assertSucceeds(
      aliceDb.collection("users").doc("alice").collection("playbooks").doc("pb_1").set(validPlaybookPayload("alice"))
    );
    await assertSucceeds(
      aliceDb.collection("users").doc("alice").collection("trades").doc("tr_1").set(validTradePayload("alice", "acc_1"))
    );
    await assertSucceeds(
      aliceDb
        .collection("users")
        .doc("alice")
        .collection("journals")
        .doc("acc_1_2026-11-20")
        .set(validJournalPayload("alice", "acc_1"))
    );

    // Bob cannot read, list, create, update, or delete Alice's documents
    await assertFails(bobDb.collection("users").doc("alice").collection("accounts").get());
    await assertFails(bobDb.collection("users").doc("alice").collection("accounts").doc("acc_1").get());
    await assertFails(bobDb.collection("users").doc("alice").collection("trades").get());
    await assertFails(bobDb.collection("users").doc("alice").collection("trades").doc("tr_1").get());
    await assertFails(bobDb.collection("users").doc("alice").collection("journals").doc("acc_1_2026-11-20").get());
    await assertFails(
      bobDb
        .collection("users")
        .doc("alice")
        .collection("accounts")
        .doc("acc_1")
        .update({ name: "Hijacked", updatedAt: pastTimestamp() })
    );
    await assertFails(bobDb.collection("users").doc("alice").collection("trades").doc("tr_1").delete());
    await assertFails(
      bobDb.collection("users").doc("alice").collection("playbooks").doc("pb_2").set(validPlaybookPayload("bob"))
    );
  });

  it("allows owner CRUD with Kotlin-aligned payloads, optional omitted fields, and batch parent account creation", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    const userDoc = aliceDb.collection("users").doc("alice");

    // Create account & open trade (omitting optional fields) in same WriteBatch using existsAfter
    const batch = aliceDb.batch();
    const accRef = userDoc.collection("accounts").doc("primary_default");
    const openTradeRef = userDoc.collection("trades").doc("tr_open");
    batch.set(accRef, validAccountPayload("alice"));

    const openTrade = validTradePayload("alice", "primary_default", {
      status: "OPEN",
    });
    delete openTrade.exitPrice;
    delete openTrade.exitTimeEpochMs;
    delete openTrade.stopLossPrice;
    delete openTrade.takeProfitPrice;
    delete openTrade.playbookId;
    delete openTrade.chartImagePath;
    batch.set(openTradeRef, openTrade);

    await assertSucceeds(batch.commit());

    // List queries matching TradingRepository.kt
    await assertSucceeds(userDoc.collection("accounts").orderBy("createdAt", "asc").get());
    await assertSucceeds(userDoc.collection("trades").orderBy("entryTimeEpochMs", "desc").get());
    await assertSucceeds(userDoc.collection("trades").where("demoRecord", "==", false).get());

    // Partial update on trade (closing it and updating notes) preserving immutable createdAt
    await assertSucceeds(
      openTradeRef.update({
        status: "CLOSED",
        exitPrice: 145.0,
        exitTimeEpochMs: 1763510000000,
        playbookId: "pb_1",
        chartImagePath: "/data/user/0/com.aistudio.edgelog.vqxrmk/files/trade_charts/sample.jpg",
        notes: "Closed at target",
        updatedAt: pastTimestamp(),
      })
    );

    // Clearing optional fields via FieldValue.delete() (mirrors deletePlaybook & reopening trade)
    const { createRequire } = require("node:module");
    const rulesRequire = createRequire(require.resolve("@firebase/rules-unit-testing"));
    const { deleteField, updateDoc } = rulesRequire("firebase/firestore");
    await assertSucceeds(
      updateDoc(openTradeRef._delegate, {
        status: "OPEN",
        exitPrice: deleteField(),
        exitTimeEpochMs: deleteField(),
        playbookId: deleteField(),
        chartImagePath: deleteField(),
        updatedAt: pastTimestamp(),
      })
    );

    // Create & partial update daily journal
    const journalRef = userDoc.collection("journals").doc("primary_default_2026-11-20");
    await assertSucceeds(journalRef.set(validJournalPayload("alice", "primary_default", "2026-11-20")));
    await assertSucceeds(
      journalRef.update({
        postMarketReview: "Updated review after close",
        followedPlan: false,
        updatedAt: pastTimestamp(),
      })
    );

    // Partial update account & playbook
    await assertSucceeds(accRef.update({ name: "Margin 50K", startingBalance: 50000, updatedAt: pastTimestamp() }));
    const pbRef = userDoc.collection("playbooks").doc("pb_1");
    await assertSucceeds(pbRef.set(validPlaybookPayload("alice")));
    await assertSucceeds(pbRef.update({ targetRiskReward: 3.2, updatedAt: pastTimestamp() }));

    // Delete works for owner
    await assertSucceeds(openTradeRef.delete());
    await assertSucceeds(journalRef.delete());
    await assertSucceeds(pbRef.delete());
    await assertSucceeds(accRef.delete());
  });

  it("rejects orphaned trades/journals, id injection, shadow updates, immutable field tampering, and invalid bounds", async () => {
    const aliceDb = testEnv.authenticatedContext("alice").firestore();
    const userDoc = aliceDb.collection("users").doc("alice");

    // 1. Orphaned trade & journal without parent account
    await assertFails(
      userDoc.collection("trades").doc("orphan_tr").set(validTradePayload("alice", "nonexistent_acc"))
    );
    await assertFails(
      userDoc
        .collection("journals")
        .doc("nonexistent_acc_2026-11-20")
        .set(validJournalPayload("alice", "nonexistent_acc"))
    );

    // Create valid account first
    const accRef = userDoc.collection("accounts").doc("acc_1");
    await assertSucceeds(accRef.set(validAccountPayload("alice")));

    // 2. Injecting "id" field into payload (violates hasOnly & @DocumentId safety)
    await assertFails(
      userDoc.collection("accounts").doc("acc_bad").set({
        ...validAccountPayload("alice"),
        id: "acc_bad",
      })
    );

    // 3. Future timestamp rejection
    const future = new Date(Date.now() + 120000);
    await assertFails(
      userDoc.collection("playbooks").doc("pb_future").set({
        ...validPlaybookPayload("alice"),
        createdAt: future,
        updatedAt: future,
      })
    );

    // 4. Shadow Update (ghost field) & immutable createdAt/userId tampering
    await assertFails(
      accRef.update({
        name: "Valid Name",
        role: "admin",
        updatedAt: pastTimestamp(),
      })
    );
    await assertFails(
      accRef.update({
        createdAt: new Date(Date.now() - 50000),
        updatedAt: pastTimestamp(),
      })
    );
    await assertFails(
      accRef.update({
        userId: "bob",
        updatedAt: pastTimestamp(),
      })
    );

    // 5. Invalid enum, rating out of bounds, invalid dateIso
    await assertFails(
      userDoc.collection("trades").doc("tr_bad_enum").set(validTradePayload("alice", "acc_1", { assetClass: "MEME" }))
    );
    await assertFails(
      userDoc.collection("trades").doc("tr_bad_rating").set(validTradePayload("alice", "acc_1", { rating: 6 }))
    );
    await assertFails(
      userDoc
        .collection("journals")
        .doc("acc_1_bad")
        .set(validJournalPayload("alice", "acc_1", "2026/11/20"))
    );
  });
});
