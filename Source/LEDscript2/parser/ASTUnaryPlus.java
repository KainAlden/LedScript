/* Generated By:JJTree: Do not edit this line. ASTUnaryPlus.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=LEDscript2.interpreter.BaseASTNode,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package LEDscript2.parser;

public
class ASTUnaryPlus extends SimpleNode {
  public ASTUnaryPlus(int id) {
    super(id);
  }

  public ASTUnaryPlus(Sili p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SiliVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=ed0634843d25622d8e713282164b3808 (do not edit this line) */