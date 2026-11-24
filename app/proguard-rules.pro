# ProGuard / R8 rules for Tiny-Metronome

-keepattributes *Annotation*

# Preserve enum names and valueOf()/values() used by PresetRepository JSON persistence
-keepclassmembers enum com.metronome.app.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep enum com.metronome.app.model.** { *; }

# Preserve AndroidViewModel constructor invoked via reflection
-keep class com.metronome.app.ui.MetronomeViewModel {
    public <init>(android.app.Application);
}
