package net.mikolak.pomisos.testutils

import org.scalacheck.Gen

object GensMore {

  import org.scalacheck.Gen._

  def structuredList[Element, Param](start: Param)(generator: Param => Gen[Element])(
      step: Element => Param): Gen[List[Element]] =
    sized { size =>
      iterateList(generator)(start)(step)(size)
    }

  private def iterateList[Element, Param](generator: Param => Gen[Element])(current: Param)(step: Element => Param)(
      left: Int): Gen[List[Element]] =
    generator(current)
      .flatMap(elem =>
        lzy(if (left == 0) const(List.empty) else iterateList(generator)(step(elem))(step)(left - 1)).map(List(elem) ++ _))
}
