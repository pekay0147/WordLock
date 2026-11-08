# Word Lock

화면을 켤 때마다 그림이 나오고, 그 그림의 영어 단어를 맞혀야 열리는 안드로이드 잠금화면 앱.

## 기능
- 화면이 켜지면 잠금화면 위에 그림 퀴즈 표시 (자체 영어 키보드)
- 3번 틀리면 글자 힌트, 휴대폰을 흔들면 다른 문제
- 맞히면 기기 기본 잠금(PIN/지문)으로 넘어감
- 비상 PIN, 난이도 3단계, 오늘 맞힌 단어 기록

## 빌드
Android SDK(platform 35)와 JDK 17+가 필요해요.

```bash
./gradlew assembleDebug
```

결과물: `app/build/outputs/apk/debug/app-debug.apk`
