import SwiftUI

@main
struct FridayApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ChatMessage: Identifiable {
    let id = UUID()
    let role: String // "user" or "assistant"
    let text: String
}

private let BACKEND_URL = "https://YOUR-BACKEND-URL.example.com"

// Friday visual identity
private let bgColor = Color(red: 0.059, green: 0.078, blue: 0.125)
private let surfaceColor = Color(red: 0.086, green: 0.114, blue: 0.180)
private let amberColor = Color(red: 0.949, green: 0.663, blue: 0.231)
private let textPrimary = Color.white
private let textMuted = Color(red: 0.545, green: 0.573, blue: 0.659)

struct ContentView: View {
    @State private var messages: [ChatMessage] = []
    @State private var input: String = ""
    @State private var loading = false
    @FocusState private var inputFocused: Bool

    var body: some View {
        ZStack {
            bgColor.ignoresSafeArea()
            VStack(spacing: 0) {
                header
                quickActionsRow
                chatList
                inputBar
            }
        }
        .onOpenURL { url in
            // Handles the widget's widgetURL("friday://quick-chat") deep link
            if url.absoluteString.contains("quick-chat") {
                inputFocused = true
            }
        }
    }

    private var header: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("Friday").font(.system(size: 17, weight: .semibold)).foregroundColor(textPrimary)
                Text("Quick chat").font(.system(size: 11)).foregroundColor(textMuted)
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 8)
    }

    private var quickActionsRow: some View {
        HStack(spacing: 10) {
            quickActionButton(icon: "flashlight.on.fill", label: "Flashlight") {
                FridayQuickActions.toggleFlashlight()
            }
            quickActionButton(icon: "gearshape.fill", label: "Settings") {
                FridayQuickActions.openSettingsApp()
            }
            quickActionButton(icon: "camera.fill", label: "Camera") {
                _ = FridayQuickActions.openApp(scheme: "camera://")
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
    }

    private func quickActionButton(icon: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon).foregroundColor(amberColor).font(.system(size: 18))
                Text(label).font(.system(size: 10)).foregroundColor(textMuted)
            }
            .padding(10)
            .background(surfaceColor)
            .cornerRadius(12)
        }
    }

    private var chatList: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 10) {
                    ForEach(messages) { msg in
                        chatBubble(msg).id(msg.id)
                    }
                    if loading {
                        Text("Friday is working…")
                            .font(.system(size: 12))
                            .foregroundColor(textMuted)
                            .padding(.horizontal, 4)
                    }
                }
                .padding(16)
            }
            .onChange(of: messages.count) { _ in
                if let last = messages.last { proxy.scrollTo(last.id) }
            }
        }
    }

    private func chatBubble(_ msg: ChatMessage) -> some View {
        HStack {
            if msg.role == "user" { Spacer(minLength: 40) }
            Text(msg.text)
                .font(.system(size: 13.5))
                .foregroundColor(msg.role == "user" ? bgColor : textPrimary)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(msg.role == "user" ? amberColor : surfaceColor)
                .cornerRadius(16)
            if msg.role != "user" { Spacer(minLength: 40) }
        }
    }

    private var inputBar: some View {
        HStack(spacing: 8) {
            TextField("Message Friday…", text: $input)
                .focused($inputFocused)
                .padding(10)
                .background(surfaceColor)
                .cornerRadius(12)
                .foregroundColor(textPrimary)
                .onSubmit { send() }

            Button(action: send) {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 28))
                    .foregroundColor(input.isEmpty || loading ? textMuted : amberColor)
            }
            .disabled(input.isEmpty || loading)
        }
        .padding(12)
    }

    private func send() {
        guard !input.isEmpty, !loading else { return }
        let userText = input
        messages.append(ChatMessage(role: "user", text: userText))
        input = ""
        loading = true

        callChat(message: userText, history: messages) { reply in
            DispatchQueue.main.async {
                messages.append(ChatMessage(role: "assistant", text: reply))
                loading = false
            }
        }
    }

    private func callChat(message: String, history: [ChatMessage], completion: @escaping (String) -> Void) {
        guard let url = URL(string: "\(BACKEND_URL)/chat") else {
            completion("Backend URL not set.")
            return
        }
        let historyPayload = history.map { ["role": $0.role, "content": $0.text] }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: [
            "message": message,
            "history": historyPayload
        ])

        URLSession.shared.dataTask(with: request) { data, _, error in
            guard let data = data, error == nil,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let reply = json["reply"] as? String else {
                completion("Offline — check your connection or backend URL.")
                return
            }
            completion(reply)
        }.resume()
    }
}
