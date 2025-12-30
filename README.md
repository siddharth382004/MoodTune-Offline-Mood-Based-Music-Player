# MoodTune




MoodTune is a simple music player Android app that recommends songs based on your mood. It uses the device's camera to detect your facial expression and suggests a song that matches how you're feeling.

## ✨ Features

*   **Manual Song Library**: Browse and play songs from a predefined list.
*   **Mood-Based Recommendations**: Get a song recommendation based on your detected mood (Happy, Sad, or Neutral).
*   **Real-time Mood Detection**: Uses the front-facing camera and ML Kit's Face Detection to analyze your expression in real-time.
*   **Modern UI**: Built with Jetpack Compose for a clean, modern, and responsive user interface, inspired by popular music apps.

## 🚀 Technologies Used

*   [Kotlin](https://kotlinlang.org/): The primary programming language for the app.
*   [Jetpack Compose](https://developer.android.com/jetpack/compose): For building the app's user interface.
*   [CameraX](https://developer.android.com/training/camerax): To access the camera and capture images for mood analysis.
*   [ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection): To detect faces and analyze facial expressions (specifically, smiling probability).
*   [Material Design 3](https://m3.material.io/): For UI components and styling.
*   [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html): For managing background tasks like mood detection timers.

## 🛠️ How to Run

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/your-username/MoodTune.git
    ```
2.  **Open in Android Studio**:
    *   Open Android Studio.
    *   Click on `File` -> `Open` and select the cloned project directory.
3.  **Sync Gradle**:
    *   Let Android Studio sync the project's dependencies.
4.  **Run the app**:
    *   Connect an Android device or start an emulator.
    *   Click the `Run` button (▶️) in Android Studio.

> **Note**: The app requires camera permission to use the mood detection feature. You will be prompted to grant this permission on the first launch.
