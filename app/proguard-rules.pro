# kotlinx.serialization
-keep,includedescriptorclasses class com.towerbreak.towerbreakgame.**$$serializer { *; }
-keepclassmembers class com.towerbreak.towerbreakgame.** {
    *** Companion;
}
-keepclasseswithmembers class com.towerbreak.towerbreakgame.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**

-keep class com.appsflyer.** { *; }
-keep class com.android.installreferrer.** { *; }
-dontwarn com.appsflyer.**

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-keep class okhttp3.** { *; }

-keep class androidx.security.crypto.** { *; }

# Gray entry points (rebrand.py updates paths) + native game host
-keep class com.towerbreak.towerbreakgame.ignite.BastionFcm
-keep class com.towerbreak.towerbreakgame.pane.BastionGate
-keep class com.towerbreak.towerbreakgame.pane.BastionApp
-keep class com.towerbreak.towerbreakgame.presentation.MainActivity

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
