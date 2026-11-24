# Support Ticket Resolutions (as of 15 Nov 2026, 10:00 IST)

### T1 — u02 Rahul (Premium) · 9 Nov
* **(a) What happened:** Rahul watched in `Asia/Kolkata` on Nov 7 (`07:09 IST`), flew to San Francisco (`tz_change` to `America/Los_Angeles` on Nov 7 `22:10 PST`), and watched on Nov 8 (`21:30 PST`). The old system evaluated him in `home_tz` (`Asia/Kolkata`), where `21:30 PST` Nov 8 was `11:00 IST` Nov 9, leaving Nov 8 blank.
* **(b) System shows:** **54** active through Nov 13 (`ACTIVE_PENDING_TODAY` at 10:00 IST = `20:30 PST` Nov 14; becomes **55** at `10:31 IST` when his Nov 14 `21:01 PST` lesson finishes).
* **(c) Reply to user:** "Hi Rahul, welcome to San Francisco! Our old streak tracker was still checking your schedule against India time instead of your current California timezone, which made Nov 8 look blank after your flight. We've updated our system to follow your local device timezone—your full streak is completely intact and unbroken!"
* **(d) Action:** Product fix (timezone evaluation by `device_tz`); no freeze deducted.

### T2 — u03 Ananya (Free) · 11 Nov
* **(a) What happened:** On Nov 10 (flight), Ananya watched `v019` (165s) offline at `14:05 IST`, which synced 19.6 hours later on Nov 11 at `09:40 IST` (`04:10Z`) with duplicate retry packets from app `3.1.4`. She received a premature "streak lost" push before sync completed. (Her Nov 12 lesson also synced 37.75h late on Nov 14).
* **(b) System shows:** **26** (`ACTIVE_PENDING_TODAY`, `12 + 14` days).
* **(c) Reply to user:** "Hi Ananya, sorry for the scary notification! Your offline lesson from your flight on Nov 10 synced safely to our servers the next morning, and our system automatically credited it back to Nov 10. Your 26-day streak is safe and active—and we're updating our morning alerts so offline lessons don't trigger false alarms."
* **(d) Action:** Product fix (deduplicated `3.1.4` retries + 7-day offline sync grace); prompt her to update from `3.1.4` to `3.2.0+`.

### T3 — u04 Vikram (Free, Beta) · 14 Nov
* **(a) What happened:** From Nov 1-5 Vikram watched full lessons (75-162s/day), reaching a 25-day streak. From Nov 6-14, he opened 6-8 videos every night around 23:06 IST and swiped away after 2-7 seconds each (totaling only 28-39s/day).
* **(b) System shows:** **0** (`BROKEN` on `2026-11-06` with `INSUFFICIENT_WATCH_TIME`).
* **(c) Reply to user:** "Hi Vikram, we love seeing you check in every night! Streaks-v2 requires at least 60 seconds of active lesson watching per day so that streaks reflect real learning rather than quick previews—since Nov 6, your sessions totaled about 30-39 seconds per night. Watch one full ~2-minute lesson tonight to start building a fresh streak!"
* **(d) Action:** No restore; educate user on the 60s daily learning requirement.

### T4 — u05 Meera (Free) · 10 Nov
* **(a) What happened:** On Nov 9, Meera opened the app at `23:58:40 IST`, started `v010` at `23:59:05 IST`, and finished at `2026-11-10T00:00:27+05:30` (80s watched). The old system attributed the lesson to Nov 10 (`video_end` date), breaking her streak on Nov 9.
* **(b) System shows:** **20** (`ACTIVE_PENDING_TODAY`, `6 + 14` days).
* **(c) Reply to user:** "Hi Meera, you're completely right—you started your lesson before midnight on Nov 9! Our updated system now credits lessons to the day you start watching, so both Nov 9 and Nov 10 count properly. Your 20-day streak is fully restored!"
* **(d) Action:** Product fix (assign lessons to `video_start` calendar date).

### T5 — u07 Sneha (Premium) · 8 Nov
* **(a) What happened:** Sneha watched `v013` (125s) offline in the metro on Nov 7 (`21:15 IST`). At `09:02 IST` on Nov 8 (before syncing), the old app warned she missed Nov 7, so she bought a Rs 49 freeze (`GPA.3391-2271-8812`). At `12:00 IST` (`06:30Z`), her Nov 7 lesson synced.
* **(b) System shows:** **72** (`ACTIVE_PENDING_TODAY`, `58 + 14` days; Nov 7 is `COMPLETED`).
* **(c) Reply to user:** "Hi Sneha, thank you for flagging this! Your Nov 7 metro lesson synced safely on Nov 8, so your 72-day streak never actually needed a freeze. We've issued a full Rs 49 refund for the unnecessary Streak Freeze purchase (`GPA.3391-2271-8812`), which will reflect in 3-5 business days."
* **(d) Action:** **Refund Rs 49** (`order=GPA.3391-2271-8812`).

