package clock

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import scala.collection.mutable.ArrayBuffer

import scalafx.Includes._
import scalafx.beans.property._
import scalafx.event.ActionEvent
import scalafx.geometry.{HPos, Pos}
import scalafx.scene.control._
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.scene.text.Font
import scalafx.stage.StageStyle
import scalafx.util.converter.IntStringConverter
import scalafx.scene.paint.Color

class FontChooser extends VBox(0) {
  border = Clock.outerBorder
  val fontNames = Font.fontNames
  val listView = new ListView(fontNames) {
    private val current = fontNames.find {_ == Clock.options.fontName.get}.getOrElse("System Bold")
    selectionModel.value.select(current)
    Clock.options.fontName <== selectionModel.value.selectedItem /* Beware NPE */
  }
  content = List(new Label("Font Name"), listView)
}

//class FontSizeCombo(option: IntegerProperty) extends ComboBox[Int] {
//  editable = true
//  converter = new IntStringConverter
//  List(10, 12, 14, 16, 18, 20, 24, 28, 32).foreach {this += _}
//  val selModel = selectionModel.value
//  /* Ambiguous overload */
//  selModel.select(option.value)
//  option <== selModel.selectedItem
//}

class FontSizeCombo(option : IntegerProperty) extends ComboBox[String] {
  editable = true
  //  val editCell = buttonCell.value
  //  editCell.maxWidth = 20
  val converter1 = new IntStringConverter
  List(10, 12, 14, 16, 18, 20, 24, 28, 32).foreach {this += converter1.toString(_)}
  private val current = option.value.toString
  selectionModel.value.select(current) /* May fail */
  value.onChange((_, _, newValue) => option.value = converter1.fromString(newValue))
}

class FormatCombo(formats : ArrayTup2[String, String], optionFormat : StringProperty)
  extends ComboBox[String] {
  formats.getAs.foreach {FormatCombo.this += _}
  private val current = formats.getA(optionFormat.value)
  selectionModel.value.select(current)
  value.onChange((_, _, newValue) => optionFormat.value = formats.getB(newValue))
}

class TimeDateLabel(formatter : ObjectProperty[DateTimeFormatter], fontSize : IntegerProperty)
  extends Label {
  background <== Clock.options.background
  def rgbString(color : Color) =
    "rgb(%d,%d,%d)".format((color.red * 255).toInt, (color.green * 255).toInt,
      (color.blue * 255).toInt)
  def fontColorStyle : String = "-fx-text-fill: " + rgbString(Clock.options.fontColor.value) + ";"
  style = fontColorStyle
  Clock.options.fontColor.onChange {style = fontColorStyle}
  font = new Font(Clock.options.fontName.value, fontSize.value)
  Clock.options.fontName.onChange {font = new Font(Clock.options.fontName.value, fontSize.value)}
  fontSize.onChange {font = new Font(Clock.options.fontName.value, fontSize.value)}
  def textNow : String = formatter.value.format(LocalDateTime.now)
  var currentText = textNow
  text = currentText
  Ticker.tick.onChange {
    val newText = textNow
    /* Filter useless layout and rendering */
    if (newText != currentText) {
      currentText = newText
      text = currentText
    }
  }
}

class TimeDateClock {

  import Clock.options._

  val displayPane = new VBox(0) {
    alignment = Pos.CENTER
    background <== Clock.options.background
    border = Clock.outerBorder
    //centerShape = true
    //fillWidth = true
    val timeText = new TimeDateLabel(timeFormatter, timeFontSize)
    val dayText = new TimeDateLabel(dayFormatter, dayFontSize)
    val dateText = new TimeDateLabel(dateFormatter, dateFontSize)
    def setContent() {
      val contents = ArrayBuffer[TimeDateLabel](timeText)
      if (dayDisplay.value) contents += dayText
      if (dateDisplay.value) contents += dateText
      content = contents
    }
    setContent()
    List(dayDisplay, dateDisplay).foreach {_.onChange {setContent()}}
  }
  /* Options UI */
  val backColorPicker = new ColorPicker(Clock.options.backColor.value) {
    value.onChange {Clock.options.backColor.value = value.value}
    /* Problem with scalafx, javafx cvn */
    //Clock.options.backColor <== backColorPicker.value
  }
  val fontColorPicker = new ColorPicker(Clock.options.fontColor.value) {
    value.onChange {Clock.options.fontColor.value = value.value}
  }
  lazy val fontChooser = new FontChooser
  val fontButton = new Button(Clock.options.fontName.value) {
    text <== Clock.options.fontName
  }
  fontButton.onAction = (_ : ActionEvent) => {
    Clock.display.popOutProperty.value = Some(fontChooser)
    fontChooser.requestFocus()
  }
  val frames = new ArrayTup2[String, StageStyle](
    "All" -> StageStyle.DECORATED, "Minimal" -> StageStyle.UTILITY,
    "None" -> StageStyle.UNDECORATED)
  val frameCombo = new ComboBox[String] {
    tooltip = Tooltip("Requires restart")
    /* No effect */
    //tooltip.value.font = new Font("System", 12)
    frames.getAs.foreach {this += _}
    val current = frames.getA(Clock.options.mainStageFrame.value)
    selectionModel.value.select(current)
    value.onChange((_, _, newValue) => Clock.options.mainStageFrame.value = frames.getB(newValue))
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
    selected <==> Clock.options.dayDisplay
  }
  val dateDisplayCheck = new CheckBox() {
    selected <==> Clock.options.dateDisplay
  }
  val optionsPane = new GridPane {
    val labelContraints = new ColumnConstraints(new javafx.scene.layout.ColumnConstraints) {
      halignment = HPos.CENTER
    }
    val valueContraints = new ColumnConstraints(new javafx.scene.layout.ColumnConstraints) {
      fillWidth = true
    }
    columnConstraints.addAll(labelContraints, valueContraints)
    val row = new Count(0)
    addRow(row ++, new Label("Background"), backColorPicker)
    addRow(row ++, new Label("Font Color"), fontColorPicker)
    addRow(row ++, new Label("Font Name"), fontButton)
    addRow(row ++, new Label("Frame"), frameCombo)
    addRow(row ++, new Label("Time Format"), timeFormatCombo)
    addRow(row ++, new Label("Time Font Size"), new FontSizeCombo(Clock.options.timeFontSize))
    addRow(row ++, new Label("Day Display"), dayDisplayCheck)
    addRow(row ++, new Label("Day Format"), dayFormatCombo)
    addRow(row ++, new Label("Day Font Size"), new FontSizeCombo(Clock.options.dayFontSize))
    addRow(row ++, new Label("Date Display"), dateDisplayCheck)
    addRow(row ++, new Label("Date Format"), dateFormatCombo)
    addRow(row ++, new Label("Date Font Size"), new FontSizeCombo(Clock.options.dateFontSize))
  }
  /* recursive value error if in class init */
  optionsPane.content.foreach {
    _.onMousePressed = {(_ : MouseEvent) => Clock.display.popOutProperty.value = None}
  }
}
