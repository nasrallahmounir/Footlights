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
import java.util.NoSuchElementException
import java.util.logging.Logger

import me.footlights.api.{Application,KernelInterface,ModifiablePreferences}

package me.footlights.demos.tictactoe {

/**
 * A demo application for playing Tic-Tac-Toe against human or computer opponents.
 * @author Jonathan Anderson <jon@footlights.me>
 */
class TicTacToe(val prefs:ModifiablePreferences) extends Application("Tic-Tac-Toe")
{
	override val ajaxHandler = Some(new Ajax(this))

	var game = new Game

	def gameOver = {
		game.state match {
			case Game.Status.WON => incrementCounter("wins")
			case Game.Status.LOST => incrementCounter("losses")
			case _ =>
				throw new IllegalArgumentException("Invalid game state: " + game.state)
		}
	}

	def startNewGame = {
		incrementCounter("playCount")
		game = new Game
	}

	def getCounter(name:String):Int = prefs getInt(name) map { _.intValue } getOrElse 0

	private def incrementCounter(name:String) = {
		var count = getCounter(name) + 1
		prefs.set(name, count)
		count
	}
}


/** Builder used by Footlights to find and initialize the app. */
object TicTacToe
{
	def init(kernel:KernelInterface, prefs:ModifiablePreferences, log:Logger) = {
		log.info("Starting Tic-Tac-Toe")
		new TicTacToe(prefs)
	}
}

}
