package org.lpmti.helpdeskplatform.gate

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement

/**
 * DOM-интерфейс гейта: волнистый индикатор M3, статусы процесса, всплывающее
 * уведомление о фолбэке и кнопка повтора.
 *
 * Элементы описаны в index.html: #gate-status, #gate-notice и #gate-retry.
 */
internal class GateUi {
    private val status: HTMLDivElement?
        get() = document.getElementById(ELEMENT_STATUS) as? HTMLDivElement

    private val notice: HTMLDivElement?
        get() = document.getElementById(ELEMENT_NOTICE) as? HTMLDivElement

    private val retryButton: HTMLButtonElement?
        get() = document.getElementById(ELEMENT_RETRY) as? HTMLButtonElement

    /** Показать текущий этап работы гейта, скрыть уведомление и кнопку повтора. */
    fun showStatus(text: String) {
        status?.textContent = text
        hideNotice()
        retryButton?.hidden = true
    }

    /** Показать ошибку и кнопку повтора. */
    fun showError(message: String) {
        status?.textContent = message
        hideNotice()
        retryButton?.hidden = false
    }

    /** Показать всплывающее уведомление (например, о редиректе при фолбэке). */
    fun showNotice(message: String) {
        notice?.textContent = message
        notice?.hidden = false
    }

    /** Скрыть уведомление. */
    fun hideNotice() {
        notice?.hidden = true
    }

    /** Подписаться на нажатие кнопки повтора. */
    fun onRetry(handler: () -> Unit) {
        retryButton?.addEventListener("click", { handler() })
    }

    private companion object {
        const val ELEMENT_STATUS = "gate-status"
        const val ELEMENT_NOTICE = "gate-notice"
        const val ELEMENT_RETRY = "gate-retry"
    }
}
