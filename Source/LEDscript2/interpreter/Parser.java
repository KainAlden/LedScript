package LEDscript2.interpreter;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import LEDscript2.parser.*;
import LEDscript2.values.*;

public class Parser implements SiliVisitor {
	// stores led arrays
	String[][] lights = new String[10][1000];
	String[][] colour = new String[10][1000];
	int lednums[] = new int[10];

	// Scope display handler
	private Display scope = new Display();

	// Get the ith child of a given node.
	private static SimpleNode getChild(SimpleNode node, int childIndex) {
		return (SimpleNode) node.jjtGetChild(childIndex);
	}

	// Get the token value of the ith child of a given node.
	private static String getTokenOfChild(SimpleNode node, int childIndex) {
		return getChild(node, childIndex).tokenValue;
	}

	// Execute a given child of the given node
	private Object doChild(SimpleNode node, int childIndex, Object data) {
		return node.jjtGetChild(childIndex).jjtAccept(this, data);
	}

	// Execute a given child of a given node, and return its value as a Value.
	// This is used by the expression evaluation nodes.
	Value doChild(SimpleNode node, int childIndex) {
		return (Value) doChild(node, childIndex, null);
	}

	// Execute all children of the given node
	Object doChildren(SimpleNode node, Object data) {
		return node.childrenAccept(this, data);
	}

	// Called if one of the following methods is missing...
	public Object visit(SimpleNode node, Object data) {
		System.out.println(node + ": acceptor not implemented in subclass?");
		return data;
	}

	// Execute a Sili program
	public Object visit(ASTCode node, Object data) {
		return doChildren(node, data);
	}

	// Execute a statement
	public Object visit(ASTStatement node, Object data) {
		return doChildren(node, data);
	}

	// Execute a block
	public Object visit(ASTBlock node, Object data) {
		return doChildren(node, data);
	}

	// Function definition
	public Object visit(ASTFnDef node, Object data) {
		// Already defined?
		if (node.optimised != null)
			return data;
		// Child 0 - identifier (fn name)
		String fnname = getTokenOfChild(node, 0);
		if (scope.findFunctionInCurrentLevel(fnname) != null)
			throw new ExceptionSemantic("Function " + fnname + " already exists.");
		FunctionDefinition currentFunctionDefinition = new FunctionDefinition(fnname, scope.getLevel() + 1);
		// Child 1 - function definition parameter list
		doChild(node, 1, currentFunctionDefinition);
		// Add to available functions
		scope.addFunction(currentFunctionDefinition);
		// Child 2 - function body
		currentFunctionDefinition.setFunctionBody(getChild(node, 2));
		// Child 3 - optional return expression
		if (node.fnHasReturn)
			currentFunctionDefinition.setFunctionReturnExpression(getChild(node, 3));
		// Preserve this definition for future reference, and so we don't define
		// it every time this node is processed.
		node.optimised = currentFunctionDefinition;
		return data;
	}

	// Function definition parameter list
	public Object visit(ASTParmlist node, Object data) {
		FunctionDefinition currentDefinition = (FunctionDefinition) data;
		for (int i = 0; i < node.jjtGetNumChildren(); i++)
			currentDefinition.defineParameter(getTokenOfChild(node, i));
		return data;
	}

	// Function body
	public Object visit(ASTFnBody node, Object data) {
		return doChildren(node, data);
	}

	// Function return expression
	public Object visit(ASTReturnExpression node, Object data) {
		return doChildren(node, data);
	}

	// Function call
	public Object visit(ASTCall node, Object data) {
		FunctionDefinition fndef;
		if (node.optimised == null) {
			// Child 0 - identifier (fn name)
			String fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
			if (fndef == null)
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			// Save it for next time
			node.optimised = fndef;
		} else
			fndef = (FunctionDefinition) node.optimised;
		FunctionInvocation newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		scope.execute(newInvocation, this);
		return data;
	}

	// Function invocation in an expression
	public Object visit(ASTFnInvoke node, Object data) {
		FunctionDefinition fndef;
		if (node.optimised == null) {
			// Child 0 - identifier (fn name)
			String fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
			if (fndef == null)
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			if (!fndef.hasReturn())
				throw new ExceptionSemantic(
						"Function " + fnname + " is being invoked in an expression but does not have a return value.");
			// Save it for next time
			node.optimised = fndef;
		} else
			fndef = (FunctionDefinition) node.optimised;
		FunctionInvocation newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		return scope.execute(newInvocation, this);
	}

