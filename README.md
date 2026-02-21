# Reactive Chat
Event-driven ve tamamen reaktif mimariyle geliştirilmiş gerçek zamanlı bir sohbet (chat) uygulaması.

Bu proje, modern mikroservis mimarisinin gerçek zamanlı iletişim senaryolarında nasıl uygulanabileceğini göstermek amacıyla geliştirilmiştir. Uygulama, yüksek eşzamanlı kullanıcıyı düşük kaynak tüketimiyle yönetebilmek için Spring WebFlux ve non-blocking I/O modelini temel alır.

Gerçek zamanlı mesajlaşma için WebSocket, servisler arası mesaj yayılımı için Redis Pub/Sub, kalıcı veri saklama için MongoDB, kimlik doğrulama için ise JWT kullanılmaktadır.

---

## Mimari

```
                    ┌─────────────────┐
                    │  API Gateway    │
                    │  localhost:8080 │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│ Auth Service   │  │ Chat Service   │  │Presence Service │  │ Notification │
│     :8081      │  │     :8082      │  │     :8083       │  │    :8084     │
│ Kayıt, Login,  │  │ Odalar, Mesaj, │  │ Online/Offline  │  │   Event      │
│ JWT, Refresh   │  │ WebSocket      │  │ Heartbeat       │  │   listener   │
└───────┬────────┘  └───────┬────────┘  └────────┬────────┘  └──────┬──────┘
        │                   │                    │                    │
        ▼                   ▼                    ▼                    ▼
   MongoDB (auth)      MongoDB + Redis      Redis (set)          Redis (sub)
```

- Tüm HTTP/WebSocket istekleri **Gateway (8080)** üzerinden gider; JWT doğrulama burada yapılır.
- **Auth:** Kayıt, login, access + refresh token.
- **Chat:** Oda oluşturma, mesaj geçmişi (REST), canlı mesajlaşma (WebSocket), Redis Pub/Sub.
- **Presence:** Kullanıcı online/offline, heartbeat, son görülme.
- **Notification:** Redis kanallarını dinleyerek offline bildirim mantığı.

---

## Teknoloji

| Alan        | Teknoloji                          |
|------------|-------------------------------------|
| Backend    | Spring Boot 3.x/4.x, Spring WebFlux |
| Gateway    | Spring Cloud Gateway (reaktif)      |
| Güvenlik   | JWT (JJWT 0.11), Spring Security   |
| Veritabanı | MongoDB (Reactive)                  |
| Mesajlaşma | Redis Pub/Sub                       |
| Java       | 21                                  |

---

## Gereksinimler

- **Java 21**
- **Maven 3.8+**
- **Docker & Docker Compose** (MongoDB, Redis)

---

## Hızlı Başlangıç

### 1. Altyapı

```bash
docker-compose up -d
```

### 2. Servisleri başlat

Sıra önemli değil; Gateway diğerlerine 8081–8084’ten bağlanır.

### 3. Base URL

Tüm REST istekleri **Gateway** üzerinden:

- **Base URL:** `http://localhost:8080`
- **WebSocket:** `ws://localhost:8080/ws/chat?roomId=<oda>&token=<accessToken>`

---

## Postman Collection

API ve WebSocket’i Postman ile test etmek için hazır collection:

** [apiler](https://web.postman.co/workspace/Personal-Workspace~5d4ba628-e1fc-46f3-b40e-86e8c38346b2/collection/37942261-b7dfa56a-da3d-43c8-974d-d9f56dec02b7?action=share&source=copy-link&creator=37942261
)

** [websocket](https://web.postman.co/workspace/Personal-Workspace~5d4ba628-e1fc-46f3-b40e-86e8c38346b2/collection/699981ca3d43333f1620463f?action=share&source=copy-link&creator=37942261
): 
- **Auth:** Register, Login, Refresh Token
- **Presence:** Online, Heartbeat, Offline, Status, Online listesi
- **Chat:** Oda oluştur, Benim odalarım, Mesaj geçmişi
- **WebSocket:** Bağlantı örneği (URL’de `roomId` ve `token`)

Login sonrası dönen `accessToken` değerini environment’ta saklayıp diğer isteklerde `Authorization: Bearer {{accessToken}}` kullanın.

---

## API Özeti

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| POST | `/auth/register` | Kayıt |
| POST | `/auth/login` | Login (access + refresh token) |
| POST | `/auth/refresh` | Token yenileme |
| POST | `/api/presence/online` | Online yap |
| POST | `/api/presence/heartbeat` | Heartbeat |
| POST | `/api/presence/offline` | Offline yap |
| GET | `/api/presence/{userId}/status` | Kullanıcı durumu |
| GET | `/api/presence/online` | Online kullanıcı listesi |
| POST | `/api/rooms` | Oda oluştur (body: `{"name":"..."}`) |
| GET | `/api/rooms` | Benim odalarım |
| GET | `/api/rooms/{roomId}/messages?limit=50` | Mesaj geçmişi |
| WebSocket | `/ws/chat?roomId=...&token=...` | Canlı mesaj (gönder: `{"content":"..."}` veya `{"roomId":"...","content":"..."}`) |

Tüm REST isteklerinde (login/register hariç) header: `Authorization: Bearer <accessToken>`.

---

## WebSocket

1. Bağlan: `ws://localhost:8080/ws/chat?roomId=room1&token=ACCESS_TOKEN`
2. Mesaj gönder (json): `{"content":"Merhaba"}` veya `{"roomId":"room1","content":"Merhaba"}`
3. Aynı odadaki tüm bağlı client’lar mesajı anında alır.


---




