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

package com.streamreduce.core.model;

// we don't use a single collection for persistence
// so we have all these different tag impls for services,
// and REST resources... yet SobaObject is where the tags are stored. ugh!
public interface TaggableService<T extends Taggable> {

    void addHashtag(T target, SobaObject tagger, String tag);

    void removeHashtag(T target, SobaObject tagger, String tag);

}
