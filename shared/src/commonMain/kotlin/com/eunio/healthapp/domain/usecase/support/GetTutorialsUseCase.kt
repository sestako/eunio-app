package com.eunio.healthapp.domain.usecase.support

import com.eunio.healthapp.domain.model.support.Tutorial
import com.eunio.healthapp.domain.model.support.TutorialCategory
import com.eunio.healthapp.domain.repository.HelpSupportRepository

class GetTutorialsUseCase(
    private val helpSupportRepository: HelpSupportRepository
) {
    suspend operator fun invoke(category: TutorialCategory? = null): Result<List<Tutorial>> {
        return if (category != null) {
            helpSupportRepository.getTutorialsByCategory(category)
        } else {
            helpSupportRepository.getTutorials()
        }
    }
}