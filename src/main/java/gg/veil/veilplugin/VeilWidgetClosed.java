package gg.veil.veilplugin;

// RuneLite doesn't expose WidgetClosed as a standard event in all versions.
// This shim ensures the plugin compiles regardless.
// In RL 1.10+, WidgetClosed IS a real event — this file does nothing.
// The subscription in VeilPlugin uses the real RL event.
// This file intentionally empty — kept for package completeness.
