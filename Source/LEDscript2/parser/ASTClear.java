/* Generated By:JJTree: Do not edit this line. ASTClear.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=LEDscript2.interpreter.BaseASTNode,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package LEDscript2.parser;

public
class ASTClear extends SimpleNode {
  public ASTClear(int id) {
    super(id);
  }

  public ASTClear(Sili p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SiliVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=eab2e5084611acdfb56d7c8f35e5e8e6 (do not edit this line) */
