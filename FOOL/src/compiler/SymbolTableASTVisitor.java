package compiler;

import java.lang.reflect.Method;
import java.util.*;
import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {
	
	private List<Map<String, STentry>> symTable = new ArrayList<>();
	private Map<String, Map<String, STentry>> classTable = new HashMap<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug);} // enables print for debugging

	private STentry stLookup(String id) {
		int j = nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = symTable.get(j--).get(id);	
		return entry;
	}

	@Override
	public Void visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = new HashMap<>();
		symTable.add(hm);
	    for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		symTable.remove(0);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}
	
	@Override
	public Void visitNode(FunNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();  
		for (ParNode par : n.parlist) parTypes.add(par.getType()); 
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes,n.retType),decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		} 
		//creare una nuova hashmap per la symTable
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level 
		decOffset=-2;
		
		int parOffset=1;
		for (ParNode par : n.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		//rimuovere la hashmap corrente poiche' esco dallo scope               
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level 
		return null;
	}
	
	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);
		visit(n.exp);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		STentry entry = new STentry(nestingLevel,n.getType(),decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Var id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		if (print) printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}
	
	@Override
	public Void visitNode(EqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(LessEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(NotNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}
	
	@Override
	public Void visitNode(TimesNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(DivNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(PlusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(MinusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(AndNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(OrNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	// OBJECT-ORIENTED EXTENSION

	@Override
	public Void visitNode(ClassNode n) {
		if (print) printNode(n);

		Map<String, STentry> hm = symTable.get(0);
		ClassTypeNode type = null;
		Map<String, STentry> virtualTable = null;

		if(n.superID != null){ //eredita
			if (!symTable.get(0).containsKey(n.superID)){ // la classe padre non esiste
				System.out.println("Super class id " + n.superID + " at line "+ n.getLine() +" not declared");
				stErrors++;
			} else{ // copio il tipo della classe padre
				ClassTypeNode superType = (ClassTypeNode) symTable.get(0).get(n.superID).type;
				type = new ClassTypeNode(new ArrayList<>(superType.allFields), new ArrayList<>(superType.allMethods));
				virtualTable = new HashMap<>(classTable.get(n.superID));
			}
		}else{ // non eredita
			type = new ClassTypeNode(new ArrayList<>(), new ArrayList<>());
			virtualTable = new HashMap<>();
		}

		STentry entry = new STentry(0, type, decOffset--);
		if (hm.put(n.id, entry) != null) {
			System.out.println("Class id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}

		// livello dentro la dichiarazione della classe
		nestingLevel++;
		symTable.add(virtualTable);

		// campi
		int fieldOffset=-(type.allFields.size()) -1;
		for (FieldNode field : n.fields){
			if (virtualTable.containsKey(field.id)){ // overriding
				// controllo di non fare overriding di un metodo
				if (virtualTable.get(field.id).type instanceof MethodTypeNode){
					System.out.println("Field id " + field.id + " at line "+ n.getLine() +" already declared as method id");
					stErrors++;
				}
				int oldOffset = virtualTable.get(field.id).offset;
				virtualTable.put(field.id, new STentry(nestingLevel, field.getType(), oldOffset));
				type.allFields.add(-oldOffset-1, field.getType());
			} else{
				virtualTable.put(field.id, new STentry(nestingLevel, field.getType(), fieldOffset--));
				type.allFields.add(field.getType());
			}
		}


		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level
		decOffset=type.allMethods.size();

		// metodi
		for (MethodNode method : n.methods){
			visit(method);
			type.allMethods.add(method.offset, (ArrowTypeNode) method.getType());
		}

		// inserisco la virtual table nella class table
		classTable.put(n.id, virtualTable);

		//rimuovere la hashmap corrente poiche' esco dallo scope
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level

		return null;
	}

	@Override
	public Void visitNode(MethodNode n) {
		if (print) printNode(n);

//		Map<String, STentry> hm = symTable.get(nestingLevel);
//		List<TypeNode> parTypes = new ArrayList<>();
//		for (ParNode par : n.parlist) parTypes.add(par.getType());
//		STentry entry = new STentry(nestingLevel, new MethodTypeNode(new ArrowTypeNode(parTypes,n.retType)),decOffset++);
//		//inserimento di ID nella symtable
//		if (hm.put(n.id, entry) != null) {
//			System.out.println("Fun id " + n.id + " at line "+ n.getLine() +" already declared");
//			stErrors++;
//		}
//		//creare una nuova hashmap per la symTable
//		nestingLevel++;
//		Map<String, STentry> hmn = new HashMap<>();
//		symTable.add(hmn);
//		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level
//		decOffset=-2;
//
//		int parOffset=1;
//		for (ParNode par : n.parlist)
//			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
//				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
//				stErrors++;
//			}
//		for (Node dec : n.declist) visit(dec);
//		visit(n.exp);
//		//rimuovere la hashmap corrente poiche' esco dallo scope
//		symTable.remove(nestingLevel--);
//		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level

		return null;
	}
}
