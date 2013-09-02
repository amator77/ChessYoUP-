package com.chessyoup.game.view;

import java.util.HashMap;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.view.View;

import com.chessyoup.model.GameTree.Node;
import com.chessyoup.model.pgn.PGNOptions;
import com.chessyoup.model.pgn.PgnToken;
import com.chessyoup.model.pgn.PgnTokenReceiver;

public class PgnScreenTextView implements PgnTokenReceiver {
	private SpannableStringBuilder sb = new SpannableStringBuilder();
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

	public PgnScreenTextView(PGNOptions options) {
		nodeToCharPos = new HashMap<Node, NodeInfo>();
		this.options = options;
	}

	public final SpannableStringBuilder getSpannableData() {
		return sb;
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
				sb.setSpan(new LeadingMarginSpan.Standard(indent),
						paraStart, paraEnd,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (paraBold) {
				int paraEnd = sb.length();
				sb.setSpan(new StyleSpan(Typeface.BOLD), paraStart,
						paraEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (!eof)
				sb.append('\n');
			paraStart = sb.length();
			paraIndent = nestLevel;
			paraBold = false;
		}
		col0 = true;
	}

	boolean pendingNewLine = false;

	/** Makes moves in the move list clickable. */
	private final static class MoveLink extends ClickableSpan {
		private Node node;

		MoveLink(Node n) {
			node = n;
		}

		@Override
		public void onClick(View widget) {
			// if (ctrl != null)
			// ctrl.goNode(node);
		}

		@Override
		public void updateDrawState(TextPaint ds) {
		}
	}

	public void processToken(Node node, int type, String token) {
		
		if( token == null ){
			return;
		}
		
		if ((prevType == PgnToken.RIGHT_BRACKET)
				&& (type != PgnToken.LEFT_BRACKET)) {
			if (options.view.headers) {
				col0 = false;
				newLine();
			} else {
				sb.clear();
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
			sb.setSpan(new MoveLink(node), l0, l1,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
			int color = ColorTheme.instance().getColor(
					ColorTheme.PGN_COMMENT);
			sb.setSpan(new ForegroundColorSpan(color), l0, l1,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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

	@Override
	public void clear() {
		sb.clear();
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

	BackgroundColorSpan bgSpan = new BackgroundColorSpan(0xff888888);

	@Override
	public void setCurrent(Node node) {
		sb.removeSpan(bgSpan);
		NodeInfo ni = nodeToCharPos.get(node);
		if ((ni == null) && (node != null) && (node.getParent() != null))
			ni = nodeToCharPos.get(node.getParent());
		if (ni != null) {
			int color = ColorTheme.instance().getColor(
					ColorTheme.CURRENT_MOVE);
			bgSpan = new BackgroundColorSpan(color);
			sb.setSpan(bgSpan, ni.l0, ni.l1,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			currPos = ni.l0;
		} else {
			currPos = 0;
		}
		currNode = node;
	}
}
