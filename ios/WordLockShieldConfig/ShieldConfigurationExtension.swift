import ManagedSettings
import ManagedSettingsUI
import UIKit

/// The screen iOS shows when the user opens a blocked app.
class ShieldConfigurationExtension: ShieldConfigurationDataSource {
    private let ink = UIColor(red: 0.06, green: 0.09, blue: 0.10, alpha: 1)
    private let sand = UIColor(red: 0.90, green: 0.83, blue: 0.70, alpha: 1)

    private func make() -> ShieldConfiguration {
        ShieldConfiguration(
            backgroundBlurStyle: .systemUltraThinMaterialDark,
            backgroundColor: ink,
            icon: UIImage(systemName: "character.book.closed")?.withTintColor(sand, renderingMode: .alwaysOriginal),
            title: ShieldConfiguration.Label(text: "영어 단어를 맞혀야 열려요", color: .white),
            subtitle: ShieldConfiguration.Label(
                text: "아래 버튼을 누르고 알림을 탭해 퀴즈를 풀면 바로 열려요.",
                color: UIColor(white: 1, alpha: 0.7)
            ),
            primaryButtonLabel: ShieldConfiguration.Label(text: "퀴즈 풀기", color: UIColor(red: 0.1, green: 0.09, blue: 0.07, alpha: 1)),
            primaryButtonBackgroundColor: sand,
            secondaryButtonLabel: ShieldConfiguration.Label(text: "닫기", color: .white)
        )
    }

    override func configuration(shielding application: Application) -> ShieldConfiguration { make() }
    override func configuration(shielding application: Application, in category: ActivityCategory) -> ShieldConfiguration { make() }
    override func configuration(shielding webDomain: WebDomain) -> ShieldConfiguration { make() }
    override func configuration(shielding webDomain: WebDomain, in category: ActivityCategory) -> ShieldConfiguration { make() }
}
