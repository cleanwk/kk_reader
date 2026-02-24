# Sherpa-ONNX
-keep class com.k2fsa.sherpa.onnx.** { *; }

# epub4j
-dontwarn org.xmlpull.**
-keep class nl.siegmann.epublib.** { *; }

# PdfBox
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn org.bouncycastle.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Commons Compress
-dontwarn org.apache.commons.compress.**
