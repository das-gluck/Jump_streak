# Streaks-v2 Product & Engineering Decisions

## 1. Core Streak Rules & Stakeholder Resolutions

| Rule Domain | Decision | Data & Unit Economics Rationale | Guardrail Metric (What Proves Me Wrong) |
|---|---|---|---|
| **Learning Day Threshold** | **>= 60 seconds cumulative `watched_s`** per calendar day (assigned to `video_start` date). | Sided partially with **Neha** over **Farhan's** strict 120s rule. Many single videos are 72-118s (`v002`=73s, `v010`=80s); 8 of 15 users (`u03, u05, u06, u07, u11, u13, u14, u15`) watch 1 video/day of 72-118s. Requiring 120s breaks valid habits, whereas 60s catches **Vikram (`u04`)** who swipes 6-8 videos for 2-7s each (28-39s/day). Crediting by `video_start` protects **Meera (`u05`)**, who started at `23:59:05 IST` and finished at `00:00:27 IST`. | % of active streak days with only 1 video watched dropping, or quiz completion scores falling for single-video users. |
| **Day Boundary & Travel** | Local calendar date of `client_ts` in **`device_tz`** (with freeze bridging for International Date Line skips). | Sided with **Neha**. Using `home_tz` broke **Rahul (`u02`)** when he moved IST -> PST (`21:30 PST` Nov 8 = `11:00 IST` Nov 9). When **Ishaan (`u15`)** flew LA -> Auckland (`-08:00` -> `+13:00`), Nov 10 was skipped on his clock; his 1 Premium freeze bridges the skipped date without adding +1 day. | >0.5% of users changing `device_tz` more than twice in 7 days (timezone-hopping abuse). |
| **Late Syncs & Clock Tampering** | Trust `client_ts` up to **7 days (168h)** late **iff**: (1) `client_ts <= received_at`, (2) video published <= `client_ts`, and (3) on app `3.2.0+`, online `app_open` reports `queue_depth > 0`. | Resolves **Sameer** vs. **Neha**. Rejects **Karthik (`u06`)**, whose online `app_open` at `20:01 IST` reported `queue_depth=0` 5 minutes before backdated Nov 8-9 events arrived, while accepting **Sneha (`u07`)** and honest offline users. | Honest offline users with app crashes losing `queue_depth` metadata and filing tickets. |
| **Duplicates** | Deduplicate by `event_id` **and** composite key `(user_id, event_type, video_id, client_ts, watched_s)`. | App `3.1.4` (**Ananya `u03`**) resent Nov 10 events with both identical IDs and regenerated IDs (`e_c9af18f843b9`). | Legitimate re-watches of the same video in the exact same second (impossible in practice). |
| **9 Nov Outage & Freezes** | `OUTAGE_EXCUSED` for any user in `affected_uids` on `2026-11-09`. Protected days (outages & freezes) **bridge** the streak (+0 days). | Sided with **Neha** on outage forgiveness and **Ritika** on bridge semantics (Play Store contract). **Arjun (`u08`)** had three 503s during his 9 PM window; his 300+ streak bridges Nov 9 without deducting his freeze. | Support ticket volume on outage dates or Play Store rating complaints. |

---

## 2. Unverifiable "I Was Offline" Syncs (`u13`, `u14`)

