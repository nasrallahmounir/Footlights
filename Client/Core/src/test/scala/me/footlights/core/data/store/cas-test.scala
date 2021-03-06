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

import java.io.IOException
import java.net.URL
import java.nio.ByteBuffer
import java.util.logging.Level._
import java.util.logging.Logger

import scala.collection.JavaConversions._

import org.junit.runner.RunWith

import org.mockito.Matchers._
import org.mockito.Mockito.when

import org.scalatest.{BeforeAndAfter,FreeSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

import me.footlights.core.Preferences
import me.footlights.core.crypto.Fingerprint
import me.footlights.core.data.Block
import me.footlights.core.data
import me.footlights.core.tags._


package me.footlights.core.data.store {

@RunWith(classOf[JUnitRunner])
class CASTest extends FreeSpec with BeforeAndAfter with MockitoSugar with ShouldMatchers {

	before { prefs = Map() }

	"A CAS client" - {
		"should refuse to upload files without an upload key" in {
			val f = mock[Fingerprint]
			val b = mock[ByteBuffer]

			intercept[IOException] { cas.put(f, b) }
		}

		"when talking to a local server" - {
			"should be able to upload a file" taggedAs(LocalCAS) in {
				prefs += (SharedSecretName -> SharedSecret)
				prefs += (UploadUrl._1 -> "http://localhost:8080/footlights-uploadserver/upload")

				try { cas.put(block.name, block.getBytes) }
				catch {
					case e:java.net.ConnectException =>
						log.log(WARNING, "Can't connect to local CAS; is Tomcat running?", e)
				}
			}
		}

		"when talking to a remote server" - {
			"should be able to upload a file" taggedAs(InternetAccess) in {
				prefs += (SharedSecretName -> SharedSecret)
				prefs += UploadUrl

				try { cas.put(block.name, block.getBytes) }
				catch {
					case e:java.net.UnknownHostException =>
						log.log(WARNING, "Failed to resolve CAS; connected to Internet?", e)
				}
			}

			"should be able to download a file" taggedAs(InternetAccess) in {
				prefs += DownloadUrl
				try { cas get block.name should equal(Some(block.getBytes)) }
				catch {
					case e:java.net.UnknownHostException =>
						log.log(WARNING, "Failed to resolve CAS; connected to Internet?", e)
				}
			}
		}
	}

	/** Generate a new {@link CASClient} on demand to test. */
	private def cas:CASClient = {
		def uploadUrl() = prefs get UploadUrl._1 map { new URL(_) }
		def downloadUrl(f:Fingerprint) =
			prefs get DownloadUrl._1 map { base =>
				new URL(base + "/" + java.net.URLEncoder.encode(f.encode, "utf-8")) }

		new CASClient(downloadUrl, uploadUrl, Option(SharedSecret), cache);
	}


	/** A fairly trivial (but deterministic) {@link Block} for testing purposes. */
	val block = Block.newBuilder
		.addContent { 1 to 16 map { _.toByte } toArray }
		.build

	/** Preferences which start out empty. */
	private var prefs:Map[String,String] = _

	/** Where we download blocks. */
	val DownloadUrl = ("cas.downloadURL" -> "https://footlights-cas.s3.amazonaws.com")

	/** Where to upload blocks. */
	val UploadUrl = ("cas.uploadURL" -> "https://upload.footlights.me/upload")

	/** The name of the preference containing the CAS secret. */
	val SharedSecretName = "cas.secret"

	/** For the moment, the server just checks for a shared secret */
	val SharedSecret = Preferences.loadFromDefaultLocation getString SharedSecretName get

	// A couple of mocks.
	val resolver = mock[me.footlights.core.Resolver]
	val cache = None

	val log = Logger getLogger classOf[CASClient].getCanonicalName
}

}
