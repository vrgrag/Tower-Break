# kotlinx.serialization keeps generated serializers; the plugin already adds the
# needed rules, but we pin the models package to be safe under aggressive R8.
-keep,includedescriptorclasses class com.towerbreak.towerbreakgame.**$$serializer { *; }
-keepclassmembers class com.towerbreak.towerbreakgame.** {
    *** Companion;
}
-keepclasseswithmembers class com.towerbreak.towerbreakgame.** {
    kotlinx.serialization.KSerializer serializer(...);
}
