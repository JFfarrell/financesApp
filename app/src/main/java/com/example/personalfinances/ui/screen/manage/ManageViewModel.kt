package com.example.personalfinances.ui.screen.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinances.domain.model.Category
import com.example.personalfinances.domain.model.Merchant
import com.example.personalfinances.domain.model.OperationResult
import com.example.personalfinances.domain.usecase.category.DeleteCategoryUseCase
import com.example.personalfinances.domain.usecase.category.GetCategoriesUseCase
import com.example.personalfinances.domain.usecase.category.GetCategoryUsageUseCase
import com.example.personalfinances.domain.usecase.category.RenameCategoryUseCase
import com.example.personalfinances.domain.usecase.merchant.DeleteMerchantUseCase
import com.example.personalfinances.domain.usecase.merchant.GetMerchantUsageUseCase
import com.example.personalfinances.domain.usecase.merchant.GetMerchantsUseCase
import com.example.personalfinances.domain.usecase.merchant.RenameMerchantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A category with how many transactions use it. */
data class ManagedCategory(val category: Category, val usage: Int)

/** A merchant with how many transactions use it. */
data class ManagedMerchant(val merchant: Merchant, val usage: Int)

/** The category or merchant an edit or delete is about, with its usage at the time it was chosen. */
sealed class ManageTarget {
    abstract val name: String
    abstract val usage: Int

    data class OfCategory(val item: ManagedCategory) : ManageTarget() {
        override val name get() = item.category.name
        override val usage get() = item.usage
    }

    data class OfMerchant(val item: ManagedMerchant) : ManageTarget() {
        override val name get() = item.merchant.name
        override val usage get() = item.usage
    }
}

/**
 * State of the manage screen. [renaming] and [deleting] hold the item a dialog is open for;
 * [renameError] is the reason the last rename was refused (the dialog stays open to show it);
 * [message] is a one-off notice such as why a delete was refused.
 */
data class ManageUiState(
    val categories: List<ManagedCategory> = emptyList(),
    val merchants: List<ManagedMerchant> = emptyList(),
    val renaming: ManageTarget? = null,
    val renameError: String? = null,
    val deleting: ManageTarget? = null,
    val message: String? = null
)

/** User actions on the manage screen. */
sealed class ManageEvent {
    data class StartRename(val target: ManageTarget) : ManageEvent()
    data class ConfirmRename(val newName: String) : ManageEvent()
    object CancelRename : ManageEvent()
    data class StartDelete(val target: ManageTarget) : ManageEvent()
    object ConfirmDelete : ManageEvent()
    object CancelDelete : ManageEvent()
    object DismissMessage : ManageEvent()
}

/**
 * Lets the user rename and delete their categories and merchants. Rules (no blank or duplicate
 * names, nothing in use can be deleted) live in the use cases; this class only drives the dialogs
 * and shows the outcome.
 */
@HiltViewModel
class ManageViewModel @Inject constructor(
    getCategoriesUseCase: GetCategoriesUseCase,
    getCategoryUsageUseCase: GetCategoryUsageUseCase,
    private val renameCategoryUseCase: RenameCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    getMerchantsUseCase: GetMerchantsUseCase,
    getMerchantUsageUseCase: GetMerchantUsageUseCase,
    private val renameMerchantUseCase: RenameMerchantUseCase,
    private val deleteMerchantUseCase: DeleteMerchantUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageUiState())
    val uiState: StateFlow<ManageUiState> = _uiState.asStateFlow()

    init {
        combine(getCategoriesUseCase(), getCategoryUsageUseCase()) { categories, usage ->
            categories
                .sortedWith(compareBy({ it.type.ordinal }, { it.name.lowercase() }))
                .map { ManagedCategory(it, usage[it.id] ?: 0) }
        }.onEach { list ->
            _uiState.update { it.copy(categories = list) }
        }.launchIn(viewModelScope)

        combine(getMerchantsUseCase(), getMerchantUsageUseCase()) { merchants, usage ->
            merchants
                .sortedBy { it.name.lowercase() }
                .map { ManagedMerchant(it, usage[it.id] ?: 0) }
        }.onEach { list ->
            _uiState.update { it.copy(merchants = list) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: ManageEvent) {
        when (event) {
            is ManageEvent.StartRename ->
                _uiState.update { it.copy(renaming = event.target, renameError = null) }
            ManageEvent.CancelRename ->
                _uiState.update { it.copy(renaming = null, renameError = null) }
            is ManageEvent.ConfirmRename -> viewModelScope.launch {
                val target = _uiState.value.renaming ?: return@launch
                val result = when (target) {
                    is ManageTarget.OfCategory -> renameCategoryUseCase(target.item.category, event.newName)
                    is ManageTarget.OfMerchant -> renameMerchantUseCase(target.item.merchant, event.newName)
                }
                when (result) {
                    OperationResult.Success ->
                        _uiState.update { it.copy(renaming = null, renameError = null) }
                    is OperationResult.Failure ->
                        _uiState.update { it.copy(renameError = result.message) }
                }
            }
            is ManageEvent.StartDelete -> _uiState.update { it.copy(deleting = event.target) }
            ManageEvent.CancelDelete -> _uiState.update { it.copy(deleting = null) }
            ManageEvent.ConfirmDelete -> viewModelScope.launch {
                val target = _uiState.value.deleting ?: return@launch
                val result = when (target) {
                    is ManageTarget.OfCategory -> deleteCategoryUseCase(target.item.category)
                    is ManageTarget.OfMerchant -> deleteMerchantUseCase(target.item.merchant)
                }
                _uiState.update {
                    it.copy(
                        deleting = null,
                        message = (result as? OperationResult.Failure)?.message
                    )
                }
            }
            ManageEvent.DismissMessage -> _uiState.update { it.copy(message = null) }
        }
    }
}
