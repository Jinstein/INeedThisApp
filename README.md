# 메모 키워드 앱 (MemoKeyword)

키워드 기반 메모 정리 및 검색 안드로이드 앱

## 주요 기능

### 1. 메모 작성 & 키워드 자동 추출
- 제목과 내용을 입력하면 **한국어/영어 키워드를 자동 추출**
- 불용어(조사, 접속사 등) 제거 후 의미 있는 단어만 추출
- **직접 키워드 추가** 가능 (쉼표로 여러 개 입력)
- 키워드 태그 추가/삭제 UI 제공

### 2. 키워드 검색
- **키워드 검색**: 저장된 키워드 태그로 메모 검색
- **내용 검색**: 제목/본문 텍스트로 메모 검색
- 실시간 검색 결과 업데이트

### 3. 메모 관리
- 메모 목록 (최신순 정렬)
- 메모 수정 (기존 키워드 유지)
- 메모 삭제 (길게 누르기)

## 기술 스택

| 구성요소 | 기술 |
|---|---|
| 언어 | Kotlin |
| 데이터베이스 | Room (SQLite) |
| 아키텍처 | MVVM (ViewModel + LiveData) |
| UI | Fragment + ViewBinding |
| 네비게이션 | Navigation Component |
| 레이아웃 | Material Design 3, FlexboxLayout |
| 비동기 | Coroutines |

## 데이터베이스 구조

```
memos (메모)
  id, title, content, createdAt, updatedAt

keywords (키워드)
  id, word

memo_keyword_cross_ref (다대다 연결)
  memoId, keywordId
```

## 빌드 방법

Android Studio에서 프로젝트를 열거나 아래 명령어로 빌드하세요.

```bash
./gradlew assembleDebug
```

- compileSdk: 34
- minSdk: 24 (Android 7.0+)
- targetSdk: 34