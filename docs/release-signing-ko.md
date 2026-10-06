# 구독체크 출시 서명 설정

플레이스토어에 업로드하는 AAB는 반드시 서명되어 있어야 합니다.

현재 프로젝트는 두 가지 방식으로 release 서명을 할 수 있습니다.

- Android Studio의 `Generate Signed Bundle / APK` 마법사 사용
- 루트 폴더의 `keystore.properties` 파일을 사용한 Gradle 빌드

## 1. 업로드 키스토어 생성

아래 명령으로 업로드 키를 만듭니다.

```powershell
keytool -genkeypair -v `
  -keystore release-keystore.jks `
  -alias upload `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000
```

명령 실행 중 아래 값을 입력하게 됩니다.
- 키스토어 비밀번호
- 키 별칭 비밀번호
- 이름/조직/국가 코드

## 2. keystore.properties 만들기

이 단계는 Android Studio 마법사가 아니라 터미널에서 `bundleRelease`를 직접 실행할 때만 필요합니다.

루트 폴더에서 `keystore.properties.example`을 복사해 `keystore.properties`를 만듭니다.

예시:

```properties
storeFile=release-keystore.jks
storePassword=여기에_키스토어_비밀번호
keyAlias=upload
keyPassword=여기에_키_비밀번호
```

## 3. 서명된 AAB 생성

```powershell
./gradlew.bat bundleRelease
```

완성된 파일:

`app/build/outputs/bundle/release/app-release.aab`

## 4. Play App Signing

Play Console에서 앱을 처음 업로드할 때는 일반적으로 `Google Play App Signing`을 사용합니다.
이 경우 지금 만든 키는 `업로드 키` 역할을 합니다.

## 5. 서명 확인

업로드 전에 AAB가 Play에 등록된 업로드 키로 서명됐는지 SHA-1을 비교한다.

```powershell
# 빌드한 AAB의 인증서
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab

# 키스토어의 인증서 (비밀번호를 물어본다)
keytool -list -v -keystore release-keystore.jks -alias upload
```

두 SHA-1이 같고, Play Console → 테스트 및 출시 → 설정 → 앱 무결성 → 앱 서명의
"업로드 키 인증서" SHA-1과도 같아야 한다.

## 6. 업로드 키를 잃어버렸을 때

Play App Signing을 쓰고 있으면 앱 서명 키는 Google이 보관하므로, 업로드 키만 새로 만들어 교체할 수 있다.
(2026-10-01에 비밀번호 분실로 한 번 진행했다.)

1. 1번 명령으로 새 업로드 키스토어를 만든다.
2. 새 키의 인증서를 PEM으로 내보낸다.

   ```powershell
   keytool -export -rfc -keystore release-keystore.jks -alias upload -file upload_certificate.pem
   ```

3. Play Console → 테스트 및 출시 → 설정 → 앱 무결성 → 앱 서명 → **업로드 키 재설정 요청**에서
   사유를 고르고 `upload_certificate.pem`을 올린다.
4. 승인 메일이 오고, 안내된 날짜부터 새 키로 서명한 AAB를 올릴 수 있다. 그 전에는 업로드가 거부된다.
5. `keystore.properties`의 비밀번호를 새 값으로 바꾸고, 5번 방법으로 서명을 확인한다.

`upload_certificate.pem`은 공개 인증서라 비밀은 아니지만 저장소에는 올리지 않는다(`.gitignore`에 포함).

## 주의사항
- `release-keystore.jks`와 `keystore.properties`는 분실하지 않도록 안전하게 보관합니다.
- 비밀번호를 잊어버리면 6번처럼 업로드 키를 재설정해야 하고, 승인까지 며칠 동안 업데이트를 올릴 수 없습니다.
- 키스토어와 비밀번호는 비밀번호 관리자 등 컴퓨터 밖 안전한 곳에도 보관합니다.
- `keystore.properties`는 Git에 올리지 않습니다.