* **Policy:** Accept offline syncs up to 7 days late when `video_id` was already published at `client_ts` and app `3.2.0+` reports `queue_depth > 0` on reconnect.
* **Applied to `u13` (Nikhil) & `u14` (Tanvi):** Both are on `3.2.0`, both opened the app online the next evening with `queue_depth=3`, and immediately flushed 3 valid events (`24.10h` and `23.88h` late). **Both are accepted** (`u13` = **58**, `u14` = **65**).
* **Expected Monthly Cost at 2M MAU (3,000 cases/day = 90,000/month):**
  * **Wrongly rejecting 1 honest user:** `0.60 * (0.90 * 0.16 * Rs 45 + 0.10 * 0.12 * Rs 1,600) + (0.35 * Rs 40) = Rs 15.41 + Rs 14.00 = Rs 29.41`
  * **Wrongly accepting 1 fake user:** `(0.20 * Rs 49) + Rs 5.00 = Rs 14.80`
  * **At 11.5% fake rate (T&S audit):** Rejecting all costs **Rs 23.42 lakh/mo** vs. Accepting all costs **Rs 1.53 lakh/mo** (15.3x worse to reject).
  * **At 25% fake rate (Farhan's upper bound):** Rejecting all costs **Rs 19.85 lakh/mo** vs. Accepting all costs **Rs 3.33 lakh/mo** (6.0x worse to reject).
* **What would change it:** If T&S proves fake rates exceed **66.5%** (the breakeven threshold where `p * 14.80 > (1-p) * 29.41`) or if a client-side cryptographic HMAC timestamp is shipped in `3.3.0`.

---

## 3. The Missed Day, Hour by Hour

| Time Since Missed Midnight | (a) Unsynced Offline User (`queue_depth > 0` on device) | (b) Free User Who Forgot (`0` freezes) | (c) Premium User Who Forgot (`1` freeze in balance) |
|---|---|---|---|
| **00:00 (Midnight)** | App shows **"Syncing / Pending"** (amber flame). No push sent. | App shows **"Streak at Risk - 24h Grace"** (amber flame). No push sent. | App shows **"Streak Protected (1 Freeze Ready)"**. No push sent. |
| **08:30 (Next Morning)** | App detects offline queue locally; prompts *"Connect to sync yesterday's lesson."* | Push + In-app modal: *"Don't lose your X-day streak! Repair yesterday with a Streak Freeze (Rs 49) or complete 2 lessons today."* | Push: *"Life happens! Your Premium Streak Freeze is standing by to protect your X-day streak."* |
| **+24h (End of Day + 1)** | Still amber **"Awaiting Sync (up to 7d)"** if offline; if synced, turns green **"Completed"** (and refunds any panic-bought freeze). | If no freeze bought: flame resets to **0**; push *"Start a new streak today!"* | **Auto-applies 1 freeze** from `freeze_balance` at +24h; streak bridges at X days; balance becomes 0. |
| **+48h (End of Day + 2)** | Still amber **"Awaiting Sync"** up to Day 7; if still unsynced at Day 7, breaks. | Streak remains broken (or at 1-2 if resumed). | Streak active at X + 1 (if learned on Day + 1 & Day + 2). |

* **Effect on Freeze Revenue:** Ritika's **-58%** figure in Weeks 1-4 was distorted by a **Simpson's paradox** (the billing-SDK bug excluded ~90% of Premium users from `v2`, and Premium users buy freezes at 8.3x the Free rate). In post-fix Weeks 5-10, freeze purchases fell **-34.8%**. By showing a **24h grace window with an explicit Rs 49 freeze offer at 08:30 AM** (instead of a silent 48h pending state with zero freeze prompts), we recover most of the ~Rs 15.7L/mo freeze drop while eliminating false "You lost your streak!" pushes.

---

## 4. Beta Decision (`beta_cohorts.csv`): Run Targeted Follow-Up Experiment

* **Why not ship v2 as-is?** In post-SDK-fix **Weeks 5-10** (weighted 91% Free / 9% Premium):
  * **D7 Retention Lift:** `34.30%` -> `36.73%` = **+2.43 pp** x Rs 2.6 lakh/pp = **+Rs 6.32 lakh/month**.
  * **Freeze Revenue Loss:** `42.08` -> `27.45` freezes/1k = **-34.8%** x Rs 45 lakh/mo = **-Rs 15.66 lakh/month**.
  * **Net Impact:** **-Rs 9.34 lakh/month** (value-destroying due to missing freeze prompts).
* **Follow-Up Experiment (`streaks-v2.1`):**
  * **Change:** Replace silent 48h pending with a **24h pending window + 08:30 AM freeze offer modal** (as designed in Section 3).
  * **Primary Metrics:** (1) D7 retention, (2) 14-day freeze revenue per 1,000 users.
  * **Guardrail:** Support ticket rate per 1,000 users (must stay <= control).
  * **Duration & Stop Rule:** 3 weeks (~30,000 users/arm). **Stop/kill** if freeze revenue per 1k drops >15% vs. control without offsetting D7 retention lift (>= +4.0 pp).

---

## 5. Three Things Deliberately Not Built

1. **Automated "Double-Lesson Streak Repair" Quest:** Neha implied letting free users earn back a broken streak by watching 2 videos the next day. **Skipped:** It directly cannibalizes Rs 49 freeze purchases (Ritika's core revenue stream) and complicates state transitions.
2. **Retroactive Auto-Refund Job on Late Sync:** While we flag `REFUND Rs 49` in the support inspector when a lesson syncs over an `explicitFreezeDate`, we did **not** build automated Google Play Billing refund API calls—manual agent approval prevents refund-abuse loops.
3. **Real-Time 2M-User Stream Aggregator (Kafka/Flink):** Built a clean deterministic engine over stored events per Sameer's guidance, documenting the production scaling architecture below instead of over-engineering infrastructure.

---

## 6. Scaling to 2M Users (Architecture Notes)

* **Pre-Aggregated Daily State Table (`user_daily_streaks`):** Store one materialized row per `(user_id, local_date)` (`watched_s`, `status`, `streak_count`) in PostgreSQL/Cassandra, partitioned by `user_id`. `GET /streak` becomes a single O(1) key lookup instead of replaying historical events.
* **Targeted Recompute on Late Events:** When a late event arrives for `D_old`, only recompute rows from `D_old` to `today` (typically 1-7 rows) for that single `user_id` inside a serialized per-user partition key.
* **Idempotency Deduplication Cache:** Maintain a 14-day TTL Redis Bloom filter / key set on `event_id` and `natural_key` at the ingestion edge (`POST /v1/events`) to drop `3.1.x` retry storms before hitting the database.
* **Push Notification Delay Queue:** Schedule morning streak-risk pushes via a delayed queue (e.g., SQS/Kafka timer) that re-checks `user_daily_streaks` at send time (`08:30` local) so users whose offline lessons synced overnight never receive false "streak lost" pushes.
