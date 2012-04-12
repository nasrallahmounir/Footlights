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
import java.net.{URLEncoder,URLDecoder}

import me.footlights.api.WebRequest


package me.footlights.api.ajax {

/** Represents a URL-encoded string. */
class URLEncoded private (val raw:String) {
	val encoded = URLEncoder.encode(raw, "utf-8")
	override val toString = encoded
}

/** Helper functions for encoding and decoding. */
object URLEncoded {
	def apply(s:String) = new URLEncoded(URLDecoder.decode(s, "utf-8"))
	def unapply(s:String) =
		try { Some(URLDecoder.decode(s, "utf-8")) }
		catch { case ex:Exception => None }
}

class RichWebRequest(req:WebRequest) {
	def map[T](f:WebRequest => T) = f(req)
}

object RichWebRequest {
	implicit def webReqToRichWebReq(r:WebRequest) = new RichWebRequest(r)
}

}
