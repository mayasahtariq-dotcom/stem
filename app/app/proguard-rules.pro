# Retrofit, OkHttp and kotlinx.serialization ship their own consumer rules.
# Ktor (pulled in by supabase-kt) references optional classes that are not on Android.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
