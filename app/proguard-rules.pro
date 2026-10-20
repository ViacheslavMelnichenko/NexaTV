# NexaTV — ProGuard/R8 rules. Apache-2.0.
# Compose and Kotlin ship consumer rules; app-specific keep rules are added here as needed.

# Proto DataStore (ADR-0008). protobuf-javalite ships NO consumer R8 rules (it is a plain JAR,
# not an AAR), and the lite runtime builds each message's schema by REFLECTING over the generated
# class's private fields (favorites_, settings_, component_, userSerial_, …). With minification on
# (release only) R8 renames/strips those fields, so (de)serialization silently breaks and the
# store loses favourites + settings — a release-only data-loss bug invisible in debug. Keep every
# generated lite message class and its members intact.
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
