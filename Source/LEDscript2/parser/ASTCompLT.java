/* Generated By:JJTree: Do not edit this line. ASTCompLT.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=LEDscript2.interpreter.BaseASTNode,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package LEDscript2.parser;

public
class ASTCompLT extends SimpleNode {
  public ASTCompLT(int id) {
    super(id);
  }

  public ASTCompLT(Sili p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SiliVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=86d97370e70de72bcdab54758f7997d1 (do not edit this line) */
