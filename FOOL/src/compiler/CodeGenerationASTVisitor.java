package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;
import svm.ExecuteVM;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static compiler.lib.FOOLlib.*;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

	List<List<String>> dispatchTables = new ArrayList<>();

	CodeGenerationASTVisitor() {}
	CodeGenerationASTVisitor(boolean debug) {super(false,debug);} //enables print for debugging

	@Override
	public String visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		String declCode = null;
		for (Node dec : n.declist) declCode=nlJoin(declCode,visit(dec));
		return nlJoin(
			"push 0",	
			declCode, // generate code for declarations (allocation)			
			visit(n.exp),
			"halt",
			getCode()
		);
	}

	@Override
	public String visitNode(ProgNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"halt"
		);
	}

	@Override
	public String visitNode(FunNode n) {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i=0;i<n.parlist.size();i++) popParl = nlJoin(popParl,"pop");
		String funl = freshFunLabel();
		putCode(
			nlJoin(
				funl+":",
				"cfp", // set $fp to $sp value
				"lra", // load $ra value
				declCode, // generate code for local declarations (they use the new $fp!!!)
				visit(n.exp), // generate code for function body expression
				"stm", // set $tm to popped value (function result)
				popDecl, // remove local declarations from stack
				"sra", // set $ra to popped value
				"pop", // remove Access Link from stack
				popParl, // remove parameters from stack
				"sfp", // set $fp to popped value (Control Link)
				"ltm", // load $tm value (function result)
				"lra", // load $ra value
				"js"  // jump to to popped address
			)
		);
		return "push "+funl;		
	}

	@Override
	public String visitNode(VarNode n) {
		if (print) printNode(n,n.id);
		return visit(n.exp);
	}

	@Override
	public String visitNode(PrintNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"print"
		);
	}

	@Override
	public String visitNode(IfNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();		
		return nlJoin(
			visit(n.cond),
			"push 1",
			"beq "+l1,
			visit(n.el),
			"b "+l2,
			l1+":",
			visit(n.th),
			l2+":"
		);
	}

	@Override
	public String visitNode(EqualNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"beq "+l1,
			"push 0",
			"b "+l2,
			l1+":",
			"push 1",
			l2+":"
		);
	}

	@Override
	public String visitNode(LessEqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"bleq "+l1,
				"push 0",
				"b "+l2,
				l1+":",
				"push 1",
				l2+":"
		);
	}

	@Override
	public String visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.right),
				visit(n.left),
				"bleq "+l1,
				"push 0",
				"b "+l2,
				l1+":",
				"push 1",
				l2+":"
		);
	}

	@Override
	public String visitNode(TimesNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"mult"
		);	
	}

	@Override
	public String visitNode(DivNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"div"
		);
	}

	@Override
	public String visitNode(PlusNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"add"				
		);
	}

	@Override
	public String visitNode(MinusNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"sub"
		);
	}

	@Override
	public String visitNode(NotNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.exp),
				"push 1",
				"beq "+l1,
				"push 1",
				"b "+l2,
				l1+":",
				"push 0",
				l2+":"
		);
	}

	@Override
	public String visitNode(OrNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				"push 1",
				"beq "+l1,
				visit(n.right),
				"push 1",
				"beq "+l1,
				"push 0",
				"b "+l2,
				l1+":",
				"push 1",
				l2+":"
		);
	}

	@Override
	public String visitNode(AndNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				"push 0",
				"beq "+l1,
				visit(n.right),
				"push 0",
				"beq "+l1,
				"push 1",
				"b "+l2,
				l1+":",
				"push 0",
				l2+":"
		);
	}

	@Override
	public String visitNode(IdNode n) {
		if (print) printNode(n,n.id);
		String getAR = null;
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", getAR, // retrieve address of frame containing "id" declaration
			              // by following the static chain (of Access Links)
			"push "+n.entry.offset, "add", // compute address of "id" declaration
			"lw" // load value of "id" variable
		);
	}

	@Override
	public String visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+(n.val?1:0);
	}

	@Override
	public String visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+n.val;
	}

	// OBJECT-ORIENTATION EXTENSION

	public String visitNode(ClassNode n) {
		if (print) printNode(n, n.id);

		// creo dispatch table
		List<String> dT = null;
		if (n.superID != null){ //eredita
			int offsetSuperClass = n.superEntry.offset;
			dT = new ArrayList<>(dispatchTables.get(-offsetSuperClass-2));
		} else{ // non eredita
			dT = new ArrayList<>();
		}

		// visito metodi e aggiorno dispatch table
		for (MethodNode method : n.methods){
			visit(method);
			if (method.offset < dT.size()){
				dT.set(method.offset,method.label);
			} else {
				dT.add(method.offset, method.label);
			}
		}

		dispatchTables.add(dT);

		String codeMethod = null;
		for (String label : dT){
			codeMethod = nlJoin(codeMethod,
					"push" + label, // metto sullo stack la label
					"lhp", // metto sullo stack il valore di hp
					"sw", // fa la pop dei due valori e va a scrivere all'indirizzo puntato da hp la label
					// incremento il valore di hp
					"lhp",
					"push 1",
					"add",
					"shp" );
		}

		return nlJoin(
				"lhp", // metto hp sullo stack, ovvero il dispatch pointer da ritornare alla fine
				codeMethod // creo sullo heap la dispatch table
		);
	}

	@Override
	public String visitNode(MethodNode n) {
		if (print) printNode(n,n.id);

		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i=0;i<n.parlist.size();i++) popParl = nlJoin(popParl,"pop");

		String funl = freshFunLabel();
		n.label = funl;
		putCode(
				nlJoin(
						funl+":",
						"cfp", // set $fp to $sp value
						"lra", // load $ra value
						declCode, // generate code for local declarations (they use the new $fp!!!)
						visit(n.exp), // generate code for function body expression
						"stm", // set $tm to popped value (function result)
						popDecl, // remove local declarations from stack
						"sra", // set $ra to popped value
						"pop", // remove Access Link from stack
						popParl, // remove parameters from stack
						"sfp", // set $fp to popped value (Control Link)
						"ltm", // load $tm value (function result)
						"lra", // load $ra value
						"js"  // jump to to popped address
				)
		);
		return null;
	}


	@Override
	public String visitNode(EmptyNode n) {
		if (print) printNode(n);

		return "push -1"; // nessun oggetto ha questo object pointer
	}


	@Override
	public String visitNode(ClassCallNode n) {
		if (print) printNode(n,n.objectId);

		String argCode = null, getAR = null;
		for (int i=n.arglist.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.arglist.get(i)));
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw"); // recupero object pointer

		return nlJoin(
				"lfp", // load Control Link (pointer to frame of function "id" caller)
				argCode, // generate code for argument expressions in reversed order
				"lfp", getAR, // retrieve address of frame containing "id" declaration
				// by following the static chain (of Access Links)
				"push "+n.entry.offset, "add", // compute address of "id" declaration
				"lw", // load address of "id" function
				"stm", // set $tm to popped value (with the aim of duplicating top of stack)
				"ltm", // load Access Link (pointer to frame of function "id" declaration)
				"ltm", // duplicate top of stack
				"push "+n.methodEntry.offset, "add", // compute address of method declaration
				"lw", // load address of "id" method
				"js"  // jump to popped address (saving address of subsequent instruction in $ra)
		);
	}

	@Override
	public String visitNode(CallNode n) {
		if (print) printNode(n,n.id);

		String argCode = null, getAR = null;
		for (int i=n.arglist.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.arglist.get(i)));
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");

		String code =  nlJoin(
				"lfp", // load Control Link (pointer to frame of function "id" caller)
				argCode, // generate code for argument expressions in reversed order
				"lfp", getAR, // retrieve address of frame containing "id" declaration
				// by following the static chain (of Access Links)
				"stm", // set $tm to popped value (with the aim of duplicating top of stack)
				"ltm", // load Access Link (pointer to frame of function "id" declaration)
				"ltm"// duplicate top of stack
		);

		if (n.entry.type instanceof MethodTypeNode){
			return nlJoin(
					code,
					"lw", // load address of class
					"push "+n.entry.offset, "add", // compute address of method declaration
					"lw", // load address of "id" method
					"js"  // jump to popped address (saving address of subsequent instruction in $ra)
			);
		} else{
			return nlJoin(
					code,
					"push "+n.entry.offset, "add", // compute address of "id" declaration
					"lw", // load address of "id" function
					"js"  // jump to popped address (saving address of subsequent instruction in $ra)
			);
		}
	}

	@Override
	public String visitNode(NewNode n) {
		if (print) printNode(n, n.classId);

		// richiamo tutti gli argomenti per metterli sullo stack
		String argCode = null;
		for (Node arg : n.arglist){
			argCode = nlJoin(argCode, visit(arg));
		}

		// tolgo gli argomenti dallo stack uno alla volta e li metto nello heap
		for (Node arg : n.arglist){
			argCode = nlJoin(argCode,
					"lhp", // metto sullo stack il valore di hp
					"sw", // fa la pop dei due valori e va a scrivere all'indirizzo puntato da hp il valore sullo stack
					// incremento il valore di hp
					"lhp",
					"push 1",
					"add",
					"shp" );
		}

		int off = ExecuteVM.MEMSIZE + n.entry.offset; // offset per recuperare dispatch pointer
		return nlJoin(argCode,
				"push" + off,
				"lw", // recupero dispatch pointer
				"lhp",
				"sw", // scrivo ad indirizzo hp il dispatch pointer
				"lhp", // carico sullo stack l'object pointer da ritornare
				// incremento hp
				"lhp",
				"push 1",
				"add",
				"shp"
				);

	}
}