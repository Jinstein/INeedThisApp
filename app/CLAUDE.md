# app/CLAUDE.md

이 파일은 `app` 모듈에 대한 Claude Code 가이드입니다.
루트의 `CLAUDE.md`와 함께 참조하세요.

---

## 모듈 설정 (`app/build.gradle`)

```
applicationId: com.example.memokeyword
compileSdk:    34
minSdk:        24  (Android 7.0 Nougat)
targetSdk:     34
versionCode:   1
versionName:   "1.0"
```

---

## 소스 파일 위치

모든 Kotlin 소스는 아래 경로에 있습니다:

```
app/src/main/java/com/example/memokeyword/
```

리소스는 아래 경로에 있습니다:

```
app/src/main/res/
├── layout/         # XML 레이아웃
├── navigation/     # nav_graph.xml
├── menu/           # bottom_nav_menu.xml
├── drawable/       # 배경 등 드로어블
└── values/         # strings.xml, colors.xml, themes.xml
```

---

## 데이터 레이어 (`data/`)

| 파일 | 역할 |
|---|---|
| `Memo.kt` | 메모 Room 엔티티 (`memos` 테이블) |
| `Keyword.kt` | 키워드 Room 엔티티 (`keywords` 테이블) |
| `MemoKeywordCrossRef.kt` | 메모-키워드 다대다 연결 테이블 |
| `MemoPhoto.kt` | 사진 파일 참조 엔티티 (`memo_photos` 테이블) |
| `MemoWithKeywords.kt` | 관계 쿼리 결과 데이터 클래스 |
| `MemoDao.kt` | 메모 CRUD + 키워드/내용 검색 쿼리 |
| `KeywordDao.kt` | 키워드 CRUD + 자동완성 쿼리 |
| `MemoPhotoDao.kt` | 사진 참조 CRUD |
| `AppDatabase.kt` | Room DB 싱글톤, 마이그레이션 정의 |

### 새 엔티티 추가 방법

1. `data/` 에 엔티티 파일 생성 (`@Entity` 어노테이션)
2. DAO 파일 생성 (`@Dao` 어노테이션)
3. `AppDatabase.kt`의 `@Database` 어노테이션에 엔티티 추가
4. `AppDatabase.kt`에 DAO 추상 메서드 추가
5. DB 버전 올리고 마이그레이션 작성

---

## UI 레이어 (`ui/`)

### Fragment

| 파일 | 화면 |
|---|---|
| `MemoListFragment.kt` | 메모 목록 (홈) |
| `MemoEditFragment.kt` | 메모 작성 / 편집 |
| `SearchFragment.kt` | 키워드 / 내용 검색 |

### Adapter

| 파일 | 역할 |
|---|---|
| `MemoAdapter.kt` | RecyclerView - 메모 목록 아이템 |
| `KeywordChipAdapter.kt` | FlexboxRecyclerView - 키워드 칩 |
| `PhotoThumbnailAdapter.kt` | RecyclerView - 사진 썸네일 (수평) |

### Fragment 네비게이션

`res/navigation/nav_graph.xml`에 정의되어 있으며, Bottom Navigation과 연동됩니다.

- `MemoListFragment` → `MemoEditFragment` (FAB 클릭, 메모 클릭)
- `SearchFragment` → `MemoEditFragment` (검색 결과 클릭)

---

## 레이아웃 파일 (`res/layout/`)

| 파일 | 용도 |
|---|---|
| `activity_main.xml` | MainActivity 레이아웃 |
| `fragment_memo_list.xml` | 메모 목록 Fragment |
| `fragment_memo_edit.xml` | 메모 작성/편집 Fragment |
| `fragment_search.xml` | 검색 Fragment |
| `item_memo.xml` | 메모 목록 아이템 |
| `item_keyword_chip.xml` | 키워드 칩 아이템 |
| `item_photo_thumbnail.xml` | 사진 썸네일 아이템 |

---

## 권한 처리

`AndroidManifest.xml`에 선언된 권한:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

- `READ_MEDIA_IMAGES`: Android 13 (API 33) 이상
- `READ_EXTERNAL_STORAGE`: Android 12 (API 32) 이하 (`maxSdkVersion="32"`)
- 런타임 권한 요청은 `MemoEditFragment` 또는 `MainActivity`에서 처리

---

## 스크린샷 감지 (`MainActivity.kt`)

`ContentObserver`로 `MediaStore`의 이미지를 모니터링합니다.

- 새 이미지 파일 경로에 "screenshot" 포함 여부 감지
- 감지 시 사용자에게 메모 저장 여부 다이얼로그 표시
- 권한 필요: `READ_MEDIA_IMAGES` 또는 `READ_EXTERNAL_STORAGE`

---

## 테마 및 스타일

`res/values/themes.xml`에서 Material Design 3 테마 사용:

- 기본 테마: `Theme.MemoKeyword` (Material3)
- 색상 정의: `res/values/colors.xml`
- 키워드 칩 배경: `res/drawable/bg_keyword_chip.xml`

---

## ProGuard (`proguard-rules.pro`)

릴리즈 빌드 시 R8/ProGuard 규칙이 적용됩니다.
Room, Kotlin Coroutines, Jsoup 관련 규칙이 필요할 수 있습니다.
릴리즈 빌드 후 크래시 발생 시 이 파일을 확인하세요.

---

## 자주 하는 작업

### 새 Fragment 추가

1. `ui/` 에 Fragment 파일 생성
2. `res/layout/` 에 레이아웃 XML 생성
3. `res/navigation/nav_graph.xml` 에 destination 추가
4. 필요시 `res/menu/bottom_nav_menu.xml` 업데이트

### 새 DAO 쿼리 추가

1. DAO 파일에 `@Query` 어노테이션으로 메서드 추가
2. `MemoRepository.kt`에 Repository 메서드 추가
3. `MemoViewModel.kt`에 ViewModel 메서드 및 LiveData 추가
4. Fragment에서 `observe` 또는 `viewLifecycleOwner.lifecycleScope` 사용

### 의존성 추가

`app/build.gradle`의 `dependencies` 블록에 추가:

```gradle
// 예: Glide 이미지 라이브러리
implementation 'com.github.bumptech.glide:glide:4.16.0'
ksp 'com.github.bumptech.glide:ksp:4.16.0'  // KSP 사용 시
```
