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
package me.footlights.core;

import java.net.URI;
import java.util.*;

import me.footlights.core.plugin.*;
import me.footlights.plugin.KernelInterface;



/** Interface to the software core */
public interface Footlights extends KernelInterface
{
	public void registerUI(UI ui);
	public void deregisterUI(UI ui);


	public Collection<PluginWrapper> plugins();
	public PluginWrapper loadPlugin(String name, URI uri) throws PluginLoadException;
	public void unloadPlugin(PluginWrapper plugin);
}
