package org.lpmti.helpdeskplatform.gate

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement

/**
 * DOM-интерфейс гейта: волнистый индикатор M3, статусы процесса и кнопка
 * повтора.
 *
 * Элементы описаны в index.html: #gate-status и #gate-retry.
 */
internal class GateUi {
    private val status: HTMLDivElement?
        get() = document.getElementById(ELEMENT_STATUS) as? HTMLDivElement

    private val retryButton: HTMLButtonElement?
        get() = document.getElementById(ELEMENT_RETRY) as? HTMLButtonElement

    /** Показать текущий этап работы гейта, скрыть кнопку повтора. */
    fun showStatus(text: String) {
        status?.textContent = text
        retryButton?.hidden = true
    }

    /** Показать ошибку и кнопку повтора. */
    fun showError(message: String) {
        status?.textContent = message
        retryButton?.hidden = false
    }

    /** Подписаться на нажатие кнопки повтора. */
    fun onRetry(handler: () -> Unit) {
        retryButton?.addEventListener("click", { handler() })
    }

    private companion object {
        const val ELEMENT_STATUS = "gate-status"
        const val ELEMENT_RETRY = "gate-retry"
    }
}
