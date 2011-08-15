/*
 * Copyright 2011 Jonathan Anderson
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
package me.footlights.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import me.footlights.core.Preferences;

import com.google.inject.AbstractModule;


/** Guice configuration for a Footlights web app. */
public class WebAppGuiceModule extends AbstractModule
{
	@Override
	protected void configure()
	{
		final Preferences preferences;

		try { preferences = Preferences.loadFromDefaultLocation(); }
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Error loading Preferences", e);
			throw new RuntimeException(e);
		}

		bind(Preferences.class).toInstance(preferences);
		bind(Uploader.class).to(AmazonUploader.class);
	}

	private static final Logger log = Logger.getLogger(WebAppGuiceModule.class.getName());
}
