import Foundation

struct Word: Equatable {
    let picture: String
    let english: String
    let korean: String
    var alternatives: [String] = []

    var letters: String { english.lowercased().replacingOccurrences(of: " ", with: "") }
    var accepted: [String] { ([english] + alternatives).map { $0.lowercased().replacingOccurrences(of: " ", with: "") } }
}

enum Level: String, CaseIterable, Identifiable {
    case easy, mid, hard
    var id: String { rawValue }
    var label: String {
        switch self {
        case .easy: return "쉬움"
        case .mid: return "보통"
        case .hard: return "어려움"
        }
    }
    var help: String {
        switch self {
        case .easy: return "3–5글자 · 일상 사물과 동물"
        case .mid: return "6–8글자 · 조금 긴 단어"
        case .hard: return "8글자 이상 · 과학, 자연, 두 단어"
        }
    }
}

enum Words {
    static let easy: [Word] = [
        Word(picture: "🍎", english: "apple", korean: "사과"),
        Word(picture: "🐶", english: "dog", korean: "개"),
        Word(picture: "🐱", english: "cat", korean: "고양이"),
        Word(picture: "🚗", english: "car", korean: "자동차"),
        Word(picture: "☀️", english: "sun", korean: "태양"),
        Word(picture: "🌙", english: "moon", korean: "달"),
        Word(picture: "🐟", english: "fish", korean: "물고기"),
        Word(picture: "🏠", english: "house", korean: "집", alternatives: ["home"]),
        Word(picture: "🔑", english: "key", korean: "열쇠"),
        Word(picture: "📚", english: "book", korean: "책", alternatives: ["books"]),
        Word(picture: "⚽", english: "ball", korean: "공"),
        Word(picture: "🐻", english: "bear", korean: "곰"),
        Word(picture: "🍰", english: "cake", korean: "케이크"),
        Word(picture: "🦆", english: "duck", korean: "오리"),
        Word(picture: "🌳", english: "tree", korean: "나무"),
        Word(picture: "👟", english: "shoe", korean: "신발", alternatives: ["shoes", "sneaker"]),
        Word(picture: "🐸", english: "frog", korean: "개구리"),
        Word(picture: "🥚", english: "egg", korean: "달걀"),
        Word(picture: "🦉", english: "owl", korean: "부엉이"),
    ]
    static let mid: [Word] = [
        Word(picture: "🦒", english: "giraffe", korean: "기린"),
        Word(picture: "🐧", english: "penguin", korean: "펭귄"),
        Word(picture: "🚲", english: "bicycle", korean: "자전거", alternatives: ["bike"]),
        Word(picture: "🌂", english: "umbrella", korean: "우산"),
        Word(picture: "🎸", english: "guitar", korean: "기타"),
        Word(picture: "🐢", english: "turtle", korean: "거북이", alternatives: ["tortoise"]),
        Word(picture: "✈️", english: "airplane", korean: "비행기", alternatives: ["plane"]),
        Word(picture: "🧲", english: "magnet", korean: "자석"),
        Word(picture: "🦋", english: "butterfly", korean: "나비"),
        Word(picture: "🥕", english: "carrot", korean: "당근"),
        Word(picture: "🍌", english: "banana", korean: "바나나"),
        Word(picture: "🐘", english: "elephant", korean: "코끼리"),
        Word(picture: "🍉", english: "watermelon", korean: "수박"),
    ]
    static let hard: [Word] = [
        Word(picture: "🔭", english: "telescope", korean: "망원경"),
        Word(picture: "🌋", english: "volcano", korean: "화산"),
        Word(picture: "🦖", english: "dinosaur", korean: "공룡"),
        Word(picture: "🧭", english: "compass", korean: "나침반"),
        Word(picture: "🚁", english: "helicopter", korean: "헬리콥터"),
        Word(picture: "🐙", english: "octopus", korean: "문어"),
        Word(picture: "🦔", english: "hedgehog", korean: "고슴도치"),
        Word(picture: "🥦", english: "broccoli", korean: "브로콜리"),
        Word(picture: "🔬", english: "microscope", korean: "현미경"),
        Word(picture: "🎡", english: "ferris wheel", korean: "대관람차"),
        Word(picture: "🍓", english: "strawberry", korean: "딸기"),
        Word(picture: "🦩", english: "flamingo", korean: "플라밍고"),
    ]

    static func list(_ level: Level) -> [Word] {
        switch level {
        case .easy: return easy
        case .mid: return mid
        case .hard: return hard
        }
    }

    static func random(_ level: Level, except: Word? = nil) -> Word {
        let l = list(level)
        guard l.count > 1 else { return l[0] }
        var w: Word
        repeat { w = l.randomElement()! } while w == except
        return w
    }
}
