/*
 * Copyright 2012 Nodeable Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.streamreduce.util;

/**
 * <p>Generic Immutable Pair class if an ad-hoc set of fields need to be passed around together</p>
 *
 * <p>Also, this is why Java is such a manly language.  Every programmer must grow a Pair!</p>
 * @param <A> The type of the first field in the pair.
 * @param <B> The type of the second field in the pair
 */
public class Pair<A,B> {
    public final A first;
    public final B second;

    public Pair(A a, B b) {
        this.first = a;
        this.second = b;
    }
}