	// Function invocation argument list.
	public Object visit(ASTArgList node, Object data) {
		FunctionInvocation newInvocation = (FunctionInvocation) data;
		for (int i = 0; i < node.jjtGetNumChildren(); i++)
			newInvocation.setArgument(doChild(node, i));
		newInvocation.checkArgumentCount();
		return data;
	}

	// Execute an IF
	public Object visit(ASTIfStatement node, Object data) {
		// evaluate boolean expression
		Value hopefullyValueBoolean = doChild(node, 0);
		if (!(hopefullyValueBoolean instanceof ValueBoolean))
			throw new ExceptionSemantic("The test expression of an if statement must be boolean.");
		if (((ValueBoolean) hopefullyValueBoolean).booleanValue())
			doChild(node, 1); // if(true), therefore do 'if' statement
		else if (node.ifHasElse) // does it have an else statement?
			doChild(node, 2); // if(false), therefore do 'else' statement
		return data;
	}

	// Execute a FOR loop
	public Object visit(ASTForLoop node, Object data) {
		// loop initialisation
		doChild(node, 0);
		while (true) {
			// evaluate loop test
			Value hopefullyValueBoolean = doChild(node, 1);
			if (!(hopefullyValueBoolean instanceof ValueBoolean))
				throw new ExceptionSemantic("The test expression of a for loop must be boolean.");
			if (!((ValueBoolean) hopefullyValueBoolean).booleanValue())
				break;
			// do loop statement
			doChild(node, 3);
			// assign loop increment
			doChild(node, 2);
		}
		return data;
	}

	// Process an identifier
	// This doesn't do anything, but needs to be here because we need an
	// ASTIdentifier node.
	public Object visit(ASTIdentifier node, Object data) {
		return data;
	}

	// Execute the WRITE statement
	public Object visit(ASTWrite node, Object data) {
		System.out.println();
		System.out.println("||-------------------------------------||");

		// for each light array
		for (int p = 0; p < 10; p++) {

			// find how many rows of lights
			int numlines = lednums[p] / 3;
			if (lednums[p] % 3 != 0) {
				numlines += 1;
			}
			// if teh name of teh led array is correct
			if (colour[p][0].equals(doChild(node, 0).stringValue())) {
				int counter = 1;
				//print out teh elemetns of the light aray to the screen
				// for the puroprse of this assignemt  a mock up of the array will be printed to screen
				// to controll leds the write will need to be modified to output teh rgb values to the specified leds
				for (int outc = 0; outc < numlines; outc++) {
					System.out.println(
							"||" + lights[p][counter] + "||" + lights[p][counter + 1] + "||" + lights[p][counter + 2]);
					System.out.println("||-------------------------------------||");
					counter += 3;

				}
				p += 100;
			}
		}

		return data;
	}

