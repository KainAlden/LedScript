/* Generated By:JJTree: Do not edit this line. ASTInteger.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=LEDscript2.interpreter.BaseASTNode,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package LEDscript2.parser;

public
class ASTInteger extends SimpleNode {
  public ASTInteger(int id) {
    super(id);
  }

  public ASTInteger(Sili p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SiliVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=fba435603227086078d666ed65bfaded (do not edit this line) */
