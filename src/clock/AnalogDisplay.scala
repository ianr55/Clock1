package clock

import java.time.LocalTime

import scalafx.geometry.{HPos, Pos}
import scalafx.scene.Group
import scalafx.scene.control.{Slider, CheckBox, ColorPicker, Label}
import scalafx.scene.layout.{Priority, ColumnConstraints, GridPane, Pane, VBox}
import scalafx.scene.shape.{Circle, Ellipse}
import scalafx.scene.transform.{Rotate, Scale}

class ClockFace(x : Double = 100, y : Double = 100, hourRadius : Double = 6,
  minuteRadius : Double = 3)
  extends Group {
  (0 to 11).foreach {
    hour =>
      val dot = new Circle {
        centerX = x + (100 - hourRadius) * Math.cos(Math.toRadians(30 * hour))
        centerY = y + (100 - hourRadius) * Math.sin(Math.toRadians(30 * hour))
        radius = hourRadius
        smooth = true
        fill = Clock.options.faceColor.value
        Clock.options.faceColor.onChange {fill = Clock.options.faceColor.value}
      }
      content.add(dot)
  }
  (0 to 59).foreach {
    minute =>
      val dot = new Circle {
        centerX = x + (100 - hourRadius) * Math.cos(Math.toRadians(6 * minute))
        centerY = y + (100 - hourRadius) * Math.sin(Math.toRadians(6 * minute))
        radius = minuteRadius
        smooth = true
        fill = Clock.options.faceColor.value
        Clock.options.faceColor.onChange {fill = Clock.options.faceColor.value}
      }
      content.add(dot)
  }
  //  Clock.options.faceColor.onChange {
  //    (new ObservableBuffer(content)).foreach {
  //      item => {new Circle(item.asInstanceOf[Shape]).fill = Clock.options.faceColor.value}
  //    }
  //  }

}

class ClockHand(x : Double = 100, y : Double = 100, rotateDeg : Double = 0,
  scalePct : Double = 100, thicknessPct : Double = 4)
  extends Ellipse {
  centerX = x
  centerY = y / 2
  radiusX = thicknessPct / 2
  radiusY = 50
  smooth = true
  fill = Clock.options.handColor.value
  Clock.options.handColor.onChange {fill = Clock.options.handColor.value}
  val rotater = new Rotate(rotateDeg, centerX.value, centerY.value * 2)
  val scaler = new Scale(scalePct / 100, scalePct / 100, centerX.value, centerY.value * 2)
  transforms = List(rotater, scaler)
  def rotation : Double = rotater.angle.value
  def rotation_=(angle : Double) { rotater.angle = angle }
}

class AnalogPane extends VBox {
  alignment = Pos.CENTER
  fillWidth = false
  val face = new ClockFace
  val secondHand = new ClockHand(thicknessPct = 4)
  val minuteHand = new ClockHand(thicknessPct = 8)
  val hourHand = new ClockHand(scalePct = 80, thicknessPct = 16)
  val absPane = new Pane {
    content.addAll(face, hourHand, minuteHand)
    if (Clock.options.analogSeconds.value) content.addAll(secondHand)
    else content.removeAll(secondHand)
    Clock.options.analogSeconds.onChange {
      /* Type error if expression is boolean when Unit reqd */
      if (Clock.options.analogSeconds.value) content.addAll(secondHand)
      else content.removeAll(secondHand)
      ()
    }
    def setScale() {
      val scale = Clock.options.analogScale.value / 100.0
      val scaler = new Scale(scale, scale, 0, 0)
      transforms = List(scaler)
      prefHeight = 200 * scale
      prefWidth = 200 * scale
    }
    setScale()
    Clock.options.analogScale.onChange {setScale()}
  }
  content = absPane
  Ticker.tick.onChange {
    val nowTime = LocalTime.now
    val nowSecond = nowTime.getSecond
    secondHand.rotation = 6.0 * nowSecond
    minuteHand.rotation = 6.0 * (nowTime.getMinute + nowSecond / 60.0)
    hourHand.rotation = 30.0 * (nowTime.getHour % 12 + nowTime.getMinute / 60.0)
  }
}

class AnalogOptionsPane extends GridPane {
  val labelContraints = new ColumnConstraints(new javafx.scene.layout.ColumnConstraints) {
    halignment = HPos.CENTER
  }
  val valueContraints = new ColumnConstraints(new javafx.scene.layout.ColumnConstraints) {
    /*? no effect */
    fillWidth = true
    hgrow = Priority.ALWAYS
  }
  columnConstraints.addAll(labelContraints, valueContraints)
  hgap = 3
  val faceColorPicker = new ColorPicker(Clock.options.faceColor.value) {
    value.onChange {Clock.options.faceColor.value = value.value}
  }
  val handColorPicker = new ColorPicker(Clock.options.handColor.value) {
    value.onChange {Clock.options.handColor.value = value.value}
  }
  val analogSecondsCheck = new CheckBox() {
    selected <==> Clock.options.analogSeconds
  }
  val analogScaleSlider = new Slider(10, 100, Clock.options.analogScale.get) {
    blockIncrement = 1
    majorTickUnit = 10
    showTickLabels = false
    showTickMarks = true
    Clock.options.analogScale <== value
  }
  val row = new Count(0)
  addRow(row ++, new Label("Face Color"), faceColorPicker)
  addRow(row ++, new Label("Hand Color"), handColorPicker)
  addRow(row ++, new Label("Show Seconds"), analogSecondsCheck)
  addRow(row ++, new Label("Scale"), analogScaleSlider)


}