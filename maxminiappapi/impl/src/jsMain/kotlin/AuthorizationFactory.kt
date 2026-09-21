import kotlinx.coroutines.await
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""
    () => {
        return new Promise((resolve, reject) => {
            try {
                const webApp = window.WebApp;
                
                // 1. Проверяем, что объект WebApp вообще существует
                if (!webApp) {
                    reject(new Error("MAX Bridge (window.WebApp) недоступен"));
                    return;
                }
                
                // 2. Получаем значение initData
                const initData = webApp.initData;
                
                // 3. Строгая проверка на то, что это именно непустая строка
                if (typeof initData === "string" && initData.trim() !== "") {
                    resolve(initData);
                } else if (typeof initData === "function") {
                    reject(new Error("initData является функцией, ожидалась строка. Проверьте версию вашего SDK"));
                } else {
                    reject(new Error("Параметр initData отсутствует, равен null или не является строкой"));
                }
            } catch (error) {
                // Ловим непредвиденные ошибки (например, SecurityError при доступе к window)
                reject(error instanceof Error ? error : new Error(String(error)));
            }
        });
    }
""")
private external fun initUserData(): Promise<String>

private class MaxMaxAuthorization : MaxAuthorization {
    override suspend fun getInitUserData(): MaxAuthorizationResult {
        // if (!isMaxBridgeAvailable()) return MaxAuthorizationResult.Unavailable

        return try {
            val result = initUserData().await()
            return MaxAuthorizationResult.Success(result)
        } catch (throwable: Throwable) {
            MaxAuthorizationResult.Error(throwable)
        }
    }

}

actual fun createAuthorizationObject(): MaxAuthorization {
    return MaxMaxAuthorization()
}