/*
 * Copyright 2012 Jonathan Anderson
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
package me.footlights.core.crypto

import java.nio.ByteBuffer
import java.security.{Key,MessageDigest,PublicKey}

import me.footlights.api
import me.footlights.api.support.Either._
import me.footlights.core
import me.footlights.core.crypto
import me.footlights.core.data


/** An identity which may have signed things. */
class Identity(val publicKey:PublicKey) extends core.HasBytes {
	def verify(x:(Fingerprint, ByteBuffer)): Boolean = x match { case (fingerprint, signature) =>
		verify(fingerprint, signature)
	}

	def verify(f:Fingerprint, signature:ByteBuffer): Boolean = {
		val s = new Array[Byte](signature.remaining)
		signature get s

		verify(f, s)
	}

	override def getBytes = {
		val algorithm = publicKey.getAlgorithm.getBytes
		val lengths = List(algorithm.length, encoded.length) map core.IO.int2bytes map { _.toArray }

		val header = Identity.Magic.toArray :: lengths
		val body = algorithm :: encoded :: Nil
		val complete = (header ++ body) map ByteBuffer.wrap

		val len = (0 /: complete) { _ + _.remaining }
		val buffer = ByteBuffer allocate len
		complete foreach buffer.put
		buffer.flip
		buffer.asReadOnlyBuffer
	}

	override val toString = "Identity { %s }" format { Fingerprint of encoded }
	override def equals(x:Any) = {
		if (!x.isInstanceOf[Identity]) false
		else {
			val other = x.asInstanceOf[Identity]
			other.encoded.toList == encoded.toList
		}
	}

	protected[crypto] def signatureAlgorithm(hashAlgorithm:MessageDigest, key:Key) =
		java.security.Signature getInstance
			hashAlgorithm.getAlgorithm.replaceAll("-", "") + "with" + key.getAlgorithm

	private def verify(f:Fingerprint, signature:Array[Byte]) = {
		val verifier = signatureAlgorithm(f.getAlgorithm, publicKey)

		verifier initVerify publicKey
		verifier update f.copyBytes
		verifier verify signature
	}

	private lazy val encoded = publicKey.getEncoded
}

object Identity {
	def apply(publicKey: PublicKey) = new Identity(publicKey)

	def parse(bytes:ByteBuffer): Either[Exception,Identity] = {
		val magic = new Array[Byte](Magic.length)
		bytes get magic
		if (magic.toList != Magic)
			Left(new data.FormatException("Magic '%s' != '%s'" format (magic.toList, Magic)))

		else {
			val algorithmLength = bytes.getInt
			val keyLength = bytes.getInt

			val (alg, encoded) = (new Array[Byte](algorithmLength), new Array[Byte](keyLength))

			bytes get alg
			bytes get encoded

			val algorithm = new String(alg)
			val factory = java.security.KeyFactory.getInstance(algorithm)
			val keySpec = new java.security.spec.X509EncodedKeySpec(encoded)

			Right(apply(factory generatePublic keySpec))
		}
	}

	/** Magic for {@link Identity}: FOOTID. */
	private val Magic = List(0xF0, 0x07, 0x1D, 0x00) map { _.toByte }
}