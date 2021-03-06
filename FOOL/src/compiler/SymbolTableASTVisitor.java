package compiler;

import java.lang.reflect.Method;
import java.sql.Ref;
import java.util.*;
import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {
	
	private List<Map<String, STentry>> symTable = new ArrayList<>();
	private Map<String, Map<String, STentry>> classTable = new HashMap<>();
	private HashSet<String> localDeclaration;
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug, true);} // enables print for debugging

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

		ArrowTypeNode funType = new ArrowTypeNode(parTypes, n.retType);
		n.setType(funType);

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

	@Override
	public Void visitNode(ArrowTypeNode n){
		if (print) printNode(n);

		for (Node par : n.parlist)
			visit(par);

		return null;
	}

	// OBJECT-ORIENTED EXTENSION

	@Override
	public Void visitNode(ClassNode n) {
		if (print) printNode(n);

		localDeclaration = new HashSet<>();
		Map<String, STentry> hm = symTable.get(0);
		ClassTypeNode type = null;
		Map<String, STentry> virtualTable = null;

		if(n.superID != null){ //eredita
			if (!(classTable.containsKey(n.superID))){ // la classe padre non esiste
				System.out.println("Super class id " + n.superID + " at line "+ n.getLine() +" not declared");
				stErrors++;
			} // else{
				n.superEntry = symTable.get(0).get(n.superID); // uso della super classe
				// copio il tipo della classe padre
//				ClassTypeNode superType = (ClassTypeNode) symTable.get(0).get(n.superID).type;
				ClassTypeNode superType = (ClassTypeNode) n.superEntry.type;
				type = new ClassTypeNode(new ArrayList<>(superType.allFields), new ArrayList<>(superType.allMethods));
				virtualTable = new HashMap<>(classTable.get(n.superID));
			// }
		}else{ // non eredita
			type = new ClassTypeNode(new ArrayList<>(), new ArrayList<>());
			virtualTable = new HashMap<>();
		}

		// creo la STEntry e setto nel nodo il suo tipo
		n.type = type;
		STentry entry = new STentry(0, type, decOffset--);


		if (hm.put(n.id, entry) != null) {
			System.out.println("Class id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}

		// inserisco la virtual table
		symTable.add(virtualTable);
		classTable.put(n.id, virtualTable);

		// livello dentro la dichiarazione della classe
		nestingLevel++;

		// campi
		int fieldOffset=-(type.allFields.size()) -1;
		for (FieldNode field : n.fields){
			if (localDeclaration.contains(field.id)){ // controllo dichiarazione multipla
				System.out.println("Field id " + field.id + " at line "+ n.getLine() +" already declared in this scope");
				stErrors++;
			} else {
				localDeclaration.add(field.id);
				if (virtualTable.containsKey(field.id)){ // overriding
					// controllo di non fare overriding di un metodo
					if (virtualTable.get(field.id).type instanceof MethodTypeNode){ // overriding sbagliato
						System.out.println("Field id " + field.id + " at line "+ n.getLine() +" already declared as method id");
						stErrors++;
					}
					// overriding corretto
					int oldOffset = virtualTable.get(field.id).offset;
					virtualTable.put(field.id, new STentry(nestingLevel, field.getType(), oldOffset));
					field.offset = oldOffset;
					type.allFields.set(-oldOffset-1, field.getType());
				} else{ // no overriding
					virtualTable.put(field.id, new STentry(nestingLevel, field.getType(), fieldOffset));
					field.offset = fieldOffset;
					fieldOffset--;
					type.allFields.add(field.getType());
				}
			}
		}

		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level
		decOffset=type.allMethods.size();

		// metodi
		for (MethodNode method : n.methods){
			if (localDeclaration.contains(method.id)){
				System.out.println("Method id " + n.id + " at line "+ n.getLine() +" already declared in this scope");
				stErrors++;
			} else {
				localDeclaration.add(method.id);
				visit(method);
				if (virtualTable.containsKey(method.id)){ //overriding
					if (!(virtualTable.get(method.id).type instanceof MethodTypeNode)){
						System.out.println("Method id " + n.id + " at line "+ n.getLine() +" already declared as field id");
						stErrors++;
					}
					int oldOffset = virtualTable.get(method.id).offset;
					virtualTable.put(method.id, new STentry(nestingLevel, method.getType(), oldOffset));
					method.offset = oldOffset;
					type.allMethods.set(oldOffset, ((MethodTypeNode) method.getType()).fun);
				} else{ // no-overriding
					virtualTable.put(method.id, new STentry(nestingLevel, method.getType(), decOffset));
					method.offset = decOffset;
					decOffset++;
					type.allMethods.add(((MethodTypeNode) method.getType()).fun);
				}
			}
		}

		//rimuovere la hashmap corrente poiche' esco dallo scope
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level

		return null;
	}

	@Override
	public Void visitNode(MethodNode n) {
		if (print) printNode(n);

		Map<String, STentry> hm = symTable.get(nestingLevel); // prendo la virtual table della classe

		// tipi dei parametri
		List<TypeNode> parTypes = new ArrayList<>();
		for (ParNode par : n.parlist) parTypes.add(par.getType());


		MethodTypeNode methodType = new MethodTypeNode(new ArrowTypeNode(parTypes, n.retType));
		n.setType(methodType);

		// entro nel nesting level del metodo
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset;

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
		decOffset=prevNLDecOffset;

		return null;
	}

	@Override
	public Void visitNode(ClassCallNode n) {
		if (print) printNode(n);

		// object lo troviamo nella SymbolTable
		STentry objectEntry = stLookup(n.objectId);
		if (objectEntry == null) {
			System.out.println("Object id " + n.objectId + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			// metodo lo troviammo nella ClassTable -> VirtualTable della classe di object
			if (!(objectEntry.type instanceof RefTypeNode)){
				System.out.println("Object id " + n.objectId + " at line "+ n.getLine() + " has not a RefTypeNode");
				stErrors++;
			} else{
				String classId = ((RefTypeNode) objectEntry.type).id;
				STentry methodEntry = classTable.get(classId).get(n.methodId);
				if (methodEntry == null){
					System.out.println("Method id " + n.objectId + " at line "+ n.getLine() + " not declared");
					stErrors++;
				}else{
					n.entry = objectEntry;
					n.methodEntry = methodEntry;
					n.nl = nestingLevel;
				}
			}
		}

		for (Node arg : n.arglist) visit(arg);

		return null;
	}

	@Override
	public Void visitNode(NewNode n) {
		if (print) printNode(n);

		// controllo che ID sia in classTable
		if (!(classTable.containsKey(n.classId))){
			System.out.println("Class id " + n.classId + " at line "+ n.getLine() + " not declared");
			stErrors++;
		}else{
			STentry classEntry = symTable.get(0).get(n.classId);
			if (classEntry == null){ // controllare che la classe sia stata dichiarata al livello 0
				System.out.println("Class id " + n.classId + " at line "+ n.getLine() + " not declared at level 0");
				stErrors++;
			}else{
				n.entry=classEntry;
			}
		}

		for (Node arg : n.arglist) visit(arg);

		return null;
	}

	@Override
	public Void visitNode(EmptyNode n) {
		if (print) printNode(n);

		return null;
	}



}
