/* Generated By:JJTree: Do not edit this line. ASTDivassign.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=LEDscript2.interpreter.BaseASTNode,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package LEDscript2.parser;

public
class ASTDivassign extends SimpleNode {
  public ASTDivassign(int id) {
    super(id);
  }

  public ASTDivassign(Sili p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SiliVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=7f04b43ebf50c81f5133e475fcd0e777 (do not edit this line) */
