# Seck8s Android Client

This is a self-contained Android Studio project added alongside the backend. It displays the existing ticket, activity, login, seckill, order, and payment flows from the gateway without modifying backend sources.

1. Open `android-client` in Android Studio.
2. In `MainActivity.java`, set `DEFAULT_BASE_URL` to the gateway address reachable from the device. Use `http://10.0.2.2:8080` in the Android emulator or the computer's LAN IP on a physical phone.
3. Run on an Android 8.0+ device.

The project uses platform APIs only and needs Android Gradle Plugin / SDK installed by Android Studio.