### T6 — u08 Arjun (Premium) · 10 Nov
* **(a) What happened:** Arjun watched at ~21:05 IST on Nov 9 during the `20:45-21:50 IST` ingestion outage (`POST /v1/events` returned 503s for `uid=u08` on app `3.2.0`, which did not retry).
* **(b) System shows:** **313** (`ACTIVE_PENDING_TODAY`, `300 + 13` completed days + Nov 9 `OUTAGE_EXCUSED` bridged).
* **(c) Reply to user:** "Hi Arjun, huge apologies for the scare—we had a server outage on the evening of Nov 9 that dropped your lesson sync, and that was 100% on us. We've excused Nov 9 from our side without using your Streak Freeze, so your 300+ day streak is completely safe and active!"
* **(d) Action:** Outage bridge applied (`OUTAGE_EXCUSED`); preserve his `freeze_balance=1`.

### T7 — u06 Karthik (Free) · 10 Nov
* **(a) What happened:** Karthik opened the app online on Nov 10 at `20:01 IST` with `queue_depth=0` (confirming zero unsynced offline events on his phone), and 5 minutes later (`20:06 IST`) uploaded backdated Nov 8 and Nov 9 events for `v037` with identical 128s watch durations.
* **(b) System shows:** **5** (`ACTIVE_PENDING_TODAY` for Nov 10-14; Nov 8 & 9 marked `REJECTED_CLOCK_TAMPER`).
* **(c) Reply to user:** "Hi Karthik, we checked your sync logs for Nov 8-9, and our system detected that your app had no offline lessons queued prior to Nov 10, so those backdated entries couldn't be verified. Your current active streak from Nov 10 onward is 5 days—keep watching daily to build it right back up!"
* **(d) Action:** Do NOT restore Nov 8-9 (`REJECTED_CLOCK_TAMPER`).

### T8 — u10 Rohan (Free) · 13 Nov
* **(a) What happened:** On Nov 12, Rohan watched `v003` for `101s` (whereas on all other days he watched 2 videos totaling 186-266s). The old 120s threshold marked Nov 12 incomplete/grey.
* **(b) System shows:** **23** (`ACTIVE_PENDING_TODAY`, `9 + 14` days).
* **(c) Reply to user:** "Hi Rohan, good catch! You watched 101 seconds on Nov 12, which is a full lesson—our old threshold was mistakenly flagging single shorter lessons as incomplete. We've updated the rule to 60 seconds per day, so Nov 12 is now green and your 23-day streak is intact!"
* **(d) Action:** Product fix (60s daily threshold).

### T9 — u13 Nikhil (Free) · 13 Nov
* **(a) What happened:** Nikhil watched `v019` (164s) offline on Nov 12 (`20:56 IST`). When he reconnected on Nov 13 (`21:02 IST`), his online `app_open` reported `queue_depth=3` and flushed his Nov 12 events `24.10h` late.
* **(b) System shows:** **58** (`ACTIVE_PENDING_TODAY`, `44 + 14` days).
* **(c) Reply to user:** "Hi Nikhil, your offline lesson from Nov 12 synced cleanly when you came back online on Nov 13! Our system verified your offline queue and credited Nov 12, so your 58-day streak is completely safe."
* **(d) Action:** None (automatically credited under 7-day offline sync policy).

### T10 — u14 Tanvi (Free) · 12 Nov
* **(a) What happened:** Tanvi watched `v001` (136s) offline on the train on Nov 11 (`21:31 IST`). When she reconnected on Nov 12 (`21:24 IST`), her online `app_open` reported `queue_depth=3` and flushed her Nov 11 events `23.88h` late.
* **(b) System shows:** **65** (`ACTIVE_PENDING_TODAY`, `51 + 14` days).
* **(c) Reply to user:** "Hi Tanvi, hope you had a safe train journey! Your offline lesson from Nov 11 synced properly when you reconnected on Nov 12, and your 65-day streak is safe and uninterrupted."
* **(d) Action:** None (automatically credited under 7-day offline sync policy).

---

### Reply to Pooja (`support_expected_streaks.md`)

> Hi Pooja,
>
> Thanks for sharing the promised numbers! Here is how `streaks-v2` lines up with your table as of **15 Nov 2026, 10:00 IST**:
>
> * **T2 (`u03` Ananya = 26) & T5 (`u07` Sneha = 72):** **Exact match.** Also please process a **Rs 49 refund** for Sneha (`order=GPA.3391-2271-8812`), as her offline lesson synced after she panic-bought a freeze.
> * **T1 (`u02` Rahul = 54 at 10:00 IST / 55 at 10:31 IST):** Because Rahul is now in California (`PST`), `10:00 IST` is `20:30 PST` on Nov 14—his Nov 14 lesson was completed at `21:01 PST` (`10:31 IST`), so by the time agents close the ticket today he is at **55**.
> * **T6 (`u08` Arjun = 313 vs. promised 314):** Per our Play Store listing and Monetisation/Growth alignment, forgiven outage days **bridge** a streak without adding `+1` day (`300 + 13 active days = 313`). Since Support already promised 314, we can honor a 1-day manual goodwill adjustment if he pushes back, though 313 is the canonical engine state.
> * **T7 (`u06` Karthik = 5 vs. promised 47):** We found hard device-telemetry proof of backdating on Nov 8-9 (`queue_depth=0` on his online `app_open` at `20:01 IST` Nov 10 right before the offline batch uploaded). Please use the updated non-restoration reply in `TICKETS.md` rather than crediting 47.
