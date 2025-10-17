package com.eunio.healthapp.domain.usecase.support

import com.eunio.healthapp.domain.model.support.HelpCategory
import com.eunio.healthapp.domain.repository.HelpSupportRepository

class GetHelpCategoriesUseCase(
    private val helpSupportRepository: HelpSupportRepository
) {
    suspend operator fun invoke(): Result<List<HelpCategory>> {
        return helpSupportRepository.getHelpCategories()
    }
}