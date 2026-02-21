package com.example.memokeyword.util

object KeywordExtractor {

    // 한국어 불용어 (조사, 접속사, 부사 등)
    private val koreanStopWords = setOf(
        "이", "가", "을", "를", "은", "는", "의", "에", "에서", "와", "과",
        "도", "만", "로", "으로", "에게", "한테", "께", "이나", "나", "또는",
        "그리고", "하지만", "그러나", "그래서", "따라서", "또한", "즉", "등",
        "및", "혹은", "아니면", "그런데", "그러면", "그래도", "다시", "이제",
        "더", "덜", "아주", "매우", "너무", "조금", "많이", "잘", "못",
        "안", "않", "없", "있", "같", "다", "하다", "되다", "이다",
        "것", "수", "때", "곳", "점", "일", "후", "전", "중"
    )

    // 영어 불용어
    private val englishStopWords = setOf(
        "a", "an", "the", "and", "or", "but", "in", "on", "at", "to",
        "for", "of", "with", "by", "from", "is", "are", "was", "were",
        "be", "been", "being", "have", "has", "had", "do", "does", "did",
        "will", "would", "could", "should", "may", "might", "shall",
        "this", "that", "these", "those", "i", "you", "he", "she", "it",
        "we", "they", "my", "your", "his", "her", "its", "our", "their",
        "not", "no", "so", "if", "as", "up", "out", "about", "into"
    )

    /**
     * 텍스트에서 키워드를 추출합니다.
     * - 최소 2글자 이상인 단어만 추출
     * - 불용어 제거
     * - 중복 제거
     * - 최대 20개 키워드 반환
     */
    fun extract(text: String): List<String> {
        val words = mutableSetOf<String>()

        // 한국어 단어 추출 (2~10글자 한글 단어)
        val koreanPattern = Regex("[가-힣]{2,10}")
        koreanPattern.findAll(text).forEach { match ->
            val word = match.value
            if (word !in koreanStopWords) {
                words.add(word)
            }
        }

        // 영어 단어 추출 (2글자 이상 영문)
        val englishPattern = Regex("[a-zA-Z]{2,}")
        englishPattern.findAll(text).forEach { match ->
            val word = match.value.lowercase()
            if (word !in englishStopWords && word.length >= 2) {
                words.add(word)
            }
        }

        // 숫자+단위 조합 추출 (예: 3월, 2024년)
        val numberPattern = Regex("[0-9]+[가-힣]{1,3}")
        numberPattern.findAll(text).forEach { match ->
            words.add(match.value)
        }

        return words.take(20)
    }

    /**
     * 사용자가 입력한 키워드 문자열을 파싱합니다.
     * 쉼표, 공백, 해시태그(#) 구분자 지원
     */
    fun parseUserKeywords(input: String): List<String> {
        return input
            .split(",", " ", "#")
            .map { it.trim() }
            .filter { it.length >= 1 }
            .distinct()
            .take(30)
    }
}
