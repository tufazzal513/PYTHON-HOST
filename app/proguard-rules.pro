# PyMobile IDE ProGuard rules
# Keep Chaquopy / Python reflection working.
-keep class com.chaquo.python.** { *; }
-dontwarn com.chaquo.python.**

# Keep JGit (reflection-heavy).
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-dontwarn org.apache.**

# Gson model classes used for JSON metadata.
-keep class com.python.localhost.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# General.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
