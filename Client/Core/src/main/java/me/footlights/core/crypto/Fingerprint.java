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
package me.footlights.core.crypto;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

import com.google.common.annotations.VisibleForTesting;

import me.footlights.HasBytes;
import me.footlights.core.Preferences;
import me.footlights.core.ConfigurationError;


/** A fingerprint for a number of bytes. */
public class Fingerprint
{
	public static Fingerprint decode(String name)
		throws NoSuchAlgorithmException
	{
		String parts[] = name.replaceAll("\\+", "/").split(":");
		if (parts.length != 2)
			throw new IllegalArgumentException("Invalid fingerprint '" + name + "'");

		String algorithmName = parts[0].toLowerCase();
		String hash = parts[1].toUpperCase();

		MessageDigest algorithm = MessageDigest.getInstance(algorithmName);
		return new Fingerprint(algorithm, ByteBuffer.wrap(new Base32().decode(hash.getBytes())));
	}

	public static Builder newBuilder() { return new Builder(Preferences.getDefaultPreferences()); }

	public String encode()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(algorithm.getAlgorithm().toLowerCase());
		sb.append(":");
		sb.append(new String(new Base32().encode(bytes.array())).replaceAll("/", "+"));

		return sb.toString();
	}

	public MessageDigest getAlgorithm() { return algorithm; }

	public boolean matches(ByteBuffer b) { return (0 == bytes.compareTo(b)); }
	public boolean matches(byte[] b) { return matches(ByteBuffer.wrap(b)); }

	public ByteBuffer getBytes() { return bytes.asReadOnlyBuffer(); }
	public byte[] copyBytes()
	{
		byte[] copy = new byte[bytes.remaining()];
		bytes.mark();
		bytes.get(copy);
		bytes.reset();
		return copy;
	}

	public static class Builder
	{
		public Fingerprint build()
		{
			ByteBuffer hash = ByteBuffer.wrap(algorithm.digest(bytes));
			return new Fingerprint(algorithm, hash);
		}

		public Builder setAlgorithm(String a) throws NoSuchAlgorithmException
		{
			algorithm = MessageDigest.getInstance(a);
			return this;
		}

		public Builder setContent(byte[] b) { bytes = b; return this; }
		public Builder setContent(HasBytes h) { return setContent(h.getBytes()); }
		public Builder setContent(ByteBuffer b)
		{
			if (b.isReadOnly())
			{
				bytes = new byte[b.remaining()];
				b.get(bytes);
			}
			else bytes = b.array();

			return this;
		}
		
		private Builder(Preferences preferences)
		{
			try
			{
				algorithm = MessageDigest.getInstance(
					preferences.getString("crypto.hash.algorithm"));
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new ConfigurationError("Invalid hash algorithm: " + e);
			}
		}

		private MessageDigest algorithm;
		private byte[] bytes;
	}


	@Override public String toString() { return encode(); }
	@Override public int hashCode() { return bytes.duplicate().getInt(); }
	@Override public boolean equals(Object o)
	{
		if (!(o instanceof Fingerprint)) return false;
		Fingerprint f = (Fingerprint) o;

		if (!algorithm.getAlgorithm().toLowerCase()
				.equals(f.algorithm.getAlgorithm().toLowerCase()))
			return false;
		if (bytes.compareTo(f.bytes) != 0) return false;

		return true;
	}

	@VisibleForTesting String hex() { return Hex.encodeHexString(bytes.array()); }

	private Fingerprint(MessageDigest hashAlgorithm, ByteBuffer fingerprintBytes)
	{
		this.algorithm = hashAlgorithm;
		this.bytes = fingerprintBytes;
	}

	private MessageDigest algorithm;
	private ByteBuffer bytes;
}
