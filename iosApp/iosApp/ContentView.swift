import SwiftUI
import UIKit
import ComposeApp

/// Hosts the shared Compose Multiplatform UI. All screens live in Kotlin;
/// this file is the entire platform-specific UI for iOS.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose handles the IME itself
    }
}
