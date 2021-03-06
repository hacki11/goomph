/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.gradle.osgi;

/** Determines where the manifest is written out by {@link BndManifestPlugin}. */
public class BndManifestExtension {
	public Object copyTo = null;

	public void copyTo(Object copyTo) {
		this.copyTo = copyTo;
	}

	public boolean mergeWithExisting = false;

	public void mergeWithExisting(boolean mergeWithExisting) {
		this.mergeWithExisting = mergeWithExisting;
	}

	static final String NAME = "osgiBndManifest";
}
