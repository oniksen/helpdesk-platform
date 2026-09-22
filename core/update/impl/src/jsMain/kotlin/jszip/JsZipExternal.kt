@file:JsModule("jszip")
@file:JsNonModule

package jszip

import kotlin.js.Promise

external fun loadAsync(data: dynamic): Promise<dynamic>
