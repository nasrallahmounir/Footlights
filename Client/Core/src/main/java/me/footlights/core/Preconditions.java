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


/**
 * Utility class for expressing preconditions.
 * @author Jonathan Anderson (jon@footlights.me)
 */
public class Preconditions
{
	/**
	 * Checks that the supplied references are non-null.
	 * @throws NullPointerException if any supplied reference is null
	 */
	public static void notNull(Object... o) throws NullPointerException
	{
		for (int i = 0; i < o.length; i++)
			if (o[i] == null)
				throw new NullPointerException(
					"Precondition failed: argument " + i + " is null");
	}

	/** Checks that some conditions are true. */
	public static void check(boolean... b) throws IllegalArgumentException
	{
		for (int i = 0; i < b.length; i++)
			if (!b[i])
				throw new IllegalArgumentException(
					"Precondition failed: argument " + i + " is false");
	}

	/** Report a precondition failure. */
	public static void fail(String message, Throwable cause)
	{
		throw new IllegalArgumentException(message, cause);
	}

	/** Non-instantiable utility class. */
	private Preconditions() {}
}
