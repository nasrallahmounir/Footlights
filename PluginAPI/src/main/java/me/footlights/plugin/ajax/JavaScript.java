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

import java.io.ByteArrayInputStream;
import java.io.InputStream;


/** Some JavaScript code (guarantee closure?). */
public class JavaScript implements AjaxResponse
{
	@Override public String mimeType() { return "text/javascript"; }
	@Override public InputStream data() { return new ByteArrayInputStream(code().getBytes()); }

	public JavaScript()
	{
		builder = new StringBuilder();
		frozen = null;
	}

	public JavaScript append(JavaScript code)
	{
		append(code.exec());
		return this;
	}

	public JavaScript append(String code)
	{
		if (frozen != null)
			throw new UnsupportedOperationException(
				"append()'ing to a frozen JavaScript");

		builder.append(code);
		return this;
	}

	public JavaScript appendText(String text) { return append(sanitizeText(text)); }

	/** JavaScript for an Ajax call. */
	public static JavaScript ajax(String code) { return ajax(code, "global"); }
	public static JavaScript ajax(String code, String context)
	{
		return new JavaScript()
			.append("sandboxes.getOrCreate(")
			.append("'").append(context).append("', ")
			.append("sandboxes['global']")
			.append(")")
			.append(".ajax('").append(JavaScript.sanitizeText(code)).append("')");
	}

	/** Make a string safe to put within single quotes. */
	public static String sanitizeText(String input)
	{
		return input.replace("'", "\\'").replace("\n", "\\n");
	}

	public String asScript() { return code(); }
	public String asFunction() { return "(function(){" + code() + "})"; }
	public String exec() { return asFunction() + "();"; }

	private String code()
	{
		// TODO(jon): sanitization?
		if (frozen == null)
			frozen = builder.toString()
				.replaceAll("\\{", "{\n")
				.replaceAll("\\}", "\n}")
				.replaceAll(";", ";\n");

		return frozen;
	}

	private final StringBuilder builder;
	private String frozen;
}