	// Dereference a variable or parameter, and return its value.
	public Object visit(ASTDereference node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = node.tokenValue;
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference) node.optimised;
		return reference.getValue();
	}

	// Execute an assignment statement.
	public Object visit(ASTAssignment node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference) node.optimised;

		//chekcs teh assigned int is between 1-255 
		// if it is not it the number will be setr to teh nearest boundary
		ValueInteger test = (ValueInteger) doChild(node, 1);
		ValueInteger lower = new ValueInteger(1);
		if (test.compare(lower) == -1) {
			reference.setValue(lower);
			return data;
		}
		reference.setValue(doChild(node, 1));
		return data;
	}

	// OR
	public Object visit(ASTOr node, Object data) {
		return doChild(node, 0).or(doChild(node, 1));
	}

	// AND
	public Object visit(ASTAnd node, Object data) {
		return doChild(node, 0).and(doChild(node, 1));
	}

	// ==
	public Object visit(ASTCompEqual node, Object data) {
		return doChild(node, 0).eq(doChild(node, 1));
	}

	// !=
	public Object visit(ASTCompNequal node, Object data) {
		return doChild(node, 0).neq(doChild(node, 1));
	}

	// >=
	public Object visit(ASTCompGTE node, Object data) {
		return doChild(node, 0).gte(doChild(node, 1));
	}

	// <=
	public Object visit(ASTCompLTE node, Object data) {
		return doChild(node, 0).lte(doChild(node, 1));
	}

	// >
	public Object visit(ASTCompGT node, Object data) {
		return doChild(node, 0).gt(doChild(node, 1));
	}

	// <
	public Object visit(ASTCompLT node, Object data) {
		return doChild(node, 0).lt(doChild(node, 1));
	}

	// +
	public Object visit(ASTAdd node, Object data) {
		ValueInteger uppertest = new ValueInteger(255);
		ValueInteger totatl = (ValueInteger) doChild(node, 0).add(doChild(node, 1));
		if (totatl.compare(uppertest) == 1) {
			return uppertest;
		}
		return doChild(node, 0).add(doChild(node, 1));
	}

	// -
	public Object visit(ASTSubtract node, Object data) {
		ValueInteger lowertest = new ValueInteger(1);
		ValueInteger totatl = (ValueInteger) doChild(node, 0).subtract(doChild(node, 1));
		if (totatl.compare(lowertest) == -1) {
			return lowertest;
		}
		return doChild(node, 0).subtract(doChild(node, 1));
	}

	// *
	public Object visit(ASTTimes node, Object data) {
		ValueInteger uppertest = new ValueInteger(255);
		ValueInteger totatl = (ValueInteger) doChild(node, 0).mult(doChild(node, 1));
		if (totatl.compare(uppertest) == 1) {
			return uppertest;
		}
		return doChild(node, 0).mult(doChild(node, 1));
	}

	// /
	public Object visit(ASTDivide node, Object data) {
		ValueInteger uppertest = new ValueInteger(255);
		ValueInteger totatl = (ValueInteger) doChild(node, 0).div(doChild(node, 1));
		if (totatl.compare(uppertest) == 1) {
			return uppertest;
		}
		return doChild(node, 0).div(doChild(node, 1));
	}

	// NOT
	public Object visit(ASTUnaryNot node, Object data) {
		return doChild(node, 0).not();
	}

	// + (unary)
	public Object visit(ASTUnaryPlus node, Object data) {
		return doChild(node, 0).unary_plus();
	}

	// - (unary)
	public Object visit(ASTUnaryMinus node, Object data) {
		return doChild(node, 0).unary_minus();
	}

	// Return string literal
	public Object visit(ASTCharacter node, Object data) {
		if (node.optimised == null)
			node.optimised = ValueString.stripDelimited(node.tokenValue);
		return node.optimised;
	}

	// Return integer literal
	public Object visit(ASTInteger node, Object data) {

		if (node.optimised == null)
			node.optimised = new ValueInteger(Long.parseLong(node.tokenValue));

		ValueInteger uppertest = new ValueInteger(255);
		ValueInteger numebr = (ValueInteger) node.optimised;
		// checks number below 255
		if (numebr.compare(uppertest) == 1) {
			node.optimised = uppertest;
			return node.optimised;
		}
		return node.optimised;
	}

	// Return floating point literal
	public Object visit(ASTRational node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueRational(Double.parseDouble(node.tokenValue));
		return node.optimised;
	}

	// Return true literal
	public Object visit(ASTTrue node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueBoolean(true);
		return node.optimised;
	}

	// Return false literal
	public Object visit(ASTFalse node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueBoolean(false);
		return node.optimised;
	}

	// addsteh giuven number from teh value. then checks it is still in teh
	// boundaries
	public Object visit(ASTAddassign node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference) node.optimised;

		//checks numbers are in teh boundaary of 1-255
		ValueInteger test = (ValueInteger) doChild(node, 0).add(doChild(node, 1));
		ValueInteger lower = new ValueInteger(1);
		ValueInteger upper = new ValueInteger(255);
		if (test.compare(lower) == -1) {
			reference.setValue(lower);
			return data;
		}
		if (test.compare(upper) == 1) {
			reference.setValue(upper);
			return data;
		}
		reference.setValue(test);
		return data;
	}

	// subtract the given number from the value. then checks it is still in teh
	// boundaries
	public Object visit(ASTDecassign node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference) node.optimised;

		//checks numbers are in teh boundaary of 1-255
		ValueInteger test = (ValueInteger) doChild(node, 0).subtract(doChild(node, 1));
		ValueInteger lower = new ValueInteger(1);
		ValueInteger upper = new ValueInteger(255);

		if (test.compare(lower) == -1) {
			reference.setValue(lower);
			return data;
		}
		if (test.compare(upper) == 1) {
			reference.setValue(upper);
			return data;
		}
		reference.setValue(test);

		return data;
	}

	// flips the current value
	public Object visit(ASTflipassign node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference) node.optimised;

		//checks numbers are in teh boundaary of 1-255
		ValueInteger upper = new ValueInteger(255);
		ValueInteger test = (ValueInteger) upper.subtract(doChild(node, 0));
		ValueInteger lower = new ValueInteger(1);

		if (test.compare(lower) == -1) {
			reference.setValue(lower);
			return data;
		}
		if (test.compare(upper) == 1) {
			reference.setValue(upper);
			return data;
		}

		reference.setValue(test);
		return data;
	}

	// multiplys teh current number by the specifided number and checks it in
	// teh correct boundaries
	public Object visit(ASTMultiassign node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference) node.optimised;

		//checks numbers are in teh boundaary of 1-255
		ValueInteger test = (ValueInteger) doChild(node, 0).mult(doChild(node, 1));
		ValueInteger lower = new ValueInteger(1);
		ValueInteger upper = new ValueInteger(255);

		if (test.compare(lower) == -1) {
			reference.setValue(lower);
			return data;
		}
		if (test.compare(upper) == 1) {
			reference.setValue(upper);
			return data;
		}
		reference.setValue(test);
		return data;
	}

	// didivdes teh current number by the specifided number and checks it in teh
	// correct boundaries
	public Object visit(ASTDivassign node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference) node.optimised;

		//checks numbers are in teh boundaary of 1-255
		ValueInteger test = (ValueInteger) doChild(node, 0).div(doChild(node, 1));
		ValueInteger lower = new ValueInteger(1);
		ValueInteger upper = new ValueInteger(255);

		if (test.compare(lower) == -1) {
			reference.setValue(lower);
			return data;
		}
		if (test.compare(upper) == 1) {
			reference.setValue(upper);
			return data;
		}

		reference.setValue(test);

		return data;
	}

	// waits for the number of seconds specified
	public Object visit(ASTDelay node, Object data) {
		long store = doChild(node, 0).longValue();
		try {
			Thread.sleep(store * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	// sets teh identifier to store 1
	public Object visit(ASTClear node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference) node.optimised;

		ValueInteger set = new ValueInteger(1);
		reference.setValue(set);
		return data;
	}

	// sets the identifier to store 255
	public Object visit(ASTFill node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference) node.optimised;

		ValueInteger set = new ValueInteger(255);
		reference.setValue(set);
		return data;
	}

	public Object visit(ASTUpdate node, Object data) {

		// finds which array of leds rtelated tyo the name given
		int arraynum = -1;
		for (int p = 0; p < 10; p++) {
			if (colour[p][0].equals(doChild(node, 4).stringValue())) {
				arraynum = p;
				p += 100;
			}
		}
		
		//handels the array not being present
		if(arraynum == -1)
		{
			System.out.println("led array not found");
		}

		// finds whihc light is to be edited
		ValueInteger lightnum = (ValueInteger) doChild(node, 3);
		double light = lightnum.doubleValue();

		// gets teh rgb values that will be set to teh light
		double Red = doChild(node, 0).doubleValue();
		double Green = doChild(node, 1).doubleValue();
		double Blue = doChild(node, 2).doubleValue();

		// used rgb to find ligh golour adn set the led array to store this
		// colour
		if ((Red > 180) & (Green > 180) & (Blue > 180)) {
			colour[arraynum][(int) light] = "WHITE";
		} else if ((Red < 100) & (Green < 100) & (Blue < 100)) {
			colour[arraynum][(int) light] = "BLACK";
		} else if ((Red > 180) & (Green < 99) & (Blue < 99)) {
			colour[arraynum][(int) light] = "RED";
		} else if ((Red < 100) & (Green > 180) & (Blue < 100)) {
			colour[arraynum][(int) light] = "LIME";
		} else if ((Red < 100) & (Green < 100) & (Blue > 180)) {
			colour[arraynum][(int) light] = "BLUE";
		} else if ((Red > 180) & (Green > 180) & (Blue < 100)) {
			colour[arraynum][(int) light] = "YELLOW";
		} else if ((Red < 100) & (Green > 180) & (Blue > 180)) {
			colour[arraynum][(int) light] = "AQUA";
		} else if ((Red > 180) & (Green < 100) & (Blue > 180)) {
			colour[arraynum][(int) light] = "MAGENTA";
		} else if ((Red < 225) & (Green < 225) & (Blue < 225) & (Red > 155) & (Green > 155) & (Blue > 155)) {
			colour[arraynum][(int) light] = "SILVER";
		} else if ((Red < 190) & (Green < 190) & (Blue < 190) & (Red > 100) & (Green > 100) & (Blue > 100)) {
			colour[arraynum][(int) light] = "SILVER";
		} else if ((Red < 190) & (Green < 190) & (Blue < 190) & (Red > 100) & (Green > 100) & (Blue > 100)) {
			colour[arraynum][(int) light] = "GRAY";
		} else if ((Red < 190) & (Green < 100) & (Blue < 100) & (Red > 100)) {
			colour[arraynum][(int) light] = "MAROON";
		} else if ((Red < 190) & (Green < 190) & (Blue < 99) & (Red > 100) & (Green > 100)) {
			colour[arraynum][(int) light] = "OLIVE";
		} else if ((Green < 190) & (Red < 99) & (Blue < 99) & (Green > 100)) {
			colour[arraynum][(int) light] = "MAROON";
		} else if ((Red < 190) & (Green < 99) & (Blue < 99) & (Red > 100)) {
			colour[arraynum][(int) light] = "GREEN";
		} else if ((Blue < 190) & (Green < 99) & (Red < 99) & (Blue > 100)) {
			colour[arraynum][(int) light] = "NAVY";
		} else if ((Red < 190) & (Blue < 190) & (Green < 99) & (Red > 100) & (Blue > 100)) {
			colour[arraynum][(int) light] = "PURPLE";
		} else if ((Blue < 190) & (Green < 190) & (Red < 99) & (Blue > 100) & (Green > 100)) {
			colour[arraynum][(int) light] = "TEAL";
		} else {
			colour[arraynum][(int) light] = "UNKNOWN";
		}
		// sets teh seccond array to store specified lightsd new rgb values
		String temp = doChild(node, 0).stringValue() + "-" + doChild(node, 1).stringValue() + "-"
				+ doChild(node, 2).stringValue();

		lights[arraynum][(int) light] = temp;

		return null;
	}

	
	//writes teh colour of each light to the console
	public Object visit(ASTInfo node, Object data) {
		System.out.println();
		System.out.println("||-------------------------------------||");

		// for each light array
		for (int p = 0; p < 10; p++) {

			// find how many rows of lights
			int numlines = lednums[p] / 3;
			if (lednums[p] % 3 != 0) {
				numlines += 1;
			}
			// if teh name of teh led array is correct
			if (colour[p][0].equals(doChild(node, 0).stringValue())) {
				int counter = 1;
				for (int outc = 0; outc < numlines; outc++) {
					System.out.println(
							"||" + colour[p][counter] + "||" + colour[p][counter + 1] + "||" + colour[p][counter + 2]);
					System.out.println("||-------------------------------------||");
					counter += 3;

				}
				p += 100;
			}
		}

		System.out.println();

		return data;
	}

	//creat a new array of leds
	public Object visit(ASTLeds node, Object data) {

		//gets teh size of the array
		ValueInteger ledcount = (ValueInteger) doChild(node, 0);
		double num = ledcount.doubleValue();
		int nextarray = 0;

		//stores teh size of the array
		for (int p = 0; p < 10; p++) {
			if (lednums[p] == 0) {
				lednums[p] = (int) num;
				colour[p][0] = doChild(node, 1).stringValue();
				nextarray = p;
				p += 10;
			}

		}
		
		//initalises the array to 0 and teh colour to off
		for (int i = 1; i <= lednums[0]; i++) {
			lights[nextarray][i] = "0-0-0";
			colour[nextarray][i] = "OFF";
		}
		lights[nextarray][1] = "0-0-0";
		colour[nextarray][1] = "OFF";
		return null;
	}

	//save a ledarray to a file
	public Object visit(ASTSave node, Object data) {
		int arraynum = 0;
		
		for (int p = 0; p < 10; p++) {
			if (colour[p][0].equals(doChild(node, 0).stringValue())) {
				arraynum = p;
				p += 10;
			}
		}

		try {

			PrintWriter out = new PrintWriter((doChild(node, 0).stringValue() + ".txt"));

			int numlines = lednums[arraynum] / 3;
			
			if (lednums[arraynum] % 3 != 0) {
				numlines += 1;
			}
			int counter = 1;
			for (int outc = 0; outc < numlines; outc++) {
				out.println("|" + lights[arraynum][counter] + "|" + lights[arraynum][counter + 1] + "|"
						+ lights[arraynum][counter + 2]);
				counter += 3;

			}
			arraynum += 10;
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return arraynum;
	}
}
