@file:JsModule("jszip")
@file:JsNonModule

import kotlin.js.Promise

external object JSZip {
    fun loadAsync(data: dynamic): Promise<dynamic>
}
