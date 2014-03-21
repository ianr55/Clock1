package clock

import java.time.{Instant, LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter
import java.util.prefs.Preferences

import scala.collection.mutable.ArrayBuffer

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import scalafx.beans.property.ReadOnlyDoubleProperty.sfxReadOnlyDoubleProperty2jfx
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.scene.text.Font
import scalafx.stage.{Popup, Stage, StageStyle}
import scalafx.stage.StageStyle.sfxEnum2jfx
import scalafx.event.ActionEvent

/* import scalafx.Includes._
 * Idea does not find reqd, type mismatches wout implicits
 */


object Clock extends JFXApp {


  //  def dumpFonts() {
  //
  //    val families = javafx.scene.text.Font.getFamilies
  //    families.foreach {
  //      println(_)
  //    }
  //  }

  //dumpFonts()
  /* load prefs etc before gui build */
  val options = new Options
  val timeDateClock = new TimeDateClock
  val alarmClock = new AlarmClock

  stage = new JFXApp.PrimaryStage {
    title = "Clock"
    x = 100
    y = 2
    initStyle(StageStyle.UTILITY)
    scene = new Scene {
      root = new BorderPane {
        center = timeDateClock.displayPane
      }

      /* problem with IDEA Optimize Imports finding implicit */
      implicit def jfxMouseEvent2sfx(me: javafx.scene.input.MouseEvent) = new MouseEvent(me)

      onMouseClicked = {
        (_: MouseEvent) => {
          optionsStage.display()
        }
      }
    }
    sizeToScene()
  }
  val mainStage = stage
  val optionsStage = new Stage(StageStyle.UTILITY) {
    initOwner(mainStage)
    title = "Clock Options"
    scene = new Scene {
      root = new BorderPane {
        center = new TabPane {
          /*todo odd bug implicits
           * border = scalafx.scene.layout.Border.EMPTY
           */
          this += new Tab {
            content = timeDateClock.optionsPane
            text = "Time"
            closable = false
          }
          this += new Tab {
            content = alarmClock.optionsPane
            text = "Alarm"
            closable = false
          }
        }
      }
    }

    def display() {
      x = (mainStage.x + mainStage.width).toDouble
      y = mainStage.y.toDouble /*todo screen bounds */
      sizeToScene()
      show()
      toFront()
    }
  }
  /* Start ticker */
  Ticker.start()
}


class FormatCombo(formats: ArrayTup2[String, String], optionFormat: StringProperty)
  extends ComboBox[String] {
  formats.getAs.foreach {FormatCombo.this += _}
  private val current = formats.getA(optionFormat.value)
  selectionModel.value.select(current)
  value.onChange((_, _, newValue) => optionFormat.value = formats.getB(newValue))
}

class TimeDateLabel(formatter: ObjectProperty[DateTimeFormatter], fontSize: Int)
  extends Label {
  font = new Font(fontSize)

  /*todo per Ensemble, no javafx.scene.layout.Background javadoc !*/
  //style = "-fx-base: Yellow"
  /* from javafx tutorial http://docs.oracle.com/javafx/2/ui_controls/custom.htm*/
  //style = "-fx-background-color: Yellow"

  def nowText: String = formatter.value.format(LocalDateTime.now)

  /*? odd type mismatch */
  //    def setBackColor {
  //      val corner : scalafx.scene.layout.CornerRadii = scalafx.scene.layout.CornerRadii.Empty
  //      val fill = new BackgroundFill(Clock.options.backColor.value,
  //        corner, Insets.Empty)
  //      val backColor = new Background(Array(fill))
  //      background = backColor
  //    }

  text = nowText
  Ticker.tick.onChange((_, _, _) => text = nowText)
  formatter.onChange((_, _, _) => text = nowText)
}

class TimeDateClock {

  import Clock.options._

  val displayPane = new VBox(0) {
    alignment = Pos.CENTER
    //centerShape = true
    //fillWidth = true
    val timeText = new TimeDateLabel(timeFormatter, 20)
    val dayText = new TimeDateLabel(dayFormatter, 14)
    val dateText = new TimeDateLabel(dateFormatter, 14)
    def setContent() {
      val contents = ArrayBuffer[TimeDateLabel](timeText)
      if (dayDisplay.value) contents += dayText
      if (dateDisplay.value) contents += dateText
      content = contents
    }
    setContent()
    val resizeDeps = Array(dayDisplay, dateDisplay, timeFormatter, dayFormatter, dateFormatter)
    resizeDeps.foreach {
      property =>
        property.onChange((_, _, _) => {
          setContent()
          Clock.mainStage.sizeToScene()
        })
    }
  }
  val timeFormats = new ArrayTup2[String, String](
    "0123" -> "HHmm", "1:2" -> "H:m", "01:23" -> "HH:mm", "1:2 AM" -> "h:m a",
    "012345" -> "HHmmss", "1:2:3" -> "H:m:s", "01:23:45" -> "HH:mm:ss", "1:2:3 AM" -> "h:m:s a")
  val timeFormatCombo = new FormatCombo(timeFormats, Clock.options.timeFormat)

  val dayFormats = new ArrayTup2[String, String]("Mon" -> "EE", "Monday" -> "EEEE")
  val dayFormatCombo = new FormatCombo(dayFormats, Clock.options.dayFormat)

  val dateFormats = new ArrayTup2[String, String](
    "1 Feb" -> "d MMM", "1 February" -> "d MMMM", "Feb 1" -> "MMM d", "February 1" -> "MMMM d",
    "1 Feb 14" -> "d MMM YY", "1 Feb 2014" -> "d MMM YYYY",
    "1/2/14" -> "d/M/YY", "01/02/2014" -> "dd/MM/YYYY")
  val dateFormatCombo = new FormatCombo(dateFormats, Clock.options.dateFormat)

  val dayDisplayCheck = new CheckBox() {
    selected = Clock.options.dayDisplay.value
    selected.onChange((_, _, newValue) => Clock.options.dayDisplay.value = newValue)
  }
  val dateDisplayCheck = new CheckBox() {
    selected = Clock.options.dateDisplay.value
    selected.onChange((_, _, newValue) => Clock.options.dateDisplay.value = newValue)
  }
  val backgroundPicker = new ColorPicker(Color.WHITE) {

  }
  val optionsPane = new GridPane {
    val row = new Count(0)
    addRow(row ++, new Label("Time Format"), timeFormatCombo)
    addRow(row ++, new Label("Day Display"), dayDisplayCheck)
    addRow(row ++, new Label("Day Format"), dayFormatCombo)
    addRow(row ++, new Label("Date Display"), dateDisplayCheck)
    addRow(row ++, new Label("Date Format"), dateFormatCombo)
    addRow(row ++, new Label("Background"), backgroundPicker)
  }
}

class AlarmClock {

  val alarmOnCheck = new CheckBox() {
    selected = Clock.options.alarmOn.value
    selected.onChange((_, _, newValue) => Clock.options.alarmOn.value = newValue)
  }
  val hourPopup = new Popup

  //  val hourScene = new Scene {
  //    root = new GridPane {

  val hourGrid = new GridPane {
    var hour = 0
    (0 to 3).foreach {
      rowIndex =>
        (0 to 2).foreach {
          colIndex =>
            add(new Button(hour.toString), rowIndex, colIndex)
            hour += 1
        }
    }
  }

  //val jfxNode: scalafx.scene.Node = hourScene
  hourPopup.content.add(hourGrid)


  val hourButton = new Button("7") {
    /* api scala doc misleading */
    onAction = {
      (ae: ActionEvent) => {
        println("onAction")
        hourPopup.show(Clock.mainStage)
      }
    }
  }
  val minuteButton = new Button("30")
  val timeChooser = new HBox(0) {
    content.addAll(hourButton, minuteButton)
  }

  val optionsPane = new GridPane {
    val row = new Count(0)
    addRow(row ++, new Label("Alarm On"), alarmOnCheck)
    //addRow(row ++, new Label("Time"), alarmTimeField)
    addRow(row ++, new Label("Time"), timeChooser)

    //addRow(row ++, new Label("Hour"), alarmHourSlider)

  }
}

class Options {
  /* prefs are flushed on jvm exit */
  val prefs = Preferences.userNodeForPackage(this.getClass)
  /*? problem with type inference within function. Bug ? */
  //  def setupBooleanOption(prefKey : String, default : Boolean) : BooleanProperty = {
  //
  //    //val pref : Boolean = prefs.getBoolean(prefKey, default)
  //    val pref : Boolean = true
  //    val option = BooleanProperty(true)
  //    option.onChange[Boolean]((_, _, newValue : Boolean) => {
  //      prefs.putBoolean(prefKey, newValue)
  //    })
  //    option
  //  }
  private val dayDisplayKey = "dayDisplay"
  val dayDisplay = BooleanProperty(prefs.getBoolean(dayDisplayKey, true))
  dayDisplay.onChange((_, _, newValue) => prefs.putBoolean(dayDisplayKey, newValue))
  private val dateDisplayKey = "dateDisplay"
  val dateDisplay = BooleanProperty(prefs.getBoolean(dateDisplayKey, true))
  dateDisplay.onChange((_, _, newValue) => prefs.putBoolean(dateDisplayKey, newValue))
  /* Formatters */
  def setupFormatterOption(
    prefKey: String, default: String): (StringProperty, ObjectProperty[DateTimeFormatter]) = {
    val format = StringProperty(prefs.get(prefKey, default))
    val formatter = ObjectProperty[DateTimeFormatter](DateTimeFormatter.ofPattern(format.value))
    format.onChange((_, _, newValue) => {
      prefs.put(prefKey, newValue)
      formatter.value = DateTimeFormatter.ofPattern(format.value)
    })
    (format, formatter)
  }
  private val timeFormatKey = "timeFormat"
  private val timeFormatDefault = "HH:mm"
  val (timeFormat, timeFormatter) = setupFormatterOption(timeFormatKey, timeFormatDefault)
  private val dayFormatKey = "dayFormat"
  private val dayFormatDefault = "EEEE"
  val (dayFormat, dayFormatter) = setupFormatterOption(dayFormatKey, dayFormatDefault)
  private val dateFormatKey = "dateFormat"
  private val dateFormatDefault = "dd/MM/YYYY"
  val (dateFormat, dateFormatter) = setupFormatterOption(dateFormatKey, dateFormatDefault)

  /* Colors */
  /*todo */
  //  def colorToCss (color : Color) : String = {
  //    def hexOf(colorVal : Double) : String = java.lang.String.format("%02x", colorVal)
  //    "0x" + (color.getRed * 255).toInt + (color.getGreen * 255).toInt+ (color.getBlue * 255)
  // .toInt
  //  }
  def colorToString(color: Color): String =
    color.getRed.toString + "," + color.getBlue.toString + "," + color.getGreen.toString

  def stringToColor(s: String): Color = {
    val ss = s.split(",")
    val (red, green, blue) = (ss(0).toDouble, ss(1).toDouble, ss(2).toDouble)
    Color(red, green, blue, 1)
  }

  private val backgroundKey = "background"
  private val backgroundDefault = "1.0,1.0,1.0"
  private var backgroundPref = prefs.get(backgroundKey, backgroundDefault)
  val backColor = ObjectProperty[Color](stringToColor(backgroundPref))
  backColor.onChange((_, _, newValue) => {
    backgroundPref = colorToString(newValue)
    prefs.put(backgroundKey, backgroundPref)
  })
  /* Alarm options */
  private val alarmOnKey = "alarmOn"
  val alarmOn = BooleanProperty(prefs.getBoolean(alarmOnKey, false))
  alarmOn.onChange((_, _, newValue) => prefs.putBoolean(alarmOnKey, newValue))
  val alarmTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
  /*? */
  private val alarmTimeKey = "alarmTime"
  var alarmTimeText = prefs.get(alarmTimeKey, "07:00")
  val alarmTimeLDT = LocalTime.parse(alarmTimeText, alarmTimeFormatter)
  val alarmTime = ObjectProperty[LocalTime](alarmTimeLDT)
  alarmTime.onChange((_, _, newValue) => {
    alarmTimeText = newValue.format(alarmTimeFormatter)
    prefs.put(alarmTimeKey, alarmTimeText)
  })

}

object Ticker {
  /* Uses javafx types due scalafx incomplete */
  /* Eclipse organise imports gets confused, so explicit */
  //import javafx.concurrent._
  private val oneSecond = new javafx.util.Duration(1000)
  private val service = new javafx.concurrent.ScheduledService[Instant] {
    setDelay(oneSecond)
    setPeriod(oneSecond)
    setRestartOnFailure(false) /* redundant */
    /* Default creates many new threads */
    setExecutor(java.util.concurrent.Executors.newSingleThreadScheduledExecutor)

    override def createTask(): javafx.concurrent.Task[Instant] = {
      val task = new javafx.concurrent.Task[Instant] {
        override def call(): Instant = {
          //println("Ticker call", Instant.now.toString, Thread.currentThread.toString)
          Instant.now
        }
      }
      task
    }
  }
  val tick = new scalafx.beans.property.ReadOnlyObjectProperty(service.lastValueProperty)

  def start() { service.start() }
}


/* Utility classes */

class ArrayTup2[A, B](tup2s: (A, B)*) extends ArrayBuffer[(A, B)](tup2s.size) {
  this ++= tup2s

  def getAs: Seq[A] = this.map(tup => tup._1)

  @throws(classOf[NoSuchElementException])
  def getA(b: B): A = this.find(tup => tup._2 == b).get._1

  @throws(classOf[NoSuchElementException])
  def getB(a: A): B = this.find(tup => tup._1 == a).get._2
}

class Count(init: Int) {
  var value: Int = init

  def ++ : Int = {
    val current = value
    value += 1
    current
  }
}

