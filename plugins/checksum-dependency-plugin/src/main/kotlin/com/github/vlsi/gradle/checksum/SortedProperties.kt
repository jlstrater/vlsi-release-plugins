/*
 * Copyright 2019 Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.github.vlsi.gradle.checksum

import java.util.* // ktlint-disable

class SortedProperties(props: Properties) : Properties() {
    init {
        putAll(props)
    }

    override fun keys(): Enumeration<Any> {
        val iterator = super.keys
            .asSequence()
            .filterIsInstance<String>()
            .sorted()
            .iterator()
        return object : Enumeration<Any> {
            override fun hasMoreElements() = iterator.hasNext()
            override fun nextElement(): Any? = iterator.next()
        }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<Any, Any>>
        get() = super.entries.toSortedSet(compareBy { it.key.toString() })
}
