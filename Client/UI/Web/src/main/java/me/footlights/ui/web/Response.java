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
package me.footlights.ui.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import static me.footlights.ui.web.HttpResponseCode.*;


/**
 * A response to a Web client.
 * @author Jonathan Anderson <jon@footlights.me>
 */
class Response
{
	/** Shorthand for {@link #newBuilder().setError({@link FileNotFoundException}).build()}. */
	static Response error(FileNotFoundException e) { return newBuilder().setError(e).build(); }

	/** Shorthand for {@link #newBuilder().setError({@link SecurityException}).build()}. */
	static Response error(SecurityException e) { return newBuilder().setError(e).build(); }

	/** Shorthand for {@link #newBuilder().setError({@link Throwable}).build()}. */
	static Response error(Throwable t) { return newBuilder().setError(t).build(); }

	static Builder newBuilder() { return new Builder(); }
	static class Builder
	{
		public Builder setResponse(String mimeType, InputStream content)
		{
			this.http = OK;
			this.mimeType = mimeType;
			this.content = content;
			return this;
		}

		public Builder setError(FileNotFoundException e) { return setError(FILE_NOT_FOUND, e); }
		public Builder setError(SecurityException e) { return setError(FORBIDDEN, e); }
		public Builder setError(Throwable t) { return setError(OTHER_ERROR, t); }

		private Builder setError(HttpResponseCode http, Throwable t)
		{
			this.http = http;

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintWriter writer = new PrintWriter(baos);

			t.printStackTrace(writer);
			writer.flush();

			this.content = new ByteArrayInputStream(baos.toByteArray());

			return this;
		}

		public Response build()
		{
			return new Response(http, mimeType, content);
		}

		private HttpResponseCode http = OK;
		private String mimeType = "text/xml";
		private InputStream content;
	}

	public boolean isError() { return (http != OK); }
	public String statusMessage() { return http.toString(); }

	public void write(OutputStream out) throws IOException
	{
		StringBuffer headers = new StringBuffer();
		headers.append("HTTP/1.1 ");
		headers.append(http.toString());
		headers.append("\n");

		headers.append("Content-Type: ");
		headers.append(mimeType);
		headers.append("\n");

		headers.append("\n");

		out.write(headers.toString().getBytes());
	
		if(content != null)
		{
			byte[] data = new byte[10240];
			while(true)
			{
				int bytes = content.read(data);
				
				if(bytes <= 0) break;
				else out.write(data, 0, bytes);
			}
		}
		out.flush();
	}

	private Response(HttpResponseCode httpResponse, String mimeType, InputStream content)
	{
		this.http = httpResponse;
		this.mimeType = mimeType;
		this.content = content;
	}

	private final HttpResponseCode http;
	private final String mimeType;
	private final InputStream content;
}
