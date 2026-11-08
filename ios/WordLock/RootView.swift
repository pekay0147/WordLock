import SwiftUI

struct RootView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        Group {
            if model.authorized { HomeView() } else { OnboardingView() }
        }
        .fullScreenCover(isPresented: $model.showQuiz) {
            QuizView(level: model.level)
                .environmentObject(model)
        }
    }
}

struct OnboardingView: View {
    @EnvironmentObject private var model: AppModel
    @State private var working = false

    var body: some View {
        ZStack {
            Wallpaper()
            VStack(alignment: .leading, spacing: 0) {
                Spacer()
                Text("🦒").font(.system(size: 72))
                Text("Word Lock")
                    .font(.system(size: 48, weight: .light))
                    .tracking(-1.5)
                    .padding(.top, 12)
                Text("자주 여는 앱을 잠가 두고, 열 때마다 영어 단어를 하나씩 맞혀요.")
                    .font(.system(size: 16))
                    .foregroundStyle(.white.opacity(0.75))
                    .padding(.top, 8)

                VStack(alignment: .leading, spacing: 14) {
                    step("1", "잠글 앱을 골라요. 유튜브, 인스타그램처럼요.")
                    step("2", "그 앱을 열면 퀴즈 화면이 나와요.")
                    step("3", "맞히면 정해 둔 시간 동안 자유롭게 써요.")
                }
                .padding(.top, 32)

                Spacer()

                if let e = model.authError {
                    Text(e).font(.footnote).foregroundStyle(Theme.sand).padding(.bottom, 12)
                }
                Button {
                    working = true
                    Task { await model.requestAuthorization(); working = false }
                } label: {
                    Text(working ? "요청하는 중…" : "스크린타임 권한 허용하고 시작")
                        .font(.system(size: 17, weight: .semibold))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 17)
                        .background(Theme.sand, in: RoundedRectangle(cornerRadius: 16))
                        .foregroundStyle(Color(red: 0.1, green: 0.09, blue: 0.07))
                }
                .disabled(working)
                Text("앱 사용 기록은 휴대폰 밖으로 나가지 않아요.")
                    .font(.footnote)
                    .foregroundStyle(.white.opacity(0.55))
                    .frame(maxWidth: .infinity)
                    .padding(.top, 10)
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 24)
            .padding(.bottom, 12)
        }
    }

    private func step(_ n: String, _ text: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(n).font(.system(size: 13, weight: .medium)).foregroundStyle(Theme.sand)
            Text(text).font(.system(size: 15)).foregroundStyle(.white.opacity(0.85))
        }
    }
}
