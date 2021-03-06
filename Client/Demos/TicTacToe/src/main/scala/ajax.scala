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
import me.footlights.api.{Preferences,WebRequest}
import me.footlights.api.ajax.{AjaxHandler,JavaScript}

package me.footlights.demos.tictactoe {

/** Translates Ajax events to/from model events. */
class Ajax(app:TicTacToe) extends AjaxHandler
{
	override def service(request:WebRequest) =
	{
		request.path() match
		{
			case "init" | "new_game" => {
				app.startNewGame
				new JavaScript()
					.append("context.root.clear();")
					.append("context.log('starting new game...');")
					.append("context.load('init.js');")
			}

			case "update_stats" => new JavaScript().append(updateStats(app.prefs))

			case ClickCoordinates(x,y) => {
				val placed = app.game.place(x.toInt, y.toInt)

				val response = new JavaScript()

				if (placed != null)
					response
						.append(place(placed, x.toInt, y.toInt))
						.append(changeNextBox(app.game.next))

				if (app.game.isOver()) {
					app.gameOver
					response
						.append("context.log('Game over! Result: ")
						.appendText(app.game.state().toString())
						.append("');")
				}

				response.append(updateStats(app.prefs))
			}
		}
	}


	private val ClickCoordinates = """clicked/(\d),(\d)""".r

	private def place(piece:Game.Piece, x:Int, y:Int) =
		"context.globals.place('%s', %d, %d, %d)".format(piece.filename, x, y, piece.offset)

	private def changeNextBox(piece:Game.Piece) =
		"""
		context.globals.next.src = '%s';
		context.globals.next.onload = %s;
		""".format(
			piece.filename,
			new JavaScript().append(
				"""
				this.style.position = 'relative';
				this.style.top = %d;
				this.style.left = %d;
				""".format(0 - piece.offset, 0 - piece.offset)
			).asFunction()
		)

	private def updateStats(prefs:Preferences) = stats map { setStat(_) } reduce { _ +	_ }
	private def setStat(name:String) = """
			context.globals.%s.clear();
			context.globals.%s.appendText('%d');
			""".format(name, name, app.getCounter(name))

	/** The statistics that we keep track of via {@link Preferences}. */
	private val stats = List("playCount", "wins", "losses")
}

}
