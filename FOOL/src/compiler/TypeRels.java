package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;

public class TypeRels {

	public static Map<String, String> superType = new HashMap<>();

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {
//		return a.getClass().equals(b.getClass()) || ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode));

		if (a instanceof EmptyTypeNode && b instanceof RefTypeNode){
			return true;
		}

		if (a.getClass().equals(b.getClass()) || ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode))){
			return true;
		}

		if (a instanceof RefTypeNode && b instanceof RefTypeNode){
			return existSuperType(a.getClass().toString(),b.getClass().toString());
		}

		if (a instanceof ArrowTypeNode && b instanceof ArrowTypeNode){
			ArrowTypeNode funA = (ArrowTypeNode) a;
			ArrowTypeNode funB = (ArrowTypeNode) b;
			if (isSubtype(funA.ret,funB.ret)){
				if (funA.parlist.size() == funB.parlist.size()){

				}else{
					return false;
				}
			}else{
				return false;
			}
		}

		return false;
	}

	private static boolean existSuperType(String a, String b){
		String t = "";

		do{
			t = superType.get(a);
			if (t.equals(b)){
				return true;
			}
			a = t;
		}while(t != null);

		return false;
	}

}
