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

## 주의사항
- `release-keystore.jks`와 `keystore.properties`는 분실하지 않도록 안전하게 보관합니다.
- 비밀번호를 잊어버리면 업데이트 배포가 번거로워질 수 있습니다.
- `keystore.properties`는 Git에 올리지 않습니다.
