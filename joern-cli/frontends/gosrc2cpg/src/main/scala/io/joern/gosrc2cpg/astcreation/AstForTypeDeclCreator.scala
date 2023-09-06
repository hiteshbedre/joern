package io.joern.gosrc2cpg.astcreation

import io.joern.x2cpg.ValidationMode
import io.joern.gosrc2cpg.parser.ParserAst.*
import io.joern.gosrc2cpg.parser.{ParserKeys, ParserNodeInfo}
import io.joern.x2cpg
import io.joern.x2cpg.{Ast, ValidationMode}
import io.shiftleft.codepropertygraph.generated.{DispatchTypes, EdgeTypes, Operators}
import ujson.Value
import io.joern.x2cpg.datastructures.Stack.StackWrapper
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDecl
import scala.util.{Failure, Success, Try}

import scala.util.Try

trait AstForTypeDeclCreator(implicit withSchemaValidation: ValidationMode) { this: AstCreator =>

  def astForTypeSpec(typeSpecNode: ParserNodeInfo): Seq[Ast] = {
    // TODO: Add support for member variables and methods
    methodAstParentStack.collectFirst { case t: NewTypeDecl => t } match {
      case Some(parentMethodAstNode) =>
        val nameNode = typeSpecNode.json(ParserKeys.Name)
        val typeNode = createParserNodeInfo(typeSpecNode.json(ParserKeys.Type))

        val astParentType     = parentMethodAstNode.label
        val astParentFullName = parentMethodAstNode.fullName
        val fullName          = fullyQualifiedPackage + Defines.dot + nameNode(ParserKeys.Name).str
        val typeDeclNode_ =
          typeDeclNode(
            typeSpecNode,
            nameNode(ParserKeys.Name).str,
            fullName,
            parserResult.filename,
            typeSpecNode.code,
            astParentType,
            astParentFullName
          )

        methodAstParentStack.push(typeDeclNode_)
        scope.pushNewScope(typeDeclNode_)
        val memberAsts = astForStructType(typeNode)

        methodAstParentStack.pop()
        scope.popScope()

        val modifier = addModifier(typeDeclNode_, nameNode(ParserKeys.Name).str)
        Seq(Ast(typeDeclNode_).withChild(Ast(modifier)).withChildren(memberAsts))
      case None => Seq.empty
    }

  }

  protected def astForStructType(expr: ParserNodeInfo): Seq[Ast] = {
    Try(expr.json(ParserKeys.Fields)) match
      case Success(fields) if fields != null =>
        astForFieldList(createParserNodeInfo(fields))
      case _ => Seq.empty
  }

  private def astForFieldList(fieldList: ParserNodeInfo): Seq[Ast] = {
    fieldList
      .json(ParserKeys.List)
      .arrOpt
      .getOrElse(List())
      .map(createParserNodeInfo)
      .map(astForField)
      .toSeq
  }
  protected def astForField(field: ParserNodeInfo): Ast = {
    field.node match
      case Field => {
        val typeInfo = createParserNodeInfo(field.json(ParserKeys.Type))
        val (typeFullName, typeFullNameForCode, isVariadic, evaluationStrategy) = processTypeInfo(typeInfo)
        val fieldNodeInfo = createParserNodeInfo(field.json(ParserKeys.Names).arr.head)
        val fieldName     = fieldNodeInfo.json(ParserKeys.Name).str
        Ast(memberNode(typeInfo, fieldName, s"$fieldName $typeFullNameForCode", typeFullName, Seq()))
      }
      case _ => {
        Ast()
      }
  }

}
