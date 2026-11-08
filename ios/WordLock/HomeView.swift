import SwiftUI
import FamilyControls

struct HomeView: View {
    @EnvironmentObject private var model: AppModel
    @State private var picking = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text("ENGLISH APP LOCK")
                    .font(.system(size: 11, weight: .semibold)).tracking(2)
                    .foregroundStyle(Theme.muted)
                Text("Word Lock")
                    .font(.system(size: 44, weight: .light)).tracking(-1.5)
                    .foregroundStyle(Theme.fg)
                    .padding(.top, 8)

                TimelineView(.periodic(from: .now, by: 20)) { ctx in
                    statusCard(now: ctx.date)
                }
                .padding(.top, 24)

                section("잠글 앱")
                Button { picking = true } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("앱 고르기").font(.system(size: 16)).foregroundStyle(Theme.fg)
                            Text(model.blockedCount == 0 ? "아직 고른 앱이 없어요" : "\(model.blockedCount)개 잠그는 중")
                                .font(.system(size: 13)).foregroundStyle(Theme.muted)
                        }
                        Spacer()
                        Image(systemName: "chevron.right").foregroundStyle(Theme.muted)
                    }
                    .padding(16)
                    .background(Theme.panel, in: RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(Theme.line))
                }

                section("맞히면 열어 둘 시간")
                Picker("열어 둘 시간", selection: $model.minutes) {
                    ForEach([15, 30, 60], id: \.self) { Text("\($0)분").tag($0) }
                }
                .pickerStyle(.segmented)

                section("난이도")
                Picker("난이도", selection: $model.level) {
                    ForEach(Level.allCases) { Text($0.label).tag($0) }
                }
                .pickerStyle(.segmented)
                Text(model.level.help).font(.system(size: 13)).foregroundStyle(Theme.muted).padding(.top, 8)

                section("오늘")
                HStack(spacing: 12) {
                    stat("맞힌 단어", model.todayCount)
                    stat("힌트 없이 연속", model.streak)
                }
                if model.log.isEmpty {
                    Text("아직 맞힌 단어가 없어요.").font(.system(size: 13)).foregroundStyle(Theme.muted).padding(.top, 14)
                } else {
                    VStack(spacing: 0) {
                        ForEach(Array(model.log.prefix(20).enumerated()), id: \.offset) { _, line in
                            let p = line.components(separatedBy: "\t")
                            if p.count == 4 {
                                HStack(spacing: 10) {
                                    Text(p[0])
                                    Text(p[1]).font(.system(size: 15, weight: .medium)).foregroundStyle(Theme.fg)
                                    Text(p[2]).font(.system(size: 13)).foregroundStyle(Theme.muted)
                                    Spacer()
                                    Text(p[3]).font(.system(size: 12)).foregroundStyle(Theme.muted).monospacedDigit()
                                }
                                .padding(.vertical, 10)
                                Divider().overlay(Theme.line)
                            }
                        }
                    }
                    .padding(.top, 8)
                }
            }
            .padding(.horizontal, 22)
            .padding(.vertical, 28)
        }
        .background(Theme.bg.ignoresSafeArea())
        .familyActivityPicker(isPresented: $picking, selection: $model.selection)
        .onChange(of: model.selection) { _ in model.saveSelection() }
    }

    @ViewBuilder
    private func statusCard(now: Date) -> some View {
        let unlocked = model.isUnlocked(at: now)
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                Circle().fill(model.blockedCount == 0 ? Theme.muted : (unlocked ? Theme.ok : Theme.sand)).frame(width: 8, height: 8)
                Text(model.blockedCount == 0 ? "꺼짐" : (unlocked ? "열림" : "잠김"))
                    .font(.system(size: 13, weight: .semibold)).foregroundStyle(Theme.fg)
            }
            Group {
                if model.blockedCount == 0 {
                    Text("잠글 앱을 고르면 시작돼요.")
                } else if unlocked, let u = model.unlockUntil {
                    Text("\(u.formatted(date: .omitted, time: .shortened))까지 자유롭게 쓸 수 있어요.")
                } else {
                    Text("잠근 앱을 열면 영어 퀴즈가 나와요.")
                }
            }
            .font(.system(size: 17)).foregroundStyle(Theme.fg)

            if model.blockedCount > 0 {
                Button {
                    if unlocked { model.lockNow() } else { model.showQuiz = true }
                } label: {
                    Text(unlocked ? "지금 다시 잠그기" : "퀴즈 풀고 열기")
                        .font(.system(size: 15, weight: .medium))
                        .frame(maxWidth: .infinity).padding(.vertical, 13)
                        .background(Theme.fg, in: RoundedRectangle(cornerRadius: 12))
                        .foregroundStyle(Theme.bg)
                }
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.panel, in: RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Theme.line))
    }

    private func section(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 12, weight: .semibold)).tracking(1)
            .foregroundStyle(Theme.muted)
            .padding(.top, 32).padding(.bottom, 10)
    }

    private func stat(_ label: String, _ value: Int) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(value)").font(.system(size: 32, weight: .light)).foregroundStyle(Theme.fg).monospacedDigit()
            Text(label).font(.system(size: 12)).foregroundStyle(Theme.muted)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.panel, in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Theme.line))
    }
}
