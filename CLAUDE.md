# android/ (Kotlin + Jetpack Compose)

Parent rules apply. Kotlin + Jetpack Compose only. Build/test: `./gradlew assembleDebug`, `./gradlew test`, `./gradlew lint`. Theme tokens come from the generated `theme/TreeFortTokens.kt` (edit design/tokens.json, then regenerate); no stray `Color(0xFF...)` literals elsewhere. `local.properties` is machine-local: do not commit or print it. Auth via Supabase Auth only; never `user_metadata` for roles.
