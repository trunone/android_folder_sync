# FolderSync

FolderSync is an Android application designed to synchronize local folders on your device using the powerful `rsync` utility.

## Features

- **Local Folder Synchronization**: Sync files between two local directories on your Android device.
- **Rsync Backend**: Utilizes a native `rsync` binary for efficient and reliable synchronization.
- **Comparison Modes**: Supports synchronization based on file size/timestamp or MD5 checksums (Hash).
- **Progress Tracking**: View real-time logs and progress of the synchronization process.

## Requirements

- **Android Version**: Android 7.0 (Nougat) / API Level 24 or higher.

## Building the Project

To build the project, you need to have the Android SDK installed. You can build the debug APK using the included Gradle wrapper:

```bash
./gradlew assembleDebug
```

The output APK will be located in `app/build/outputs/apk/debug/app-debug.apk`.

## License

This project is released into the public domain under the [Unlicense](https://unlicense.org/). See the `UNLICENSE` file for more details.
