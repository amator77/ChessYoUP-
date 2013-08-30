package com.chessyoup.model.pgn;

import java.util.HashMap;

import com.chessyoup.model.GameTree.Node;

public class HTMLPGNText implements PgnTokenReceiver {

	private StringBuilder sb = new StringBuilder();
	private int prevType = PgnToken.EOF;
	int nestLevel = 0;
	boolean col0 = true;
	Node currNode = null;
	final static int indentStep = 15;
	int currPos = 0, endPos = 0;
	boolean upToDate = false;
	PGNOptions options;

	private static class NodeInfo {
		int l0, l1;

		NodeInfo(int ls, int le) {
			l0 = ls;
			l1 = le;
		}
	}

	HashMap<Node, NodeInfo> nodeToCharPos;

	public HTMLPGNText(PGNOptions options) {
		nodeToCharPos = new HashMap<Node, NodeInfo>();
		this.options = options;
	}

	public String getHTMLData() {
		return sb.toString();
	}

	public final int getCurrPos() {
		return currPos;
	}

	public boolean isUpToDate() {
		return upToDate;
	}

	int paraStart = 0;
	int paraIndent = 0;
	boolean paraBold = false;

	private final void newLine() {
		newLine(false);
	}

	private final void newLine(boolean eof) {
		if (!col0) {
			if (paraIndent > 0) {
				int paraEnd = sb.length();
				int indent = paraIndent * indentStep;				
			}
			if (paraBold) {
				int paraEnd = sb.length();				
			}
			if (!eof)
				sb.append("<br/>");
			paraStart = sb.length();
			paraIndent = nestLevel;
			paraBold = false;
		}
		col0 = true;
	}

	boolean pendingNewLine = false;

	public void processToken(Node node, int type, String token) {
		if ((prevType == PgnToken.RIGHT_BRACKET)
				&& (type != PgnToken.LEFT_BRACKET)) {
			if (options.view.headers) {
				col0 = false;
				newLine();
			} else {
				sb.delete(0, sb.length());
				paraBold = false;
			}
		}
		if (pendingNewLine) {
			if (type != PgnToken.RIGHT_PAREN) {
				newLine();
				pendingNewLine = false;
			}
		}
		switch (type) {
		case PgnToken.STRING:
			sb.append(" \"");
			sb.append(token);
			sb.append('"');
			break;
		case PgnToken.INTEGER:
			if ((prevType != PgnToken.LEFT_PAREN)
					&& (prevType != PgnToken.RIGHT_BRACKET) && !col0)
				sb.append(' ');
			sb.append(token);
			col0 = false;
			break;
		case PgnToken.PERIOD:
			sb.append('.');
			col0 = false;
			break;
		case PgnToken.ASTERISK:
			sb.append(" *");
			col0 = false;
			break;
		case PgnToken.LEFT_BRACKET:
			sb.append('[');
			col0 = false;
			break;
		case PgnToken.RIGHT_BRACKET:
			sb.append("]\n");
			col0 = false;
			break;
		case PgnToken.LEFT_PAREN:
			nestLevel++;
			if (col0)
				paraIndent++;
			newLine();
			sb.append('(');
			col0 = false;
			break;
		case PgnToken.RIGHT_PAREN:
			sb.append(')');
			nestLevel--;
			pendingNewLine = true;
			break;
		case PgnToken.NAG:
			sb.append(Node.nagStr(Integer.parseInt(token)));
			col0 = false;
			break;
		case PgnToken.SYMBOL: {
			if ((prevType != PgnToken.RIGHT_BRACKET)
					&& (prevType != PgnToken.LEFT_BRACKET) && !col0)
				sb.append(' ');
			int l0 = sb.length();
			sb.append(token);
			int l1 = sb.length();
			nodeToCharPos.put(node, new NodeInfo(l0, l1));			
			if (endPos < l0)
				endPos = l0;
			col0 = false;
			if (nestLevel == 0)
				paraBold = true;
			break;
		}
		case PgnToken.COMMENT:
			if (prevType == PgnToken.RIGHT_BRACKET) {
			} else if (nestLevel == 0) {
				nestLevel++;
				newLine();
				nestLevel--;
			} else {
				if ((prevType != PgnToken.LEFT_PAREN) && !col0) {
					sb.append(' ');
				}
			}
			int l0 = sb.length();
			sb.append(token.replaceAll("[ \t\r\n]+", " ").trim());
			int l1 = sb.length();			
			col0 = false;
			if (nestLevel == 0)
				newLine();
			break;
		case PgnToken.EOF:
			newLine(true);
			upToDate = true;
			break;
		}
		prevType = type;
	}

	public void clear() {
		sb.delete(0, sb.length());
		prevType = PgnToken.EOF;
		nestLevel = 0;
		col0 = true;
		currNode = null;
		currPos = 0;
		endPos = 0;
		nodeToCharPos.clear();
		paraStart = 0;
		paraIndent = 0;
		paraBold = false;
		pendingNewLine = false;

		upToDate = false;
	}
	
	public void setCurrent(Node node) {
		
		NodeInfo ni = nodeToCharPos.get(node);
		if ((ni == null) && (node != null) && (node.getParent() != null))
			ni = nodeToCharPos.get(node.getParent());
		if (ni != null) {			
			currPos = ni.l0;
		} else {
			currPos = 0;
		}
		currNode = node;
	}
}
