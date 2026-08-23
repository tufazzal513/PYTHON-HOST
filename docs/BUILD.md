# Building the APK

## Using GitHub Actions (recommended)

The workflow at `.github/workflows/build-apk.yml` builds the APK automatically:

- **Every push** to `main`/`master`/`arena/**` builds a **debug** APK
- **Pull requests** build a debug APK to verify compilation
- **Manual dispatch** (Actions → Run workflow) lets you pick debug or release
- **Tag pushes** (`v1.0.0`) build a signed release and create a GitHub Release

### Debug APK
No setup required. Just push, wait ~5 minutes, and download from the **Artifacts** section on the Actions run page.

### Signed Release APK
Add these repository secrets (**Settings → Secrets → Actions → New secret**):

| Secret | Value |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | Output of `base64 -w0 your-keystore.jks` |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias |
| `SIGNING_KEY_PASSWORD` | Key password |

Then go to Actions → **Build APK** → **Run workflow**, choose `release`.

## Building locally

```bash
./gradlew assembleDebug     # debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease   # release APK → app/build/outputs/apk/release/
```

Requires JDK 17 and Android SDK with platform 34.
