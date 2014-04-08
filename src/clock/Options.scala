package clock


import java.time.{Duration, Instant, LocalTime}
import java.time.format.DateTimeFormatter
import java.util.prefs.Preferences

import scalafx.Includes._
import scalafx.beans.property._
import scalafx.event.subscriptions.Subscription
import scalafx.geometry.Insets
import scalafx.scene.layout.{Background, BackgroundFill, CornerRadii}
import scalafx.scene.paint.Color
import scalafx.scene.text.Font
import scalafx.stage.StageStyle
import scalafx.util.StringConverter

class ColorConverter extends StringConverter[Color] {
  val regexpr = """Color\(([\d\.]+),([\d\.]+),([\d\.]+),([\d\.]+)\)""".r
  override def fromString(str : String) : Color = {
    val regexpr(redStr, greenStr, blueStr, opacityStr) = str
    Color(redStr.toDouble, greenStr.toDouble, blueStr.toDouble, opacityStr.toDouble)
  }
  override def toString(color : Color) : String = {
    "Color(%.5f,%.5f,%.5f,%.5f)".format(color.red, color.green, color.blue, color.opacity)
  }
}

class LocalTimeConverter extends StringConverter[LocalTime] {
  private val formatter = DateTimeFormatter.ofPattern("HH:mm")
  override def fromString(str : String) : LocalTime = LocalTime.parse(str, formatter)
  override def toString(time : LocalTime) : String = time.format(formatter)
}

class StageStyleConverter extends StringConverter[StageStyle] {
  val regexpr = """StageStyle\((\w+)\)""".r
  override def fromString(str : String) : StageStyle = {
    val regexpr(value) = str
    StageStyle(value)
  }
  override def toString(style : StageStyle) : String = {
    "StageStyle(" + style.toString + ")"
  }
}

/* Property inheritance complex.
   Duration in scalafx, javafx, java.time
 */
class DelayIntOption(prefs : Preferences, key : String, default : Int, delay : java.time.Duration) {
  val property = IntegerProperty(prefs.getInt(key, default))
  private var lastVal : Int = 0
  private var lastInstant = Instant.MIN
  private var tickListener : Option[Subscription] = None
  property.onChange((_, _, newValue) => {
    lastVal = newValue.intValue
    lastInstant = Instant.now
    if (tickListener.isEmpty) {
      tickListener = Some(Ticker.tick.onChange {
        if (Instant.now.isAfter(lastInstant.plusSeconds(delay.getSeconds))) {
          prefs.putInt(key, lastVal)
          tickListener.get.cancel()
          tickListener = None
        }
      })
    }
  })
}

class PrefOption[T](prefs : Preferences, key : String, default : T,
  converter : StringConverter[T]) {
  def this(prefs : Preferences, key : String, default : String, converter : StringConverter[T]) =
    this(prefs, key, converter.fromString(default), converter)
  private val value : T = converter.fromString(prefs.get(key, converter.toString(default)))
  val property = ObjectProperty(value)
  property.onChange {(_, _, newValue) => prefs.put(key, converter.toString(newValue))}
}

/* javafx type hierarchy a bit awkward for bind() */
class BooleanPrefOption(prefs : Preferences, key : String, default : Boolean) {
  val property = BooleanProperty(prefs.getBoolean(key, default))
  property.onChange {(_, _, newValue) => prefs.putBoolean(key, newValue)}
}

class DoublePrefOption(prefs : Preferences, key : String, default : Double) {
  val property = DoubleProperty(prefs.getDouble(key, default))
  property.onChange {(_, _, newValue) => prefs.putDouble(key, newValue.doubleValue)}
}

class IntPrefOption(prefs : Preferences, key : String, default : Int) {
  val property = IntegerProperty(prefs.getInt(key, default))
  property.onChange {(_, _, newValue) => prefs.putInt(key, newValue.intValue)}
}

class StringPrefOption(prefs : Preferences, key : String, default : String) {
  val property = StringProperty(prefs.get(key, default))
  property.onChange {(_, _, newValue) => prefs.put(key, newValue)}
}

class Options {
  /* prefs are flushed on jvm exit */
  val prefs = Preferences.userNodeForPackage(this.getClass)
  /* Window frame style */
  val mainStageFrame = (new PrefOption[StageStyle](prefs, "mainStageFrame", StageStyle.DECORATED,
    new StageStyleConverter)).property
  /* Main window position */
  /* Filter movement until delay after finished */
  val mainStageX = (new DelayIntOption(prefs, "mainStageX", 100, Duration.ofSeconds(3))).property
  val mainStageY = (new DelayIntOption(prefs, "mainStageY", 20, Duration.ofSeconds(3))).property
  /* Clock display items */
  val dayDisplay = (new BooleanPrefOption(prefs, "dayDisplay", true)).property
  val dateDisplay = (new BooleanPrefOption(prefs, "dateDisplay", true)).property
  /* Formatters */
  private def setupFormatterOption(prefKey : String, default : String) :
  (StringProperty, ObjectProperty[DateTimeFormatter]) = {
    val format = (new StringPrefOption(prefs, prefKey, default)).property
    val formatter = ObjectProperty[DateTimeFormatter](DateTimeFormatter.ofPattern(format.value))
    format.onChange {(_, _, newValue) => formatter.value = DateTimeFormatter.ofPattern(newValue)}
    (format, formatter)
  }
  val (timeFormat, timeFormatter) = setupFormatterOption("timeFormat", "HH:mm")
  val (dayFormat, dayFormatter) = setupFormatterOption("dayFormat", "EEEE")
  val (dateFormat, dateFormatter) = setupFormatterOption("dateFormat", "dd/MM/YYYY")
  /* Alarm options */
  val alarmOn = (new BooleanPrefOption(prefs, "alarmOn", false)).property
  val alarmTime = (new PrefOption[LocalTime](prefs, "alarmTime", "07:00",
    new LocalTimeConverter)).property
  /* For Windows 7, see /c/Windows/Media for .wavs */
  val soundFile = (new StringPrefOption(prefs, "soundFile", "")).property
  /* Range 0 to 1 */
  val soundVolume = (new DoublePrefOption(prefs, "soundVolume", 0.5)).property
  val soundLoop = (new BooleanPrefOption(prefs, "soundLoop", false)).property
  /* Look */
  val backColor = (new PrefOption[Color](prefs, "backColor", Color.WHITE,
    new ColorConverter)).property
  def backgroundOfColor(color : Color) : Background = {
    val backFill = new BackgroundFill(color, CornerRadii.Empty, Insets.Empty)
    new Background(Array(backFill))
  }
  val background = ObjectProperty(backgroundOfColor(backColor.value))
  backColor.onChange {(_, _, newValue) => background.value = backgroundOfColor(newValue)}
  val fontColor = (new PrefOption[Color](prefs, "fontColor", Color.BLACK,
    new ColorConverter)).property
  val fontName = (new StringPrefOption(prefs, "fontName", Font.default.getName)).property
  val timeFontSize = (new IntPrefOption(prefs, "timeFontSize", 20)).property
  val dayFontSize = (new IntPrefOption(prefs, "dayFontSize", 14)).property
  val dateFontSize = (new IntPrefOption(prefs, "dateFontSize", 14)).property
}
