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
package me.footlights.plugin.ajax;

import java.util.Map;

import me.footlights.plugin.WebRequest;

import com.google.common.collect.Maps;


/** Handle to a client-side context (ECMAScript sandbox or 'window'). */ 
public class Context implements AjaxHandler
{
	/** Construct a {@link Context} with an Ajax handler of last resort. */
	public Context(AjaxHandler defaultHandler)
	{
		this.defaultHandler = defaultHandler;
		handlers = Maps.newLinkedHashMap();
	}

	/**
	 * Construct a {@link Context} with no default Ajax handler. Any {@link WebRequest} that does not
	 * match a registered handler will cause an {@link AjaxResponse.Type.ERROR}.
	 */
	public Context() { this(null); }

	@Override
	public final JavaScript service(WebRequest request) throws Throwable
	{
		String handlerName = request.prefix();

		final AjaxHandler handler;
		if (handlers.containsKey(handlerName)) handler = handlers.get(handlerName);
		else
		{
			if (defaultHandler == null)
				throw new IllegalArgumentException(
					"No handler for request " + request.prefix() + " registered in " + this);

			handler = defaultHandler;
		}

		if (handler == null)
			throw new IllegalArgumentException(
				"Registered handler for request '" + request.prefix() + "' is null");

		return handler.service(request.shift());
	}

	synchronized
	public final Context register(String request, AjaxHandler handler)
	{
		if (handlers.containsKey(request))
			throw new IllegalArgumentException(
				this + " already has a handler registered for \"" + request
					+ "\" requests");

		handlers.put(request, handler);
		return this;
	}

	synchronized public final void unloadHandlers() { handlers.clear(); }


	@Override public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("{ handlers: [ ");
		for (Map.Entry<String, AjaxHandler> handler : handlers.entrySet())
		{
			sb.append("'");
			sb.append(handler.getKey());
			sb.append("' ");
		}
		sb.append("] }");

		return sb.toString();
	}


	/** Objects which handle requests. */
	private final Map<String, AjaxHandler> handlers;

	/** {@link AjaxHandler} of last resort, in case no handler matches a {@link WebRequest}. */
	private final AjaxHandler defaultHandler;
}
