package de.lmu.dbs.ciaa.classifier;

/**
 * Analysis routines on top of Trees.
 * 
 * @author Thomas Weber
 *
 */
public class TreeAnalyzer {

	private ForestParameters params;
	
	/**
	 * Create analyzer.
	 * 
	 * @param params
	 * @throws Exception
	 */
	public TreeAnalyzer(ForestParameters params) throws Exception {
		this.params = params;
	}

	/**
	 * Returns a visualization of all node features of the forest. For debugging use.
	 * 
	 * @param data the array to store results (additive)
	 */
	public void visualize(Tree rtree, int[][] data) {
		rtree.getTree().visualize(data);
	}

	/**
	 * Returns the number of leafs in the tree.
	 * 
	 * @return
	 */
	public int getNumOfLeafs(Tree rtree) {
		return getNumOfLeafs(rtree.getTree().left) + getNumOfLeafs(rtree.getTree().right);
	}
	
	/**
	 * Returns the number of leafs in the tree. (Internal)
	 * 
	 * @param node
	 * @return
	 */
	private int getNumOfLeafs(Node node) {
		if (node == null) return 0;
		if (node.isLeaf()) return 1;
		return getNumOfLeafs(node.left) + getNumOfLeafs(node.right);
	}
	
	/**
	 * Returns the number of leafs / nodes at each depth level of 
	 * the tree.
	 * 
	 * @return
	 */
	public int[] getDepthCounts(Tree rtree) {
		int[] counts = new int[params.maxDepth+1];
		getDepthCountsRec(rtree.getTree(), counts, 0);
		return counts;
	}
	
	/**
	 * Internal
	 * 
	 * @param node
	 * @param counts
	 * @param depth
	 */
	private void getDepthCountsRec(Node node, int[] counts, int depth) {
		counts[depth]++;
		if (!node.isLeaf()) {
			getDepthCountsRec(node.left, counts, depth+1);
			getDepthCountsRec(node.right, counts, depth+1);
		}
	}
	
	/**
	 * Returns a readable stat for tree depth counts.
	 * 
	 * @return
	 */
	public String getDepthCountsString(Tree rtree) {
		String ret = "";
		int[] counts = getDepthCounts(rtree);
		for(int i=0; i<counts.length; i++) {
			ret += i + ": " + counts[i] + " (of possible " + (int)Math.pow(2, i) + ")" + "\n";
		}
		return ret;
	}
	
	/**
	 * Returns a string representation of the tree, for overview.
	 * 
	 * @return
	 */
	public String getTreeVisualization(Tree rtree) {
		String[] s = new String[params.maxDepth+1];
		int b = (int)Math.pow(2, params.maxDepth) * 2;
		for(int i=0; i<s.length; i++) {
			s[i] = multiplyString(b+10, " ");
		}
		getTreeVisualizationRec(rtree.getTree(), s, 0, 0, 0, b);
		String ret = "";
		for(int i=0; i<s.length; i++) {
			ret += i + ": " + s[i]+"\n";
		}
		return ret;
	}

	/**
	 * Internal
	 * 
	 * @return
	 */
	private void getTreeVisualizationRec(Node node, String[] s, int mode, int part, int depth, int all) {
		int div = (int)Math.pow(2, depth);
		int x = (all/div) * part;
		int i = x + (all/div)/2 + 2;
		String c = node.isLeaf() ? "#" : ((mode==0) ? "0" : ((mode==1) ? "L" : "R"));
		s[depth] = s[depth].substring(0, i) + c + s[depth].substring(i+1, s[depth].length());
		
		if (!node.isLeaf()) {
			int nextPartL = part*2;
			getTreeVisualizationRec(node.left, s, 1, nextPartL, depth+1, all);
	
			int nextPartR = part*2+1;
			getTreeVisualizationRec(node.right, s, 2, nextPartR, depth+1, all);
		}
	}
	
	/**
	 * Returns a String containing times times the String s.
	 * 
	 * @param times
	 * @param s
	 * @return
	 */
	private String multiplyString(int times, String s) {
		String ret = "";
		for(int i=0; i<times; i++) ret+=s;
		return ret;
	}


}
