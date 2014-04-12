package clock


import java.io.PrintWriter

import scalafx.Includes._
import scalafx.application.{JFXApp, Platform}
import scalafx.beans.property._
import scalafx.scene.{Node, Scene}
import scalafx.scene.control._
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.stage.WindowEvent

/* import scalafx.Includes._
 * Idea does not find reqd, type mismatches wout implicits
 */
/*?
 * Window resize scale
 * Sep class for displays.
 * Full screen.
 */

object Clock extends JFXApp {
  /* Catch exceptions during object init construction
  *  Does not catch exceptions below java concurrent service, ie ticks. Capture stderr in shell.
  */
  override def main(args : Array[String]) {
    try {
      super.main(args)
    } catch {
      case e : Throwable =>
        val errorOut = new PrintWriter("Clock.error.txt")
        e.printStackTrace(errorOut)
        errorOut.close()
        Platform.exit() /* does not exit to bash */
        System.exit(1)
    }
  }
  /* load prefs etc before gui build */
  val options = new Options
  /* Build gui components */
  private val borderStroke = new BorderStroke(
    stroke = Color.DARKGRAY, style = BorderStrokeStyle.Solid,
    radii = new CornerRadii(3), widths = new BorderWidths(1))
  val outerBorder = new Border(new javafx.scene.layout.Border(borderStroke))
  val timeDateClock = new TimeDateClock
  val alarmClock = new AlarmClock
  val optionsDisplay = new TabPane {
    border = outerBorder
    this += new Tab {
      content = alarmClock.optionsPane
      text = "Alarm"
      closable = false
    }
    this += new Tab {
      content = timeDateClock.optionsPane
      text = "Time"
      closable = false
    }
  }
  val display = new HBox(2) {
    var optionsShow = false
    content = List(timeDateClock.displayPane)
    timeDateClock.displayPane.onMouseClicked = {
      (_ : MouseEvent) => {
        if (optionsShow) content.retainAll(timeDateClock.displayPane)
        else content.add(optionsDisplay)
        optionsShow = !optionsShow
        stage.sizeToScene()
      }
    }
    val popOutProperty = ObjectProperty[Option[Node]](None)
    popOutProperty.onChange {
      (_, _, newValue) => {
        content.retainAll(timeDateClock.displayPane, optionsDisplay)
        newValue match {
          case Some(node) =>
            content.add(node)
          case None =>
        }
        stage.sizeToScene()
      }
    }
  }
  /* Main stage window */
  stage = new JFXApp.PrimaryStage {
    title = "Clock"
    x = options.mainStageX.value
    y = options.mainStageY.value
    initStyle(options.mainStageFrame.value)
    /* Scales and bad resize */
    //initStyle(StageStyle.DECORATED)
    scene = new Scene {
      root = display
    }
    sizeToScene()
    options.mainStageX <== x
    options.mainStageY <== y
    onCloseRequest = {
      (_ : WindowEvent) =>
        Platform.exit() /* does not exit to bash */
        System.exit(0)
    }
    /* Messy to explicitly handle items which may affect layout and size, but lag effect.
    *  No visible cpu spike.
    */
    Ticker.tick.onChange {sizeToScene()}
  }
  /* Start ticker */
  Ticker.start()
  PeriodicGC.init()
}
