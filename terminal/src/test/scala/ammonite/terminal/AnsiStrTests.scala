package ammonite.terminal

import utest._


object AnsiStrTests extends TestSuite{
  import Ansi.Str.parse
  // Alias a bunch of rendered attributes to short names
  // to use in all our test cases
  val R = Ansi.Color.Red.escape
  val G = Ansi.Color.Green.escape
  val B = Ansi.Color.Blue.escape
  val Y = Ansi.Color.Yellow.escape
  val UND = Ansi.Underlined.On.escape
  val DUND = Ansi.Underlined.Off.escape
  val REV = Ansi.Reversed.On.escape
  val DREV = Ansi.Reversed.Off.escape
  val DCOL = Ansi.Color.Reset.escape
  val RES = Ansi.Attr.Reset.escape
  /**
    * ANSI escape sequence to reset text color
    */
  val RTC = "\u001b[39m"
  val tests = TestSuite{
    val rgbOps = s"+++$R---$G***$B///"
    val rgb = s"$R$G$B"
    'parsing{
      assert(
        parse(rgbOps).plainText == "+++---***///",
        parse(rgb).plainText == "",
        parse(rgbOps).render == rgbOps + RTC,
        parse(rgb).render == ""
      )
    }

    'concat{
      val concated = (parse(rgbOps) ++ parse(rgbOps)).render
      val expected = rgbOps ++ RTC ++ rgbOps ++ RTC

      assert(concated == expected)
    }

    'split{
      val splits = Seq(
        // Under-shot indexes just get clamped
        (-99,  s"", s"+++$R---$G***$B///$RTC"),
        (-1,  s"", s"+++$R---$G***$B///$RTC"),

        // These are the standard series
        (0,  s"", s"+++$R---$G***$B///$RTC"),
        (1,  s"+", s"++$R---$G***$B///$RTC"),
        (2,  s"++", s"+$R---$G***$B///$RTC"),
        (3,  s"+++", s"$R---$G***$B///$RTC"),
        (4,  s"+++$R-$RTC", s"$R--$G***$B///$RTC"),
        (5,  s"+++$R--$RTC", s"$R-$G***$B///$RTC"),
        (6,  s"+++$R---$RTC", s"$G***$B///$RTC"),
        (7,  s"+++$R---$G*$RTC", s"$G**$B///$RTC"),
        (8,  s"+++$R---$G**$RTC", s"$G*$B///$RTC"),
        (9,  s"+++$R---$G***$RTC", s"$B///$RTC"),
        (10, s"+++$R---$G***$B/$RTC", s"$B//$RTC"),
        (11, s"+++$R---$G***$B//$RTC", s"$B/$RTC"),
        (12, s"+++$R---$G***$B///$RTC", s""),

        // Overshoots just get clamped
        (13, s"+++$R---$G***$B///$RTC", s""),
        (99, s"+++$R---$G***$B///$RTC", s"")
      )
      for((index, expectedLeft0, expectedRight0) <- splits){
        val (splitLeft, splitRight) = parse(rgbOps).splitAt(index)
        val (expectedLeft, expectedRight) = (expectedLeft0, expectedRight0)
        val left = splitLeft.render
        val right = splitRight.render
        assert((left, right) == (expectedLeft, expectedRight))
      }
    }

    'overlay{
      'simple{
        val overlayed = rgbOps.overlay(Ansi.Color.Yellow, 4, 7)
        val expected = s"+++$R-$Y--*$G**$B///$RTC"
        assert(overlayed.render == expected)
      }
      'resetty{
        val resetty = s"+$RES++$R--$RES-$RES$G***$B///"
        val overlayed = resetty.overlay(Ansi.Color.Yellow, 4, 7).render
        val expected = s"+++$R-$Y--*$G**$B///$RTC"
        assert(overlayed == expected)
      }
      'mixedResetUnderline{
        val resetty = s"+$RES++$R--$RES-$UND$G***$B///"
        val overlayed = resetty.overlay(Ansi.Color.Yellow, 4, 7).render toVector
        val expected = s"+++$R-$Y--$UND*$G**$B///$DCOL$DUND" toVector

        assert(overlayed == expected)
      }
      'underlines{
        val resetty = s"$UND#$RES    $UND#$RES"
        'underlineBug{
          val overlayed = resetty.overlay(Ansi.Reversed.On, 0, 2).render
          val expected = s"$UND$REV#$DUND $DREV   $UND#$DUND"
          assert(overlayed == expected)
        }
        'barelyOverlapping{
          val overlayed = resetty.overlay(Ansi.Reversed.On, 0, 1).render
          val expected = s"$UND$REV#$DUND$DREV    $UND#$DUND"
          assert(overlayed == expected)
        }
        'endOfLine{
          val overlayed = resetty.overlay(Ansi.Reversed.On, 5, 6).render
          val expected = s"$UND#$DUND    $UND$REV#$DUND$DREV"
          assert(overlayed == expected)
        }
        'overshoot{
          val overlayed = resetty.overlay(Ansi.Reversed.On, 5, 10).render.toVector
          val expected = s"$UND#$DUND    $UND$REV#$DUND$DREV".toVector
          assert(overlayed == expected)
        }
        'empty{
          val overlayed = resetty.overlay(Ansi.Reversed.On, 0, 0).render
          val expected = s"$UND#$DUND    $UND#$DUND"
          assert(overlayed == expected)
        }
        'singleContent{
          val overlayed = resetty.overlay(Ansi.Reversed.On, 2, 4).render
          val expected = s"$UND#$DUND $REV  $DREV $UND#$DUND"
          assert(overlayed == expected)

        }
      }
    }
    'attributes{
      * - {
        Console.RESET + Ansi.Underlined.On
      }
      * - {
        Console.RESET + (Ansi.Underlined.On("Reset ") ++ Ansi.Underlined.Off("Underlined"))
      }
      * - {
        Console.RESET + Ansi.Bold.On
      }
      * - {
        Console.RESET + (Ansi.Bold.On("Reset ") ++ Ansi.Bold.Off("Bold"))
      }
      * - {
        Console.RESET + Ansi.Reversed.On
      }
      * - {
        Console.RESET + (Ansi.Reversed.On("Reset ") ++ Ansi.Reversed.Off("Reversed"))
      }
    }
    def tabulate(all: Seq[Ansi.Attr]) = {
      all.map(attr => attr.toString + " " * (25 - attr.name.length))
         .grouped(3)
         .map(_.mkString)
         .mkString("\n")
    }

    'colors - tabulate(Ansi.Color.all)
    'backgrounds - tabulate(Ansi.Back.all)
  }
}
