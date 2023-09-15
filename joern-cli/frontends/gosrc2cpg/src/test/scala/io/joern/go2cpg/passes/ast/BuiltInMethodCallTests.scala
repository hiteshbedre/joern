package io.joern.go2cpg.passes.ast

import io.joern.go2cpg.testfixtures.GoCodeToCpgSuite
import io.joern.dataflowengineoss.language.*
import io.shiftleft.semanticcpg.language.*
import io.shiftleft.codepropertygraph.generated.nodes.*

class BuiltInMethodCallTests extends GoCodeToCpgSuite {

  "When inbuilt method(make) is used to create map" should {
    val cpg = code("""package main
        |
        |func main() {
        |	myMap := make(map[string]int)
        |}""".stripMargin)
    "check identifier properties" in {
      val List(x) = cpg.identifier("myMap").l
      x.lineNumber.get shouldBe 4
      x.name shouldBe "myMap"
    }

    "check identifier(LHS) properties type" ignore {
      val List(x) = cpg.identifier("myMap").l
      x.typeFullName shouldBe "map"
    }

    "check call node properties" in {
      val List(x) = cpg.call("make").l
      x.name shouldBe "make"
      x.typeFullName shouldBe "map"
    }

    "check call node arguments" in {
      val List(x) = cpg.call("make").argument.isLiteral.l
      x.code shouldBe "map[string]int"
      x.argumentIndex shouldBe 1
      x.typeFullName shouldBe "map"
    }

  }

}
