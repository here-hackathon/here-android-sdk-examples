/*
 * Copyright (c) 2011-2019 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.here.android.example.venuesandlogging

import java.util.Locale

/**
 * Created by ankonova on 3/1/2018.
 */

object Log {

    // Flag for enabling logs
    var mEnabled: Boolean = false

    /**
     * Logs a VERBOSE message.
     * @param tag Trace tag.
     * @param message Log message.
     * @param arguments Optional arguments.
     */
    fun v(tag: String, message: String, vararg arguments: Any) {
        if (mEnabled) {
            println(android.util.Log.VERBOSE, tag, message, *arguments)
        }
    }

    /**
     * Logs a DEBUG message.
     * @param tag Trace tag.
     * @param message Log message.
     * @param arguments Optional arguments.
     */
    fun d(tag: String, message: String, vararg arguments: Any) {
        if (mEnabled) {
            println(android.util.Log.DEBUG, tag, message, *arguments)
        }
    }

    /**
     * Logs a INFO message.
     * @param tag Trace tag.
     * @param message Log message.
     * @param arguments Optional arguments.
     */
    fun i(tag: String, message: String, vararg arguments: Any) {
        if (mEnabled) {
            println(android.util.Log.INFO, tag, message, *arguments)
        }
    }

    /**
     * Logs a WARN message.
     * @param tag Trace tag.
     * @param message Log message.
     * @param arguments Optional arguments.
     */
    fun w(tag: String, message: String, vararg arguments: Any) {
        if (mEnabled) {
            println(android.util.Log.WARN, tag, message, *arguments)
        }
    }

    /**
     * Logs a ERROR message.
     * @param tag Trace tag.
     * @param message Log message.
     * @param arguments Optional arguments.
     */
    fun e(tag: String, message: String, vararg arguments: Any) {
        if (mEnabled) {
            println(android.util.Log.ERROR, tag, message, *arguments)
        }
    }

    /**
     * Logs the given trace message.
     * @param priority Priority.
     * @param tag Trace tag.
     * @param format Log message.
     * @param arguments Optional arguments.
     */
    fun println(
            priority: Int,
            tag: String,
            format: String,
            vararg arguments: Any) {
        if (mEnabled) {
            android.util.Log.println(
                    priority,
                    tag,
                    if (arguments.size > 0)
                        String.format(Locale.US, format, *arguments)
                    else
                        format)
        }
    }

    /**
     * Forms a stack trace string for the given throwable object.
     * @param throwable Throwable to form info for.
     * @return Throwable string info.
     */
    fun getStackTraceString(throwable: Throwable): String? {
        return if (mEnabled) {
            android.util.Log.getStackTraceString(throwable)
        } else {
            null
        }
    }

}
