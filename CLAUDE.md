# CLAUDE.md

이 파일은 Claude Code (AI 어시스턴트)가 이 저장소에서 작업할 때 참조하는 가이드입니다.

---

## 프로젝트 개요

**MemoKeyword**는 키워드 기반 메모 정리 및 검색 Android 앱입니다.

- **언어**: Kotlin
- **패키지**: `com.example.memokeyword`
- **아키텍처**: MVVM (ViewModel + LiveData + Repository)
- **데이터베이스**: Room (SQLite), 현재 버전 2

---

## 빌드 명령어

```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리즈 빌드
./gradlew assembleRelease

# 단위 테스트 실행
./gradlew test

# 로컬 단위 테스트만 실행
./gradlew :app:testDebugUnitTest

# 전체 빌드 + 테스트
./gradlew build

# 빌드 캐시 정리 후 빌드
./gradlew clean assembleDebug
```

---

## 프로젝트 구조

```
INeedThisApp/
├── CLAUDE.md                   # 이 파일 (루트)
├── README.md                   # 프로젝트 문서
├── build.gradle                # 루트 Gradle 설정
├── settings.gradle             # 모듈 설정
└── app/
    ├── CLAUDE.md               # 앱 모듈 가이드
    ├── build.gradle            # 앱 의존성 및 빌드 설정
    ├── proguard-rules.pro      # ProGuard 규칙
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/memokeyword/
        │   ├── MainActivity.kt
        │   ├── data/           # Room 엔티티 + DAO + Database
        │   ├── repository/     # Repository 패턴
        │   ├── viewmodel/      # ViewModel + Factory
        │   ├── ui/             # Fragment + Adapter
        │   └── util/           # 유틸리티 (키워드 추출, 링크 파싱)
        └── res/                # 레이아웃, drawable, 값 리소스
```

---

## 아키텍처 패턴

이 프로젝트는 엄격한 MVVM 레이어 분리를 따릅니다.

```
Fragment/Activity (UI)
    ↕  observe LiveData / call ViewModel methods
ViewModel (MemoViewModel)
    ↕  call repository suspend functions (in viewModelScope)
Repository (MemoRepository)
    ↕  call DAO functions
DAO + Room Database
```

**핵심 원칙:**
- UI 레이어는 직접 DAO나 Repository를 참조하지 않습니다
- ViewModel은 `viewModelScope`에서 코루틴을 실행합니다
- Repository는 키워드 추출 등 비즈니스 로직을 담당합니다
- DAO는 순수한 데이터 접근만 담당합니다

---

## 데이터베이스 마이그레이션

Room 데이터베이스 스키마를 변경할 때:

1. `AppDatabase.kt`에서 `version`을 올립니다
2. `MIGRATION_X_Y` 객체를 추가합니다
3. `.addMigrations(MIGRATION_X_Y)`를 빌더에 등록합니다
4. 기존 마이그레이션 예시 (`AppDatabase.kt` 참고):

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `memo_photos` (...)")
    }
}
```

> **주의**: Room 스키마 변경 시 마이그레이션 없이 버전만 올리면 앱이 크래시됩니다.
> 개발 중에는 `fallbackToDestructiveMigration()`을 사용할 수 있지만 프로덕션에서는 절대 사용하지 마세요.

---

## 주요 컴포넌트 설명

### KeywordExtractor (`util/KeywordExtractor.kt`)

텍스트에서 키워드를 자동 추출합니다.

- 한국어: 2~10자 단어 추출, 불용어 필터링
- 영어: 2자 이상 단어 추출
- 숫자+단위 패턴 지원 (예: "3월", "2024년")
- 최대 20개 키워드 반환

### LinkContentFetcher (`util/LinkContentFetcher.kt`)

Jsoup을 이용해 URL에서 콘텐츠를 가져옵니다.

- 제목과 본문 텍스트 추출
- Mozilla User-Agent 사용
- 최대 3,000자 반환
- 코루틴(suspend 함수)으로 비동기 처리

### MemoViewModel (`viewmodel/MemoViewModel.kt`)

UI의 단일 진입점 ViewModel입니다.

- `allMemos`: 전체 메모 목록 (LiveData)
- `searchResults`: 검색 결과 (LiveData)
- `suggestedKeywords`: 키워드 자동완성 (LiveData)
- 메모 CRUD, 키워드 관리, 사진 관리 메서드 제공

---

## 코드 컨벤션

- **파일명**: PascalCase (예: `MemoAdapter.kt`)
- **변수/함수명**: camelCase (예: `fetchLinkContent()`)
- **상수**: UPPER_SNAKE_CASE (예: `MAX_PHOTOS`)
- **레이아웃 ID**: snake_case (예: `fragment_memo_edit`)
- **문자열 리소스**: `res/values/strings.xml`에 정의 (하드코딩 지양)
- **ViewBinding** 사용 (findViewById 사용 금지)
- 코루틴 스코프는 반드시 `viewModelScope` 또는 `lifecycleScope` 사용

---

## 의존성 추가 시 주의사항

의존성을 추가할 때는 `app/build.gradle`을 수정합니다.

- Room 관련: KSP 어노테이션 프로세서 함께 추가 필요
- 새 라이브러리 추가 후 `./gradlew build`로 확인
- `build.gradle` (루트)의 플러그인 버전과 호환성 확인

---

## 알려진 제약 사항

- 사진은 메모당 최대 5장
- 키워드는 텍스트당 최대 20개 자동 추출
- 링크 콘텐츠는 최대 3,000자 추출
- `READ_EXTERNAL_STORAGE` 권한은 Android 12 이하에서만 필요
- Room 데이터베이스는 메인 스레드에서 직접 쿼리 불가 (코루틴 사용)
