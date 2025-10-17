package com.eunio.healthapp.domain.usecase.support

import com.eunio.healthapp.domain.model.support.FAQ
import com.eunio.healthapp.domain.repository.HelpSupportRepository

class SearchFAQsUseCase(
    private val helpSupportRepository: HelpSupportRepository
) {
    suspend operator fun invoke(query: String): Result<List<FAQ>> {
        return if (query.isBlank()) {
            Result.success(emptyList())
        } else {
            helpSupportRepository.searchFAQs(query.trim())
        }
    }
}