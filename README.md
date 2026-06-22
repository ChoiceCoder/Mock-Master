# GovtPrep Android App

Native Android app built with Kotlin + Jetpack Compose + Material 3.
Uses the same Supabase backend as the website.

## Quick Setup

1. **Open in Android Studio** (Hedgehog or newer)
   - File → Open → select this folder

2. **Edit `local.properties`** — add your SDK path and Supabase keys:
   ```
   sdk.dir=C:\\Users\\anura\\AppData\\Local\\Android\\Sdk
   SUPABASE_URL=https://xipvucorxmyjftmvhegn.supabase.co
   SUPABASE_ANON_KEY=your-anon-key-here
   ```

3. **Sync Gradle** — Android Studio will prompt you, click "Sync Now"

4. **Run** — Click the green play button (or Shift+F10)

## Project Structure

```
app/src/main/java/com/govtprep/app/
├── data/
│   ├── model/Models.kt          # All data models (Exam, Question, etc)
│   └── remote/
│       ├── SupabaseModule.kt     # Supabase client config
│       ├── AuthRepository.kt     # Login, signup, profile
│       ├── ExamRepository.kt     # Exams, subjects, test sets
│       └── TestRepository.kt     # Questions, scoring, attempts
├── di/AppModule.kt               # Hilt dependency injection
├── ui/
│   ├── theme/                    # Colors, typography, Material 3 theme
│   ├── navigation/NavGraph.kt    # All routes
│   └── screens/
│       ├── auth/                 # Login, Signup
│       ├── dashboard/            # Home with stats
│       ├── exam/                 # Exam detail + subjects
│       ├── test/                 # Test engine + results + review
│       ├── profile/              # User profile
│       └── settings/             # App settings
├── GovtPrepApp.kt                # Application class
└── MainActivity.kt               # Entry point
```

## Features
- Same Supabase backend (zero backend changes needed)
- Server-side scoring (anti-cheat via RPC functions)
- Material 3 design with emerald theme
- Hindi/English support
- Dark mode support
- Haptic feedback on option selection
- Countdown timer with visual warnings
- Question palette for quick navigation
- Answer review with explanations
- ProGuard rules for release builds

## Build Release APK
```bash
./gradlew assembleRelease
```
APK at: `app/build/outputs/apk/release/app-release.apk`
