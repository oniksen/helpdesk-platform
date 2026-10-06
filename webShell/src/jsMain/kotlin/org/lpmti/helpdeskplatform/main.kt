package org.lpmti.helpdeskplatform

import InfraModules
import data.UpdateDataSource
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.lpmti.helpdeskplatform.gate.GateApp
import org.lpmti.helpdeskplatform.gate.GateStorage
import org.lpmti.helpdeskplatform.gate.GateUi

/**
 * Точка входа гейта (webShell).
 *
 * Гейт — лёгкая оболочка без Compose: DOM-экран авторизации, определение
 * канала пользователя и редирект на папку версии приложения. Версия проекта
 * здесь не используется — обновляется сам гейт (ассеты с no-cache),
 * версия приложения берётся из манифеста на сервере.
 */
fun main() {
    val gateModule = module {
        single { UpdateDataSource(get()) }
        single { GateStorage() }
        single { GateUi() }
        single {
            GateApp(
                authorization = get(),
                dataSource = get(),
                storage = get(),
                ui = get(),
            )
        }
    }

    val koin = startKoin {
        modules(
            *InfraModules.all.toTypedArray(),
            gateModule,
        )
    }.koin

    koin.get<GateApp>().start()
}
