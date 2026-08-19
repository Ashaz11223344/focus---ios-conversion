import SwiftUI
import SharedFocus

struct ContentView: View {
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            QuoteFeedView()
                .tabItem {
                    Label("Daily", systemImage: "sparkles")
                }
                .tag(0)

            MoodTrackerView()
                .tabItem {
                    Label("Mood", systemImage: "heart.fill")
                }
                .tag(1)

            JournalListView()
                .tabItem {
                    Label("Journal", systemImage: "book.closed.fill")
                }
                .tag(2)

            FocusGuardView()
                .tabItem {
                    Label("Focus", systemImage: "shield.fill")
                }
                .tag(3)

            AchievementsListView()
                .tabItem {
                    Label("Badges", systemImage: "trophy.fill")
                }
                .tag(4)
        }
        .accentColor(Color(hex: "#6C5CE7"))
    }
}

// MARK: - Feature Views

struct QuoteFeedView: View {
    @State private var quoteText: String = "Believe you can and you're halfway there."
    @State private var category: String = "Motivation"
    @State private var isFavorite: Bool = false

    var body: some View {
        NavigationView {
            ZStack {
                Color.black.edgesIgnoringSafeArea(.all)
                VStack(spacing: 24) {
                    Spacer()
                    Text(quoteText)
                        .font(.system(size: 24, weight: .bold, design: .serif))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)

                    Text(category.uppercased())
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundColor(Color.gray)
                        .tracking(2)

                    Spacer()

                    HStack(spacing: 40) {
                        Button(action: {
                            isFavorite.toggle()
                        }) {
                            Image(systemName: isFavorite ? "heart.fill" : "heart")
                                .font(.title2)
                                .foregroundColor(isFavorite ? .red : .white)
                        }

                        Button(action: {
                            // Generate new quote
                        }) {
                            Image(systemName: "arrow.clockwise")
                                .font(.title2)
                                .foregroundColor(.white)
                        }
                    }
                    .padding(.bottom, 40)
                }
            }
            .navigationTitle("Focus")
            .navigationBarHidden(true)
        }
    }
}

struct MoodTrackerView: View {
    @State private var selectedMood: Int = 5
    let moods = [("😢", "Down", 1), ("😕", "Low", 2), ("😐", "Neutral", 3), ("😊", "Good", 4), ("🔥", "Great", 5)]

    var body: some View {
        NavigationView {
            ZStack {
                Color.black.edgesIgnoringSafeArea(.all)
                VStack(spacing: 24) {
                    Text("How are you feeling today?")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    HStack(spacing: 16) {
                        ForEach(moods, id: \.2) { emoji, label, value in
                            Button(action: { selectedMood = value }) {
                                VStack {
                                    Text(emoji).font(.system(size: 40))
                                    Text(label).font(.caption).foregroundColor(.gray)
                                }
                                .padding()
                                .background(selectedMood == value ? Color.purple.opacity(0.3) : Color.clear)
                                .cornerRadius(12)
                            }
                        }
                    }
                    .padding()

                    Button(action: {
                        // Log mood using shared MoodRepository
                    }) {
                        Text("Log Mood")
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.purple)
                            .cornerRadius(12)
                    }
                    .padding(.horizontal, 32)
                }
            }
            .navigationTitle("Mood Tracker")
        }
    }
}

struct JournalListView: View {
    var body: some View {
        NavigationView {
            ZStack {
                Color.black.edgesIgnoringSafeArea(.all)
                VStack {
                    Text("No journal entries yet")
                        .foregroundColor(.gray)
                }
            }
            .navigationTitle("Journal")
        }
    }
}

struct FocusGuardView: View {
    var body: some View {
        NavigationView {
            ZStack {
                Color.black.edgesIgnoringSafeArea(.all)
                VStack {
                    Text("DND Schedules & Block Rules")
                        .foregroundColor(.gray)
                }
            }
            .navigationTitle("Focus Guard")
        }
    }
}

struct AchievementsListView: View {
    var body: some View {
        NavigationView {
            ZStack {
                Color.black.edgesIgnoringSafeArea(.all)
                VStack {
                    Text("Achievements & Streaks")
                        .foregroundColor(.gray)
                }
            }
            .navigationTitle("Badges")
        }
    }
}

extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex.replacingOccurrences(of: "#", with: ""))
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}
