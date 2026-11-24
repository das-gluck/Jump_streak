Q2. When did you start and finish (date and time)? Roughly how many hours in total, and how did they split?

Ans) I started working on this project on saturady(21 Nov) arround 3p.m. It took approx 4 hrs to complete this project (this is the only time when i have written code) excluding time taken to read all the docs.

For understanding the project requirements and reading users.csv, events.csv(a complicated one) it took 2 hrs.

Q3. Who wrote what. Rough % of the first draft written by AI, for (a) the code, (b) DECISIONS.md, and (c) the ticket replies. Then: how much did you change afterwards (a little / a lot / rewrote)?

Ans) Roughly 80-90% code is written by AI.There were small changes which was made by me related to calculation, which is ofcourse is written by AI but decision was made by me.

Q4. From memory: which user was the hardest to get right, and why? (2–3 sentences)

Ans) Karthik was the difficult part to understand. The ticket which he has raised show that he was online on 8th and 9th , but in the event.csp file the "queue_depth" is 0, that means he has not opend the app.

Q5. Where in your code is (a) the place a lesson gets assigned to a calendar day, and (b) your duplicate handling? Give the file, function and approximate line for each. 

Ans) Both of them are located in StreakEngineService.java file. For calendar assignment -> evaluateDay() method is there around line 154-156 line. And for duplicate handling -> ingestEvent() method is present around line 51.

Q6. Name one `event_id` you looked at by hand, and what you were checking when you looked at it.

Ans) I have checked the Karthik(u06) eventId "e_dd20c3bfd342", to check the queue_depth. Because his data in support_ticket.md was showing something else.

Q7. From memory:** what are **u07's** and **u15's** streaks in your CSV? For each, give your confidence (0–100%). Then check your CSV and write down whether you were right.

Ans) for u07 streak is - 72. And for u15 - 30.

Q8. **Q8. Validation.** List the concrete steps you took to check that the output (code, numbers, ticket diagnoses) was correct: commands you ran, tests you wrote or read, things you compared by hand. For each step, tell us where we can see the evidence (a test name, commit, file, or the moment in your transcript).

Ans) Tested this api - "GET /users/u07/streak?as_of=2026-11-15T10:00:00+05:30" for this I got error because of URL Encoded + space parsing in StreakApiController.java.

For duplicate filtering , i Tested /events with JSON payloads .

Eg :- 

[
  {
    "event_id": "e_test_1",
    "user_id": "u01",
    "event_type": "video_end",
    "video_id": "v001",
    "watched_s": 120,
    "client_ts": "2026-11-15T08:00:00+05:30",
    "device_tz": "Asia/Kolkata",
    "received_at": "2026-11-15T02:30:05Z",
    "app_version": "3.2.0",
    "meta": null
  },
  {
    "event_id": "e_test_1",
    "user_id": "u01",
    "event_type": "video_end",
    "video_id": "v001",
    "watched_s": 120,
    "client_ts": "2026-11-15T08:00:00+05:30",
    "device_tz": "Asia/Kolkata",
    "received_at": "2026-11-15T02:30:05Z",
    "app_version": "3.2.0",
    "meta": null
  },
  {
    "event_id": "e_test_u12_open",
    "user_id": "u12",
    "event_type": "app_open",
    "video_id": null,
    "watched_s": null,
    "client_ts": "2026-11-06T06:30:55+05:30",
    "device_tz": "Asia/Kolkata",
    "received_at": "2026-11-06T01:00:56Z",
    "app_version": "3.2.0",
    "meta": "queue_depth=3"
  },
  {
    "event_id": "e_test_u12_backfill",
    "user_id": "u12",
    "event_type": "video_end",
    "video_id": "v010",
    "watched_s": 95,
    "client_ts": "2026-11-05T19:00:00+05:30",
    "device_tz": "Asia/Kolkata",
    "received_at": "2026-11-06T01:00:58Z",
    "app_version": "3.2.0",
    "meta": null
  }
]

Q9. Paste one suggestion from the AI (code or text) that you **rejected or changed**, and say why in one or two sentences. If there wasn't one, say so.

Ans) As I have mentioned in Q8 that I was getting URL encoded + space parsing issue. That's what I changed and in UI part my dropdown (for user filtering) was not working that's what I made a changes.

Q10. Describe one thing the AI got **wrong** that *you* caught: what it was, how you noticed, and where the fix is. If you didn't catch any, say so. That's an honest answer, and we'll talk about it.

Ans) Initally for u07 (Sneha) wrongly flagged as REJECTED_CLOCK_TAMPER was coming,  When I opened http://localhost:8080/support?userId=u07, Nov 7 was marked REJECTED_CLOCK_TAMPER instead of showing the ₹49 refund, because hasQueueDepthProof() only looked within 5 minutes of batchReceivedAt (06:30Z) and missed her 07:17 IST online app_open (queue_depth=3). 

And user dropdown didn't submit properly and unencoded +05:30 in query params threw a DateTimeParseException.

Q11. One decision you **changed your mind** on during the task: what you thought before, what you decided after, and what made you change.

Ans) Initially , looking at the data all the .csv files it was so confusing that from where to start. 
During the api testing some issue came related to data present in .csv file and code generated by AI. That when I stared digging in events.csv , support_tickes.csv file. It was not so difficult to read that data.

**Q12.** For each statement, answer **1** (strongly disagree) to **5** (strongly agree). Go with your first reaction.

| a | I read every line of code in my submission. - 3
| b | The AI made no mistakes on this task. 2
| c | Some of my stakeholder decisions could reasonably have gone the other way. - 4
| d | I verified every number in DECISIONS.md myself. 3
| e | There's at least one part of my submission I'm not sure is correct. - 5
| f | I have never shipped code I didn't fully understand. - 3
| g | I could rebuild the core streak logic without AI in a working day. - 4 
| h | I'd be comfortable changing any rule in my code live, on a call, today. - 4

Q13. Rate from 1 to 10: 
(a) the quality of the AI's work on this task - 9 (beacause 95% of the code was written by AI)
(b) the quality of your own contribution. - 5 (there was some important core logic changes to be made by me that align the data with the code given by AI.)

Q14. Which *ticket* took you the longest, and why? (1–2 sentences)
Ans) It was the Karthik(u06) ticket because he was lying and due to that I got confused that whether my code is wrong . 
Then in README.md file I saw this line 
"The data is messy on purpose. Not every oddity is a bug, not every bug is pointed out to you, and not everything a stakeholder tells you is right.". 

Q16. Why did u11's app stop on 11 Nov (a `video_start` with no `video_end`)? Answer in one or two sentences. 

Ans) the app crashed, was force-closed. 

Q19. Declaration.** Were Q3–Q18 written by you without AI? **Yes / Partly / No.** If partly, which ones and how.

Ans) Only answer of Q10 is written by AI.

