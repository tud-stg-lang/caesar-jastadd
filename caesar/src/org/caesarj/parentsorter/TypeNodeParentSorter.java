package org.caesarj.parentsorter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.caesarj.ast.CjClassDecl;

/**
 * Sorts the parent graph of a Caesar class, using a modified deep-first search
 * 
 * @author vaidas
 *
 */
public class TypeNodeParentSorter extends GraphSorter {
	
	/* map from type node to internal graph node */
	protected Map<CjClassDecl, NodeWrapper> wrapperMap = new HashMap<CjClassDecl, NodeWrapper>();
	
	/**
	 * Constructor
	 * 
	 * @param root 	node to be sorted
	 */
	public TypeNodeParentSorter(CjClassDecl root) {
		setRoot(wrapTypeNode(root));
	}
	
	/**
	 * Adapts the graph of class parents to the graph of the sorting algorithm 
	 */
	protected class NodeWrapper extends Node {
		/* corresponding type node */
		protected CjClassDecl typeNode;
				
		/**
		 * Constructor
		 */
		public NodeWrapper(CjClassDecl typeNode) {
			this.typeNode = typeNode;			
		}
		
		/**
		 * Get corresponding type node 
		 */
		public CjClassDecl getTypeNode() {
			return typeNode;
		}
		
		/**
		 *	The node name to be displayed in messages
		 */
		public String getDisplayName() {
			return typeNode.getID();
		}
		
		/**
		 * Return the parents of the type node as outgoing nodes of the sorting graph
		 */
		public List<Node> calcOutgoingNodes() {
			List<Node> nodes = new ArrayList<Node>();
			Iterator i = typeNode.getExplicitParents().iterator();
			while (i.hasNext()) {
				Object o = i.next();
				// parent may be unresolved
				if (o instanceof CjClassDecl) {
					CjClassDecl next = (CjClassDecl)o;
					if (next.isClassDecl())
						nodes.add(wrapTypeNode((CjClassDecl) next));
				}
			}
			return nodes;
		}
		
		/**
		 * Display as string, for debugging purposes
		 */
		public String toString() {
			return getDisplayName();
		}
	}
	
	/**
	 *	Get the adapter node for the given type node 
	 */
	public NodeWrapper wrapTypeNode(CjClassDecl typeNode) {
		NodeWrapper wrp = wrapperMap.get(typeNode);
		if (wrp == null) {
			wrp = new NodeWrapper(typeNode);
			wrapperMap.put(typeNode, wrp);
		}
		return wrp;
	}
	
	/**
	 *	Get the sorted list of the parents 
	 */
	public List<CjClassDecl> getSortedTypeNodes() throws CycleFoundException {
		List<CjClassDecl> typeNodes = new ArrayList<CjClassDecl>();
		for (Node n : getSortedNodes()) {
			typeNodes.add(((NodeWrapper)n).getTypeNode());
		}
		return typeNodes;
	}
}
