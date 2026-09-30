package com.sumi.jamplay.domain.policy

object PlaylistNamePolicy {
    enum class Error { EMPTY, DUPLICATE }

    // 기존 이름도 공백을 정리해 비교하며, 대소문자는 구분한다.
    fun validate(name: String, otherNames: List<String>): Error? {
        val trimmed = name.trim()
        return when {
            trimmed.isBlank() -> Error.EMPTY
            otherNames.any { it.trim() == trimmed } -> Error.DUPLICATE
            else -> null
        }
    }

    fun availableName(name: String, otherNames: List<String>): String? =
        name.trim().takeIf { validate(it, otherNames) == null }
}
