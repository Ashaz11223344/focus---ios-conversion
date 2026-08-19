import SwiftUI
import shared

@main
struct FocusApp: App {
    init() {
        // Initialize KMP Koin Dependency Injection
        IosKoinKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
