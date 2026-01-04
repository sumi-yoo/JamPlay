<h1 align="center">🎸 JamPlay - 무료 인디 음악 스트리밍</h1>

---

## 🚀 정식 출시 · 바로 사용하기
**JamPlay - 무료 인디 음악 스트리밍**  
구글 플레이 스토어에 정식 출시된 앱입니다.  
지금 바로 다운로드하여 직접 사용해보세요!

👉 [구글 플레이 스토어에서 다운로드](https://play.google.com/store/apps/details?id=com.sumi.jamplay)

---

## 📱 프로젝트 설명 (App Overview)
**JamPlay**는 Jamendo 오픈 API를 활용해 다양한 무료 인디 음악을 스트리밍할 수 있는 안드로이드 앱입니다.  
로그인 없이 바로 사용 가능하며, 네트워크 연결 시 실시간으로 음악을 재생할 수 있습니다.  
깔끔하고 직관적인 UI/UX로 누구나 쉽게 음악을 즐길 수 있도록 설계되었습니다.

---

## 🚀 주요 기능 (Features)
- 🎶 **스트리밍 재생**: Jamendo API를 통해 다양한 인디 음악을 바로 재생할 수 있습니다.
- 🔄 **백그라운드 재생**: 앱을 나가거나 화면이 꺼져도 음악이 계속 재생됩니다.
- 🎛️ **재생 컨트롤**
    - 재생/일시정지, 이전/다음 트랙
    - **반복 재생**: 없음 / 전체 반복 / 1곡 반복
    - **셔플**: 순서대로 / 랜덤
    - 재생 화면과 노티피케이션에서 모두 조작 가능
- 🔎 **트랙 및 아티스트 검색**: 트랙 제목, 아티스트, 키워드로 음악을 쉽게 검색할 수 있습니다.
- 📂 **재생목록 관리**: 플레이리스트를 생성하고 좋아하는 곡을 추가하여 효율적으로 관리할 수 있습니다.
- 🖼️ **시각적 플레이어**: 앨범 아트를 활용한 다이내믹 플레이어로 화면을 아름답게 구성했습니다.
- 🌍 **다국어 지원 (한국어·영어)**: 언어 설정에 따라 앱 내 UI와 텍스트가 한국어 또는 영어로 표시됩니다.

---

## 🛠 사용 기술 (Tech Stack)
| 구분                     | 내용                                                                 |
|-------------------------|----------------------------------------------------------------------|
| **언어**                 | Kotlin                                                              |
| **프레임워크 / UI**        | Android SDK, Jetpack Compose, Material3, Navigation Compose, SwipeRefresh, Accompanist SystemUI |
| **의존성 주입**           | Hilt (Dagger)                                                        |
| **스트리밍 재생**          | ExoPlayer, Media3 UI                                                 |
| **네트워크 통신**         | Retrofit, Gson (JSON 컨버터)                                         |
| **이미지 로딩**           | Coil                                                                 |
| **로컬 저장소**            | Room, Preferences DataStore                                          |
| **알림 / 포그라운드 서비스** | NotificationCompat, MediaStyle, Foreground Service                  |
| **미디어 / 시각화**        | AndroidX Media, Palette                                              |
| **페이지네이션**            | Paging Compose, Paging Runtime                                       |
| **다국어 지원**            | 한국어·영어 지원 (앱 내 UI와 텍스트 표시)                           |

---

## 📸 화면 및 기능 소개

### 1️⃣ 검색 화면 (Search Screen)
- 상단 검색창에서 트랙 제목, 아티스트, 키워드로 음악 검색 가능
- 검색 결과는 리스트 형태로 표시
- 최근 검색어 터치 시 재검색 가능
- 트랙 선택 시 **재생 화면으로 이동**

<p float="left">
  <img src="https://github.com/user-attachments/assets/40fc2e95-b0e3-42f2-9974-c206fae72d9c" width="250" />
  <img src="https://github.com/user-attachments/assets/de338e21-4160-439e-9574-72f9c8dec563" width="250" />
</p>

### 2️⃣ 재생 화면 (Playback Screen)
- 앨범 아트를 활용한 **다이내믹 플레이어**
- 재생/일시정지, 이전/다음, 셔플/반복 기능 제공
- **백그라운드 재생 지원**

<p float="left">
  <img src="https://github.com/user-attachments/assets/845cd90f-de5d-429b-99e9-471a402cb5aa" width="250" />
</p>

### 3️⃣ 노티피케이션 재생 컨트롤
- 재생 화면을 열지 않아도 노티피케이션에서 모든 재생 컨트롤 가능
- 재생/일시정지, 이전/다음, **셔플/반복** 기능 제공
- 포그라운드 서비스와 연동되어 앱 종료/화면 꺼짐에도 제어 가능

<p float="left">
  <img src="https://github.com/user-attachments/assets/44b6de64-a401-47c2-8ddb-405ea85c63aa" width="250" />
  <img src="https://github.com/user-attachments/assets/4fe01004-739f-4088-96fc-7c528573ce23" width="250" />
</p>

### 4️⃣ 플레이리스트 화면 (Playlist Screen)
- 재생 화면에서 현재 트랙을 플레이리스트에 추가 가능
- 좋아요(즐겨찾기) 버튼으로 선택한 트랙은 별도의 좋아요 플레이리스트에 자동 저장

<p float="left">
  <img src="https://github.com/user-attachments/assets/f6c272a9-17f8-4f23-ad86-3da79a6307e7" width="250" />
  <img src="https://github.com/user-attachments/assets/dee3724b-c1f8-4cae-9acf-ffa6e11ec114" width="250" />
  <img src="https://github.com/user-attachments/assets/379eb491-a74f-4bf6-8f04-44ec3792a21c" width="250" />
</p>

---

## ✉️ 연락처 (Contact)
- 이메일: sumi.yoo.dev@gmail.com
