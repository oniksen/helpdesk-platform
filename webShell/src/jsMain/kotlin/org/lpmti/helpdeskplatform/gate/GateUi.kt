package org.lpmti.helpdeskplatform.gate

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLLIElement
import org.w3c.dom.HTMLOListElement
import kotlin.coroutines.resume

/**
 * DOM-интерфейс гейта: индикатор M3, список этапов, детали процесса и
 * кнопка повтора.
 *
 * Элементы описаны в index.html: #gate-steps, #gate-status, #gate-retry.
 */
internal class GateUi {
    private val status: HTMLDivElement?
        get() = document.getElementById(ELEMENT_STATUS) as? HTMLDivElement

    private val retryButton: HTMLButtonElement?
        get() = document.getElementById(ELEMENT_RETRY) as? HTMLButtonElement

    private val steps: HTMLOListElement?
        get() = document.getElementById(ELEMENT_STEPS) as? HTMLOListElement

    /**
     * Отметить этап [stepId] текущим: пройденные шаги получают отметку,
     * последующие возвращаются в состояние ожидания.
     */
    fun showStep(stepId: String) {
        val currentIndex = STEP_IDS.indexOf(stepId)
        if (currentIndex < 0) return

        val items = steps?.children ?: return
        for (index in 0 until items.length) {
            val item = items.item(index) as? HTMLLIElement ?: continue
            val itemIndex = STEP_IDS.indexOf(item.getAttribute(ATTRIBUTE_STEP))
            when {
                itemIndex < 0 -> Unit
                itemIndex < currentIndex -> item.setAttribute(ATTRIBUTE_STATE, STATE_DONE)
                itemIndex == currentIndex -> item.setAttribute(ATTRIBUTE_STATE, STATE_ACTIVE)
                else -> item.removeAttribute(ATTRIBUTE_STATE)
            }
        }
        retryButton?.hidden = true
    }

    /** Показать деталь текущего этапа, скрыть кнопку повтора. */
    fun showDetail(text: String) {
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

    /**
     * Дождаться завершения двух кадров: первый — чтобы изменение DOM попало
     * в отрисовку, второй — чтобы браузер успел показать кадр целиком.
     * Без этого быстрая смена этапов не успевала бы отрисоваться.
     */
    suspend fun nextPaint() {
        repeat(2) {
            suspendCancellableCoroutine { continuation ->
                window.requestAnimationFrame { continuation.resume(Unit) }
            }
        }
    }

    companion object {
        const val STEP_AUTH = "auth"
        const val STEP_CHANNEL = "channel"
        const val STEP_LOAD = "load"

        private const val ELEMENT_STATUS = "gate-status"
        private const val ELEMENT_RETRY = "gate-retry"
        private const val ELEMENT_STEPS = "gate-steps"
        private const val ATTRIBUTE_STEP = "data-step"
        private const val ATTRIBUTE_STATE = "data-state"

        private const val STATE_ACTIVE = "active"
        private const val STATE_DONE = "done"

        /** Идентификаторы этапов в порядке выполнения (см. #gate-steps). */
        private val STEP_IDS = listOf(STEP_AUTH, STEP_CHANNEL, STEP_LOAD)
    }
}
