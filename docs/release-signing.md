# Release signing

The release build reads signing values from the local `keystore.properties` file. Git ignores this file and all `.jks` / `.keystore` files.

## Local files on the development machine

- Keystore: `C:/Users/CongQuyManh/.android/tilt-escape-release.jks`
- Signing properties: `keystore.properties` in the repository root
- Key alias: `tiltescape`

Never commit either local file. Back up the keystore separately and store its password in a password manager. Losing the key prevents future APK updates from being installed over versions signed with it.

## Build

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\CongQuyManh\AppData\Local\Android\Sdk'
.\gradlew.bat clean testDebugUnitTest assembleRelease lintDebug
```

Signed output:

```text
app/build/outputs/apk/release/app-release.apk
```

## Certificate identity

- Distinguished name: `CN=Tilt Escape, OU=Development, O=vanvinh2402-png, L=Ho Chi Minh City, ST=Ho Chi Minh, C=VN`
- SHA-256 certificate fingerprint: `84c6febb235863b8a9cf271ed88322c85616c013158fd7c22963b04feaf13bf0`
- Algorithm: RSA 4096
- APK signing scheme: v2

## Installing over debug builds

Debug and release APKs use different signing keys. Android will reject a release APK installed over the debug APK with the same package name. Uninstalling the debug APK first fixes the signature conflict but deletes that installation's local accounts, sessions and game results.

## Versioning

Increase `versionCode` for every published update. Change `versionName` when the user-facing release number changes.
