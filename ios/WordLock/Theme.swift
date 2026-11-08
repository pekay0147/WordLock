import SwiftUI

enum Theme {
    static let sand = Color(red: 0.90, green: 0.83, blue: 0.70)
    static let ink = Color(red: 0.06, green: 0.09, blue: 0.10)
    static let mint = Color(red: 0.75, green: 0.94, blue: 0.84)

    // Settings screen, light and dark.
    static let bg = Color(light: .init(red: 0.953, green: 0.949, blue: 0.937), dark: .init(red: 0.055, green: 0.059, blue: 0.063))
    static let panel = Color(light: .init(red: 0.984, green: 0.980, blue: 0.973), dark: .init(red: 0.082, green: 0.086, blue: 0.094))
    static let fg = Color(light: .init(red: 0.09, green: 0.10, blue: 0.106), dark: .init(red: 0.925, green: 0.922, blue: 0.91))
    static let muted = Color(light: .init(red: 0.435, green: 0.447, blue: 0.459), dark: .init(red: 0.553, green: 0.561, blue: 0.573))
    static let line = Color(light: .init(red: 0.882, green: 0.875, blue: 0.855), dark: .init(red: 0.145, green: 0.153, blue: 0.165))
    static let ok = Color(light: .init(red: 0.18, green: 0.54, blue: 0.37), dark: .init(red: 0.36, green: 0.78, blue: 0.58))
}

extension Color {
    init(light: UIColor, dark: UIColor) {
        self.init(UIColor { $0.userInterfaceStyle == .dark ? dark : light })
    }
}

/// The lock-screen-like background used behind the quiz.
struct Wallpaper: View {
    var body: some View {
        GeometryReader { g in
            let w = g.size.width, h = g.size.height
            ZStack {
                LinearGradient(colors: [Color(red: 0.11, green: 0.16, blue: 0.17), Theme.ink, Color(red: 0.04, green: 0.06, blue: 0.07)],
                               startPoint: .top, endPoint: .bottom)
                RadialGradient(colors: [Theme.sand.opacity(0.5), .clear],
                               center: UnitPoint(x: 0.72, y: 0.16), startRadius: 0, endRadius: w * 0.8)
                RadialGradient(colors: [Color(red: 0.27, green: 0.41, blue: 0.41).opacity(0.55), .clear],
                               center: UnitPoint(x: 0.05, y: 0.58), startRadius: 0, endRadius: w * 0.75)
                RadialGradient(colors: [Color(red: 0.16, green: 0.23, blue: 0.27).opacity(0.9), .clear],
                               center: UnitPoint(x: 0.85, y: 1), startRadius: 0, endRadius: max(w, h * 0.5))
            }
        }
        .ignoresSafeArea()
    }
}

struct ShakeEffect: GeometryEffect {
    var amount: CGFloat = 9
    var animatableData: CGFloat
    func effectValue(size: CGSize) -> ProjectionTransform {
        ProjectionTransform(CGAffineTransform(translationX: amount * sin(animatableData * .pi * 4), y: 0))
    }
}
