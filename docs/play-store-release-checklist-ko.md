# 구독체크 업데이트 출시 체크리스트

이미 스토어에 올라간 앱의 새 버전을 낼 때 위에서부터 차례로 확인한다.
현재 준비 중인 버전: **1.1.0 (versionCode 3)**, 스토어 버전 1.0.1 (versionCode 2).

## 1. 빌드 전
- [x] `app/build.gradle.kts`의 `versionCode`를 스토어 버전보다 크게, `versionName` 갱신
- [x] `targetSdk`가 Play 요구 수준인지 확인 (현재 36, 마감 2026-11-01)
- [x] 단위 테스트 `./gradlew testDebugUnitTest`, lint `./gradlew lintDebug` 오류 0
- [ ] DB 스키마가 바뀌었으면 기기에서 마이그레이션 테스트 `./gradlew connectedDebugAndroidTest`

## 2. 서명된 AAB 만들기
- [x] `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
- [x] 업로드 키로 서명됐는지 확인 ([release-signing-ko.md](release-signing-ko.md)의 "서명 확인")
- [x] 릴리즈 APK(`assembleRelease`)를 에뮬레이터에 설치해 시작 → 추가 → 저장 → 홈·캘린더·설정 흐름에서 크래시 없는지 확인
- [ ] 실제 기기에서 알림 권한 요청과 알림 수신 확인 (스토어 버전이 깔린 기기는 서명이 달라 덮어 설치가 안 되므로 내부 테스트 트랙으로 받아 확인)

## 3. 업로드 키 (1.1.0 한정)
- [x] 2026-10-01에 요청한 업로드 키 재설정이 승인됐는지 확인. 승인 전에는 새 키로 서명한 AAB가 거부된다.

## 4. Play Console 앱 콘텐츠
이번 버전에 Firebase(Analytics·Crashlytics·Performance)가 들어갔다.
- [x] Data safety: 대략적 위치·앱 상호작용·진단·기기 ID 수집, 제3자 공유 예
- [x] Data safety: '비정상 종료 로그'를 선택 → **필수**로 변경 (사용자가 끌 수 없게 항상 수집)
- [ ] 개인정보처리방침: Google Sites 페이지를 [privacy-policy-ko.md](privacy-policy-ko.md) 최신본(시행일 2026-10-06)으로 다시 게시
- [ ] 앱 콘텐츠 → 개인정보처리방침 URL 등록(주소에 `authuser` 붙지 않게)
- [ ] 광고 포함: 예 (홈 배너) — 변경 없음

## 5. 스토어 등록정보
- [x] 짧은 설명·전체 설명: [play-store-copy-ko.md](play-store-copy-ko.md)
- [x] 그래픽(아이콘, 그래픽 이미지, 스크린샷 6장)과 대체 텍스트: [store/](store/README.md)

## 6. 출시
- [ ] 내부 테스트 트랙에 AAB 업로드 → 테스트 기기에서 설치·실행 확인
- [x] 출시 노트 입력 ([play-store-copy-ko.md](play-store-copy-ko.md)의 "출시 노트")
- [ ] 프로덕션으로 승격, 단계적 출시 비율 결정
- [x] 게시 개요에서 "검토를 위해 변경사항 전송" (스토어 등록정보 변경과 함께 보낼 수 있음) — 2026-10-06 프로덕션 검토 요청

## 7. 출시 후
- [ ] Firebase Crashlytics에 첫 보고서가 들어오는지 확인
- [ ] Play Console 출시 대시보드의 권장 조치 확인 (edge-to-edge 경고 2건이 사라졌는지)
- [ ] 출시한 커밋에 태그 달기 (예: `git tag v1.1.0`)
