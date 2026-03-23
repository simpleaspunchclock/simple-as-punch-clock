PunchClockPanda
================

This is a simple offline Android punch clock project intended for Android Studio Panda.

Default seeded data on first launch:
- Admin PIN: 9999
- Sarah / 1234
- Mike / 2345
- Emma / 3456
- Luis / 4567

Features included:
- Shared-tablet punch in / punch out flow
- Employee name selection
- 4-digit employee PIN check
- Double-punch prevention (must alternate IN/OUT)
- Manual self-correction limited to the last 24 hours
- Admin login with 4-digit PIN
- Employee management (add/edit/activate/deactivate)
- Punch log viewing and admin corrections
- Date-range hour totals by employee
- Audit log of actions
- Fully offline SQLite storage

Open in Android Studio:
1. Unzip the folder.
2. In Android Studio, choose File > Open.
3. Select the root folder named PunchClockPanda.
4. Let Gradle sync and then run the app on a device or emulator.

Notes:
- This project was prepared to be compatible with older Android Studio / AGP combinations than the earlier version.
- I could not compile-test it inside this environment because the Android SDK is not available here.
- If Android Studio asks to update the Gradle wrapper or SDK platform, accept the minimal compatible option it suggests.
