/*
 * Copyright 2011-2012 Jonathan Anderson
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
import java.io.{ByteArrayOutputStream, IOException}
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.net.{URI,URL}
import java.security.AccessControlException
import java.util.logging.{Level, Logger}

import scala.collection.JavaConversions._
import scala.collection.mutable

import me.footlights.api
import me.footlights.api.{Application, KernelInterface, ModifiablePreferences}
import me.footlights.api.support.Either._
import me.footlights.api.support.Pipeline._
import me.footlights.api.support.Tee._

import me.footlights.core
import me.footlights.core.{Flusher, Footlights, ModifiableStorageEngine, Preferences}
import me.footlights.core.crypto.{Fingerprint, Keychain, MutableKeychain}
import me.footlights.core.data
import me.footlights.core.ProgrammerError


package me.footlights.core.apps {


/** Wrapper for applications; ensures consistent exception handling */
class AppWrapper private(init:Method, val name:URI, val kernel:KernelInterface,
		prefs:ModifiablePreferences, log:Logger) {
	override lazy val toString = "Application { '" + name + "' }"

	/** The application itself (lazily initialized). */
	lazy val app:Application = {
		try {
			init.invoke(null, kernel, prefs, log) match {
				case app:Application => app
				case a:Any => throw new ClassCastException(
						"init() returned non-application '%s'" format a)
			}
		} catch {
			case t:Throwable => throw new AppStartupException(name, t)
		}
	}
}

object AppWrapper {
	def apply(mainClass:Class[_], name:URI, footlights:Footlights, root:api.Directory) = {

		val init = mainClass.getMethod("init",
				classOf[KernelInterface], classOf[ModifiablePreferences], classOf[Logger])

		val log = Logger getLogger name.toString
		val appRootDir = root get "root" map { _.directory } getOrElse { root mkdir "root" }

		val keychain = {
			val key = "keychain"

			root get key map { case f:data.File =>
				f.getContents } map
				Keychain.parse getOrElse Right(Keychain()) map { keys =>
				new MutableKeychain(keys, (modified:Keychain) => root save (key, modified.getBytes))
			} get
		}

		val prefs = {
			val key = "prefs"

			val values = root get key map {
				_.file.get } map { case f:data.File =>
				f.getContents } map
				Preferences.parse map {
				Map() ++ _ } getOrElse Map()

			val mutableMap = scala.collection.mutable.Map(values.toSeq: _*)

			ModifiableStorageEngine(mutableMap, Some(root save (key, _)))
		}

		// Create a wrapper around the real kernel which saves keys to an app-specific keychain.
		val kernelWrapper = new KernelInterface() {
			override def save(bytes:ByteBuffer) =
				footlights save bytes tee { case f:data.File => keychain store f.link }

			override def open(name:String) = footlights openat (name split "/", root)
			override def openDirectory(dirname:String) = {
				val path = dirname split "/"

				var dir = appRootDir
				for (name <- path if !name.isEmpty) {
					dir = dir flatMap {
						_ get name map
						Right.apply getOrElse
						Left(new Exception("No directory '%s' in %s" format (name, dirname))) flatMap {
						_.directory }
					}
				}

				dir
			}

			override def open(name:URI) = try {
				keychain getLink { Fingerprint decode name } toRight {
					new Exception("Unable to find key for '%s'" format name) } flatMap
					footlights.open
			} catch {
				case ex:Exception => Left(ex)
			}

			override def openLocalFile = footlights.openLocalFile tee { case f:data.File =>
				keychain store f.link
			}

			override def saveLocalFile(file:api.File) = footlights saveLocalFile file

			override def promptUser(prompt:String, default:Option[String]) = {
				footlights.promptUser(prompt, name.toString, default)
			}
		}

		new AppWrapper(init, name, kernelWrapper, prefs, log)
	}
}

/** Provides plugin [un]loading. */
trait ApplicationManagement extends Footlights {
	protected def keychain:MutableKeychain
	protected def loadedApps:mutable.Map[URI,AppWrapper]
	protected def appLoader:ClassLoader
	protected def prefs:ModifiablePreferences

	protected def readPrefs(filename:URI):Option[Map[String,String]]

	def runningApplications() = loadedApps.values toSeq

	override def loadApplication(uri:URI): Either[Exception,AppWrapper] =
		loadedApps get(uri) map Right.apply getOrElse {
			val appClass = loadAppClass(uri.toURL).right map {
				AppWrapper(_, uri, this, applicationRoot(uri))
			}

			appClass.right foreach { loadedApps put (uri, _) }

			appClass
		}

	override def unloadApplication(app:AppWrapper) =
		loadedApps find { kv => kv._2 == app } foreach { kv => loadedApps remove kv._1 }


	/** The root directory which holds an application's state (prefs, keychain, filesystem...). */
	private def applicationRoot(appName:URI): data.MutableDirectory =
		applications(appName.toString) map {
			_.directory } getOrElse {
			applications mkdir appName.toString } map { case d:data.MutableDirectory => d } get



	/**
	 * The method provided by the lower-level {@link ClassLoader} which actually loads applications.
	 *
	 * This will only succeed if we are running with appropriate JVM privileges (which we should,
	 * since this is part of {@link Kernel} initialization).
	 */
	private val loadApplicationMethod = appLoader.getClass.getDeclaredMethod(
			"loadApplication", classOf[URL])

	loadApplicationMethod.setAccessible(true)

	/** Load an application's main class from a given classpath. */
	private def loadAppClass(classpath:URL):Either[Exception,Class[_]] =
		(try { loadApplicationMethod.invoke(appLoader, classpath) }
		catch { case ex:Exception => Left(ex) }) match {
			case Right(c:Class[_]) => Right(c)
			case Left(ex:Exception) => Left(new AppStartupException(classpath.toURI, ex))
			case a:Any =>
				Left(new AppStartupException(classpath.toURI,
						new IllegalArgumentException(
								"%s returned '%s', rather than a Class or a Throwable" format (
										loadApplicationMethod.getName, a
									)
							)
					)
				)
		}

	/** The root directory where application data is stored. */
	private lazy val applications = {
		val key = "applications"
		def remember(dir:data.Directory) = {
			save(dir) map { _.link } tee
				keychain.store map {
				_.fingerprint.encode } foreach {
				prefs set (key, _)
			}
		}

		prefs getString key map
			URI.create map
			openDirectory getOrElse {
			Right(data.Directory()) } tee {
			log info "Applications root: %s".format(_) } map {
			new data.MutableDirectory(_, this, remember)
		}
	} get

	private val log = Logger getLogger { classOf[ApplicationManagement] getCanonicalName }
}

}