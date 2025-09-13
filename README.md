# Snappy-Ruler-Set

## Snappy Ruler Drawing App

A drawing and measuring app built with Jetpack Compose in Android. It supports multiple tools like
ruler, protractor, compass, and set square, with features such as zoom, pan, snapping, calibration,
and persistence of drawings.

## Features

1.Tools

>>> Ruler – draw straight lines with length and angle display.

>>> Protractor – measure angles and rotate freely.

>>> Compass – draw circles with adjustable radius.

>>> Set Square – draw right-angle shapes with selectable variants.

Drawing & Gestures

>>> Drag – draw or move tools.

>>> Transform Gestures – pinch to zoom, two-finger pan.

>>> Long Press – toggle snapping on/off.

>>> Snap Engine – points snap to grid or nearby lines for precision.

Zoom & Pan

>>> Zoom in/out with gestures or buttons.

>>> Pan canvas with two-finger drag.

>>> Zoom range limited between 0x – 1.0x.

Calibration

>>> Calibrate the canvas using a physical ruler.

>>> Enter the actual measured length in cm, which adjusts pixels per cm (pxPerCm) for accurate scaling.

Persistence

>>> Save shapes and polylines locally using JSON.

>>> Load previously drawn shapes when app launches.

Export

>>> Export drawings as PNG or JPEG.

UI & Feedback

>>> HUD Overlay: Shows length and angle for current polyline.

>>> Snap hints with visual and haptic feedback.

>>> Floating Action Buttons for tool selection and export.

>>> Toggle switch for snap on/off.

Architecture

>>> MVVM architecture with DrawingViewModel.

>>> StateFlow for reactive UI state.

Jetpack Compose for UI.

>>> Kotlin Coroutines for asynchronous operations.

>>> Kotlinx Serialization for JSON encoding/decoding.