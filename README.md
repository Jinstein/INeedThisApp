# 메모 키워드 앱 (MemoKeyword)

키워드 기반 메모 정리 및 검색 안드로이드 앱

---

## 주요 기능

### 1. 메모 작성 & 키워드 자동 추출

- 제목과 내용을 입력하면 **한국어/영어 키워드를 자동 추출**
- 불용어(조사, 접속사 등) 제거 후 의미 있는 단어만 추출
- **직접 키워드 추가** 가능 (쉼표, 공백, # 구분자 지원)
- 키워드 태그 추가/삭제 UI 제공
- 메모당 최대 20개 키워드 추출

### 2. 검색

- **키워드 검색**: 저장된 키워드 태그로 메모 검색 (부분 일치)
- **내용 검색**: 제목/본문 텍스트로 메모 검색
- 실시간 검색 결과 업데이트 (LiveData)
- 검색 모드 토글 (키워드 / 내용)
- 검색 결과 수 표시

### 3. 메모 관리

- 메모 목록 (최신순 정렬)
- 메모 수정 (기존 키워드 유지)
- 메모 삭제 (길게 누르기)

### 4. 사진 첨부

- 메모당 최대 5장 사진 첨부
- 사진 미리보기 썸네일 (수평 스크롤)
- 앱 전용 디렉토리에 사진 파일 저장

### 5. 스크린샷 감지

- 기기 스크린샷 촬영 시 자동 감지
- 스크린샷을 메모로 저장하도록 안내

### 6. 링크 콘텐츠 가져오기

- URL 입력 시 웹 페이지 제목 및 본문 자동 추출
- Jsoup을 이용한 HTML 파싱
- 최대 3,000자 본문 추출

---

## 기술 스택

| 구성요소 | 기술 |
|---|---|
| 언어 | Kotlin |
| 데이터베이스 | Room (SQLite) |
| 아키텍처 | MVVM (ViewModel + LiveData) |
| UI | Fragment + ViewBinding |
| 네비게이션 | Navigation Component |
| 레이아웃 | Material Design 3, FlexboxLayout |
| 비동기 | Kotlin Coroutines |
| HTML 파싱 | Jsoup |
| 빌드 시스템 | Gradle (KSP) |

### 주요 라이브러리 버전

| 라이브러리 | 버전 |
|---|---|
| Kotlin | 1.9.22 |
| compileSdk | 34 |
| minSdk | 24 (Android 7.0+) |
| Room | 2.6.1 |
| Navigation | 2.7.6 |
| Lifecycle (ViewModel/LiveData) | 2.7.0 |
| Coroutines | 1.7.3 |
| Material Design | 1.11.0 |
| FlexboxLayout | 3.0.0 |
| Jsoup | 1.17.2 |

---

## 아키텍처

```
UI Layer (Fragments + Adapters)
        ↕ LiveData / StateFlow
ViewModel Layer (MemoViewModel)
        ↕ suspend functions
Repository Layer (MemoRepository)
        ↕ DAO
Data Layer (Room Database)
```

MVVM 패턴을 따르며, 각 레이어의 역할:

- **Data Layer**: Room DB, DAO, Entity 정의
- **Repository**: 데이터 접근 추상화, 키워드 추출 로직
- **ViewModel**: UI 상태 관리, LiveData 노출
- **UI**: Fragment에서 ViewModel 관찰, 사용자 이벤트 처리

---

## 데이터베이스 구조 (v2)

```
memos (메모)
  id (PK), title, content, createdAt, updatedAt

keywords (키워드)
  id (PK), word

memo_keyword_cross_ref (메모-키워드 다대다 연결)
  memoId (FK), keywordId (FK)

memo_photos (사진, v2에서 추가)
  id (PK), memoId (FK, CASCADE), filePath, orderIndex
```

> v1 → v2 마이그레이션: `memo_photos` 테이블 추가

---

## 프로젝트 구조

```
app/src/main/java/com/example/memokeyword/
├── MainActivity.kt              # 메인 액티비티, 스크린샷 감지
├── data/
│   ├── Memo.kt                  # 메모 엔티티
│   ├── Keyword.kt               # 키워드 엔티티
│   ├── MemoKeywordCrossRef.kt   # 다대다 연결 엔티티
│   ├── MemoPhoto.kt             # 사진 엔티티
│   ├── MemoWithKeywords.kt      # 관계 데이터 클래스
│   ├── MemoDao.kt               # 메모 DAO
│   ├── KeywordDao.kt            # 키워드 DAO
│   ├── MemoPhotoDao.kt          # 사진 DAO
│   └── AppDatabase.kt           # Room 데이터베이스
├── repository/
│   └── MemoRepository.kt        # 데이터 접근 추상화
├── viewmodel/
│   ├── MemoViewModel.kt         # 중앙 ViewModel
│   └── MemoViewModelFactory.kt  # ViewModel 팩토리
├── ui/
│   ├── MemoListFragment.kt      # 메모 목록 화면
│   ├── MemoEditFragment.kt      # 메모 작성/편집 화면
│   ├── SearchFragment.kt        # 검색 화면
│   └── adapter/
│       ├── MemoAdapter.kt           # 메모 목록 어댑터
│       ├── KeywordChipAdapter.kt    # 키워드 칩 어댑터
│       └── PhotoThumbnailAdapter.kt # 사진 썸네일 어댑터
└── util/
    ├── KeywordExtractor.kt      # 키워드 자동 추출
    └── LinkContentFetcher.kt    # URL 콘텐츠 가져오기
```

---

## 권한

앱이 요청하는 Android 권한:

| 권한 | 용도 |
|---|---|
| `INTERNET` | 링크 콘텐츠 가져오기 |
| `READ_MEDIA_IMAGES` | 사진 첨부 (Android 13+) |
| `READ_EXTERNAL_STORAGE` | 사진 첨부 (Android 12 이하) |

---

## 빌드 방법

Android Studio에서 프로젝트를 열거나 아래 명령어로 빌드하세요.

```bash
# 디버그 APK 빌드
./gradlew assembleDebug

# 릴리즈 APK 빌드
./gradlew assembleRelease

# 테스트 실행
./gradlew test

# 전체 빌드 및 테스트
./gradlew build
```

### 요구 사항

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17 이상
- Android SDK 34

---

## 라이선스

MIT License - 2026 Jinstein
