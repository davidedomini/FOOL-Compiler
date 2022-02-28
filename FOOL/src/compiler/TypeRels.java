package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.sql.Ref;
import java.util.HashMap;
import java.util.Map;

public class TypeRels {

	public static Map<String, String> superType = new HashMap<>(); // mappa il nome di una classe in quello della sua super classe

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {

		if( ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode))
		|| ((a instanceof BoolTypeNode) && (b instanceof BoolTypeNode))
		|| ((a instanceof IntTypeNode) && (b instanceof IntTypeNode))
		){
			return true;
		}

		// OBEJECT-ORIENTATION EXTENSION
		if (a instanceof EmptyTypeNode && b instanceof RefTypeNode){
			return true;
		}

		if (a instanceof RefTypeNode && b instanceof RefTypeNode){
			String idA = ((RefTypeNode) a).id;
			String idB = ((RefTypeNode) b).id;
			//If necessario per dichiarazione di variabili del tipo A var = new A(); se no andavamo in superType e non trovavamo nulla
			if(idA.equals(idB)) return true;
			return existSuperType(idA, idB);
		}

		if (a instanceof ArrowTypeNode && b instanceof ArrowTypeNode){
			ArrowTypeNode funA = (ArrowTypeNode) a;
			ArrowTypeNode funB = (ArrowTypeNode) b;
			if (isSubtype(funA.ret,funB.ret)){ //co-varianza
				//contro-varianza
				if (funA.parlist.size() == funB.parlist.size()){
					for (int i=0; i<funA.parlist.size(); i++){
						if(!(isSubtype(funB.parlist.get(i),funA.parlist.get(i)))){
							return false;
						}
					}
					return true;
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
			if (t != null && t.equals(b)){
				return true;
			}
			a = t;
		}while(t != null);

		return false;
	}

	public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b){

		if (isSubtype(a, new IntTypeNode()) && isSubtype(b, new IntTypeNode())){
			if (a instanceof IntTypeNode || b instanceof IntTypeNode){
				return new IntTypeNode();
			} else {
				return new BoolTypeNode();
			}
		}

		if ((a instanceof RefTypeNode || a instanceof EmptyTypeNode)
				&& (b instanceof RefTypeNode || b instanceof EmptyTypeNode)){

			if(a instanceof EmptyTypeNode) return b;
			if(b instanceof EmptyTypeNode) return a;

			String superClassId = "";
			String classA =  ((RefTypeNode) a).id;
			String classB =  ((RefTypeNode) b).id;

			if(classA.equals(classB)) return a;


			do{
				superClassId = superType.get(classA);
				RefTypeNode superClass = new RefTypeNode(superClassId);
				if (isSubtype(b, superClass)){
					return superClass;
				}
				classA = superClassId;
			}while(superClassId != null);
		}

		return null;
	}

}
