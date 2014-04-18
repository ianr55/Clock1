package clock

import java.io.File
import java.time.LocalTime

import scalafx.Includes._
import scalafx.beans.property._
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout._
import scalafx.scene.media.{Media, MediaPlayer}
import scalafx.stage.FileChooser
import scalafx.util.converter.IntStringConverter
import scalafx.geometry.HPos

class IntGrid(label : String, nRows : Int, nCols : Int, step : Int = 1) extends VBox(0) {
  border = Clock.outerBorder
  val selected = ObjectProperty[Int](0)
  val grid = new GridPane {
    val convert = new IntStringConverter
    var value = 0
    for (row <- 0 until nRows)
      for (col <- 0 until nCols) {
        val button = new Button(convert.toString(value))
        button.onAction = (_ : ActionEvent) =>
          selected.value = convert.fromString(button.text.value)
        add(button, col, row)
        value += step
      }
  }
  content = List(new Label(label), grid)
}

//class HourGrid extends VBox(0) {
//  val hourProperty = ObjectProperty[Int](0)
//  val grid = new GridPane {
//    val convert = new IntStringConverter
//    var hour = 0
//    for (row <- 0 to 7)
//      for (col <- 0 to 2) {
//        val button = new Button(convert.toString(hour))
//        button.onAction = {
//          (_: ActionEvent) => {
//            hourProperty.value = convert.fromString(button.text.value)
//          }
//        }
//        add(button, col, row)
//        hour += 1
//      }
//  }
//  content = List(new Label("Hour"), grid)
//}
class SoundPlayer {
  var player : Option[MediaPlayer] = None
  def isPlaying : Boolean = player.isDefined
  def start() {
    if (Clock.options.soundFile.value != "") {
      val uri = (new File(Clock.options.soundFile.value)).toURI
      val media = new Media(uri.toString)
      player = Some(new MediaPlayer(media))
      player.get.volume <== Clock.options.soundVolume
      player.get.cycleCount = if (Clock.options.soundLoop.value) MediaPlayer.Indefinite else 1
      /* Not in scaladoc */
      player.map {_.play()}
    }
  }
  def stop() {
    player.map {_.stop()}
    player = None
  }
  Clock.options.soundLoop.onChange((_, _, newValue) => {
    player.map {_.cycleCount = if (Clock.options.soundLoop.value) MediaPlayer.Indefinite else 1}
  })
  Clock.options.alarmTest.onChange((_, _, newValue) => {
    if (newValue) start() else stop()
  })
}

class AlarmClock {
  /* Alarm sound operations */
  val soundPlayer = new SoundPlayer
  Ticker.tick.onChange {
    if (Clock.options.alarmOn.value) {
      val now = LocalTime.now
      val alarmTime = Clock.options.alarmTime.value
      if (now.getMinute == alarmTime.getMinute && now.getHour == alarmTime.getHour &&
        !soundPlayer.isPlaying)
        soundPlayer.start()
    }
  }
  Clock.options.alarmOn.onChange((_, _, newValue) => {
    if (!newValue && !Clock.options.alarmTest.value && soundPlayer.isPlaying)
      soundPlayer.stop()
  })
}

/* Alarm options */
class AlarmOptionsPane extends GridPane {
  /* Enable */
  val alarmOnCheck = new CheckBox {
    selected = Clock.options.alarmOn.value
    def setText() { text = if (selected.value) "ON" else "OFF" }
    setText()
    selected.onChange((_, _, newValue) => {
      Clock.options.alarmOn.value = newValue
      setText()
    })
  }
  /* Hour */
  lazy val hourGrid = new IntGrid("Hour", 8, 3)
  val hourText = Clock.options.alarmTime.value.getHour.toString
  val hourButton = new Button(hourText)
  /* Weird "recursive value" error if extend Button */
  hourButton.onAction = (_ : ActionEvent) => {
    Clock.display.popOutProperty.value = Some(hourGrid)
    hourGrid.requestFocus() /* ignored */
  }
  hourGrid.selected.onChange((_, _, newValue) => {
    hourButton.text.value = newValue.toString
    val alarmTime = LocalTime.of(newValue, Clock.options.alarmTime.value.getMinute)
    Clock.options.alarmTime.value = alarmTime
    Clock.display.popOutProperty.value = None
  })
  /* Minute */
  lazy val minuteGrid = new IntGrid("Minute", 4, 3, 5)
  val minuteText = Clock.options.alarmTime.value.getMinute.toString
  val minuteButton = new Button(minuteText)
  minuteButton.onAction = (_ : ActionEvent) => {
    Clock.display.popOutProperty.value = Some(minuteGrid)
    minuteGrid.requestFocus()
  }
  minuteGrid.selected.onChange((_, _, newValue) => {
    minuteButton.text.value = newValue.toString
    val alarmTime = LocalTime.of(Clock.options.alarmTime.value.getHour, newValue)
    Clock.options.alarmTime.value = alarmTime
    Clock.display.popOutProperty.value = None
  })
  val timeChooser = new HBox(0) {
    content.addAll(hourButton, minuteButton)
  }
  /* Sound options */
  val fileChooser = new FileChooser {
    title = "Sound File"
    extensionFilters.addAll(
      /*? others */
      new javafx.stage.FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"),
      new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*"))
  }
  def soundName : String = {
    val path = Clock.options.soundFile.value
    if (path != "") {
      val file = new File(path)
      if (file.exists) file.getName
      else ""
    }
    else ""
  }
  /*? need to toggle test if change choice and playing */
  val soundChoice = new Button(soundName)
  soundChoice.onAction = (_ : ActionEvent) => {
    if (Clock.options.soundFile.value != null)
      fileChooser.initialDirectory = (new File(Clock.options.soundFile.value)).getParentFile
    val selectedFile = fileChooser.showOpenDialog(Clock.stage)
    if (selectedFile != null) {
      Clock.options.soundFile.value = selectedFile.getPath
      soundChoice.text = soundName
    }
  }
  val volumeSlider = new Slider(0, 1, Clock.options.soundVolume.get) {
    blockIncrement = 0.1
    majorTickUnit = 0.1
    showTickLabels = false
    showTickMarks = true
    Clock.options.soundVolume <== value
  }
  val loopCheck = new CheckBox("") {
    Clock.options.soundLoop <== selected
  }
  /* Not a preference option */
  val testCheck = new CheckBox("") {
    Clock.options.alarmTest <== selected
  }
  val labelContraints = new ColumnConstraints(new javafx.scene.layout.ColumnConstraints) {
    halignment = HPos.CENTER
  }
  columnConstraints.add(labelContraints)
  hgap = 3
  val row = new Count(0)
  addRow(row ++, new Label("Alarm"), alarmOnCheck)
  addRow(row ++, new Label("Time"), timeChooser)
  addRow(row ++, new Label("Sound"), soundChoice)
  addRow(row ++, new Label("Volume"), volumeSlider)
  addRow(row ++, new Label("Loop"), loopCheck)
  addRow(row ++, new Label("Test Play"), testCheck)


  /* recursive value error if in class init */
  content.foreach {
    _.onMousePressed = {(_ : MouseEvent) => Clock.display.popOutProperty.value = None}
  }
}
