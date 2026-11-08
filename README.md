# Word Lock

그림을 보고 영어 단어를 맞혀야 열리는 앱. 안드로이드와 아이폰 두 가지 버전이 있어요.

| | 안드로이드 (루트 폴더) | 아이폰 (`ios/`) |
|---|---|---|
| 무엇을 잠그나 | 휴대폰 잠금화면 | 사용자가 고른 앱 (유튜브, 인스타그램 등) |
| 작동 방식 | 화면이 켜지면 잠금화면 위에 퀴즈 | 스크린타임으로 앱을 잠그고, 열려고 하면 퀴즈 |
| 맞히면 | 기기 기본 잠금(PIN/지문)으로 넘어감 | 15/30/60분 동안 앱이 열림 |

공통: 3번 틀리면 글자 힌트, 휴대폰을 흔들면 다른 문제, 난이도 3단계, 오늘 맞힌 단어 기록.

## 안드로이드
Android SDK(platform 35)와 JDK 17+가 필요해요.

```bash
./gradlew assembleDebug
```

결과물: `app/build/outputs/apk/debug/app-debug.apk`

## 아이폰
SwiftUI + Screen Time API(FamilyControls, ManagedSettings, DeviceActivity). iOS 16 이상.

구성:
- `WordLock` 앱: 권한 요청, 잠글 앱 고르기, 퀴즈
- `WordLockShieldConfig`: 잠긴 앱을 열 때 나오는 화면
- `WordLockShieldAction`: 그 화면의 "퀴즈 풀기" 버튼 → 알림 → 앱의 퀴즈로 이동
- `WordLockMonitor`: 열어 둔 시간이 끝나면 다시 잠금

빌드 (Mac):

```bash
brew install xcodegen
cd ios && xcodegen generate && open WordLock.xcodeproj
```

실제 기기에서 돌리고 앱스토어에 올리려면:
1. 애플 개발자 계정 (연 $99)
2. `ios/project.yml`의 `DEVELOPMENT_TEAM`에 Team ID 입력
3. App Group `group.com.wordlock.app` 등록
4. Family Controls 배포 권한 신청: https://developer.apple.com/contact/request/family-controls-distribution
   (개발용 기기 테스트는 신청 없이도 됨)

GitHub Actions(`.github/workflows/ios.yml`)가 push할 때마다 서명 없이 시뮬레이터용으로 빌드해서 코드가 컴파일되는지 확인해요.
