import SwiftUI

struct QuizView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

    let level: Level
    @State private var word: Word
    @State private var typed = ""
    @State private var revealed = 0
    @State private var misses = 0
    @State private var shakes: CGFloat = 0
    @State private var usedHint = false
    @State private var message = ""
    @State private var solved = false
    @State private var showSolved = false
    @State private var toast = false
    @FocusState private var focused: Bool

    init(level: Level) {
        self.level = level
        _word = State(initialValue: Words.random(level))
    }

    private var maxLen: Int { word.accepted.map(\.count).max() ?? word.letters.count }

    var body: some View {
        ZStack {
            Wallpaper()

            VStack(spacing: 0) {
                HStack {
                    Button("닫기") { dismiss() }
                        .font(.system(size: 15)).foregroundStyle(.white.opacity(0.75))
                    Spacer()
                }
                .padding(.horizontal, 22).padding(.top, 8)

                Spacer()

                Text(word.picture)
                    .font(.system(size: 92))
                    .id(word.english)
                    .transition(.scale(scale: 0.7).combined(with: .opacity))

                Text("이 그림을 영어로")
                    .font(.system(size: 13)).tracking(1)
                    .foregroundStyle(.white.opacity(0.65))
                    .padding(.top, 16)

                tiles
                    .modifier(ShakeEffect(animatableData: shakes))
                    .padding(.top, 16)
                    .contentShape(Rectangle())
                    .onTapGesture { focused = true }

                Text(message.isEmpty ? " " : message)
                    .font(.system(size: 13)).foregroundStyle(.white.opacity(0.8))
                    .multilineTextAlignment(.center)
                    .padding(.top, 14)

                HStack(spacing: 24) {
                    link("힌트", action: hint)
                    link("흔들어서 바꾸기", action: changeWord)
                }
                .padding(.top, 10)

                Spacer()

                Button(action: submit) {
                    Text("확인")
                        .font(.system(size: 17, weight: .semibold))
                        .frame(maxWidth: .infinity).padding(.vertical, 15)
                        .background(Theme.sand, in: RoundedRectangle(cornerRadius: 14))
                        .foregroundStyle(Color(red: 0.1, green: 0.09, blue: 0.07))
                }
                .padding(.horizontal, 22).padding(.bottom, 12)

                // Hidden field that drives the system keyboard; the tiles show what's typed.
                TextField("", text: $typed)
                    .keyboardType(.asciiCapable)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.done)
                    .focused($focused)
                    .onSubmit(submit)
                    .frame(width: 1, height: 1)
                    .opacity(0.01)
                    .onChange(of: typed) { v in
                        let clean = String(v.lowercased().filter { $0.isLetter && $0.isASCII }.prefix(maxLen))
                        if clean != v { typed = clean }
                    }
            }
            .foregroundStyle(.white)

            if toast {
                VStack {
                    Text("새 문제로 바뀌었어요")
                        .font(.system(size: 13))
                        .padding(.horizontal, 16).padding(.vertical, 8)
                        .background(.ultraThinMaterial, in: Capsule())
                        .environment(\.colorScheme, .dark)
                        .padding(.top, 8)
                    Spacer()
                }
                .transition(.opacity)
            }

            if showSolved {
                ZStack {
                    Color.black.opacity(0.55).ignoresSafeArea()
                    VStack(spacing: 6) {
                        Text(word.picture).font(.system(size: 64))
                        Text(word.english).font(.system(size: 44, weight: .light)).tracking(-1)
                        Text(word.korean).font(.system(size: 16)).foregroundStyle(.white.opacity(0.7))
                        Text("\(model.minutes)분 동안 열려요")
                            .font(.system(size: 13)).foregroundStyle(Theme.sand).padding(.top, 14)
                    }
                    .foregroundStyle(.white)
                }
                .transition(.opacity)
            }
        }
        .onAppear { focused = true }
        .onReceive(NotificationCenter.default.publisher(for: .deviceShaken)) { _ in changeWord() }
        .task(id: toast) {
            if toast { try? await Task.sleep(nanoseconds: 1_300_000_000); withAnimation { toast = false } }
        }
    }

    private var tiles: some View {
        let letters = Array(word.letters)
        let count = max(letters.count, typed.count)
        var gaps = Set<Int>()
        var i = 0
        for c in word.english { if c == " " { gaps.insert(i) } else { i += 1 } }
        let w: CGFloat = count <= 6 ? 40 : (count <= 9 ? 31 : 25)
        let chars = Array(typed)

        return HStack(spacing: 5) {
            ForEach(0..<count, id: \.self) { idx in
                if gaps.contains(idx) { Spacer().frame(width: 6) }
                let c: Character? = idx < chars.count ? chars[idx] : nil
                let cursor = idx == chars.count && !solved
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(solved ? Theme.mint.opacity(0.28) : .white.opacity(c == nil ? 0.08 : 0.18))
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(solved ? Theme.mint.opacity(0.7) : (cursor ? Theme.sand : .white.opacity(c == nil ? 0.18 : 0.4)),
                                lineWidth: cursor ? 1.5 : 1)
                    if let c {
                        Text(String(c))
                            .font(.system(size: 21, weight: .semibold))
                            .foregroundStyle(idx < revealed && idx < letters.count && c == letters[idx] ? Theme.sand : .white)
                    } else if idx < revealed && idx < letters.count {
                        Text(String(letters[idx])).font(.system(size: 21)).foregroundStyle(Theme.sand.opacity(0.5))
                    }
                }
                .frame(width: w, height: 48)
            }
        }
    }

    private func link(_ title: String, action: @escaping () -> Void) -> some View {
        Button(title, action: action)
            .font(.system(size: 14))
            .foregroundStyle(.white.opacity(0.75))
            .padding(.vertical, 6)
    }

    private func hint() {
        let l = Array(word.letters)
        if revealed < l.count - 1 {
            revealed += 1
            usedHint = true
            if typed.count < revealed && typed == String(l.prefix(typed.count)) {
                typed = String(l.prefix(revealed - 1))
            }
            message = "글자 하나를 열었어요"
        } else {
            message = "더 열 수 있는 글자가 없어요"
        }
    }

    private func changeWord() {
        guard !solved else { return }
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        model.resetStreak()
        withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
            word = Words.random(level, except: word)
            typed = ""; revealed = 0; misses = 0; usedHint = false; message = ""
            toast = true
        }
    }

    private func submit() {
        guard !typed.isEmpty, !solved else { return }
        if word.accepted.contains(typed) {
            solved = true
            message = "정답이에요"
            UINotificationFeedbackGenerator().notificationOccurred(.success)
            model.recordSolved(word, usedHint: usedHint)
            model.unlock()
            focused = false
            Task {
                try? await Task.sleep(nanoseconds: 350_000_000)
                withAnimation { showSolved = true }
                try? await Task.sleep(nanoseconds: 1_400_000_000)
                dismiss()
            }
            return
        }
        misses += 1
        UINotificationFeedbackGenerator().notificationOccurred(.error)
        withAnimation(.linear(duration: 0.4)) { shakes += 1 }
        if misses % 3 == 0 {
            hint()
            message = "3번 틀려서 글자 하나를 열었어요"
        } else {
            message = "다시 해 보세요 · \(3 - misses % 3)번 더 틀리면 힌트"
        }
    }
}
