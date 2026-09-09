import kotlinx.coroutines.await
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""
    () => {
        // Проверяем физическое наличие метода. 
        // Дополнительно страхуемся: если мы в обычном браузере на localhost/обычном домене, 
        // и у нас нет специфичных для MAX объектов в window, сразу возвращаем false.
        if (typeof window === "undefined" || !window.WebApp || typeof window.WebApp.openCodeReader !== "function") {
            return false;
        }
        
        // Если поле не успело проинициализироваться (undefined), считаем мост недоступным.
        const platform = window.WebApp.platform;
        if (!platform) {
            return false;
        }
        
        return true;
    }
""")
private external fun isMaxBridgeAvailable(): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""
    () => {
        return new Promise((resolve, reject) => {
            try {
                const webApp = window.WebApp;
                
                // 1. Проверяем, что объект WebApp вообще существует
                if (!webApp) {
                    reject(new Error("MAX Bridge (window.WebApp) is unavailable"));
                    return;
                }
                
                // 2. Получаем значение initData
                const initData = webApp.initData;
                
                // 3. Строгая проверка на то, что это именно непустая строка
                if (typeof initData === "string" && initData.trim() !== "") {
                    resolve(initData);
                } else if (typeof initData === "function") {
                    reject(new Error("initData is a function, expected a string. Check your SDK version."));
                } else {
                    reject(new Error("initData is missing, null, or not a string"));
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