package org.openscad.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import org.openscad.psi.OpenSCADTypes;
import com.intellij.psi.TokenType;

%%

%class OpenSCADLexerImpl
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

%{
  public OpenSCADLexerImpl() {
    this((java.io.Reader)null);
  }
%}

EOL=\n|\r|\r\n
WHITE_SPACE=[ \t\f]

DIGIT=[0-9]
LETTER=[a-zA-Z_]
IDENT=(\$)?{LETTER}({LETTER}|{DIGIT})*
NUMBER=({DIGIT}+(\.{DIGIT}*)?|{DIGIT}*\.{DIGIT}+)([eE][-+]?{DIGIT}+)?
STRING=\"([^\"\\\r\n]|\\.)*\"
LINE_COMMENT_TEXT="//"[^\r\n]*
BLOCK_COMMENT_TEXT="/*"([^*]|\*+[^*/])*\*+"/"

%%

<YYINITIAL> {
  {WHITE_SPACE}+ { return TokenType.WHITE_SPACE; }
  {EOL}+ { return TokenType.WHITE_SPACE; }
  
  // Comments (must come before operators)
  {LINE_COMMENT_TEXT} { return OpenSCADTypes.LINE_COMMENT; }
  {BLOCK_COMMENT_TEXT} { return OpenSCADTypes.BLOCK_COMMENT; }
  
  // Keywords (must come before IDENT)
  "module" { return OpenSCADTypes.MODULE_KW; }
  "function" { return OpenSCADTypes.FUNCTION_KW; }
  "include" { return OpenSCADTypes.INCLUDE_KW; }
  "use" { return OpenSCADTypes.USE_KW; }
  "if" { return OpenSCADTypes.IF_KW; }
  "else" { return OpenSCADTypes.ELSE_KW; }
  "for" { return OpenSCADTypes.FOR_KW; }
  "intersection_for" { return OpenSCADTypes.INTERSECTION_FOR_KW; }
  "let" { return OpenSCADTypes.LET_KW; }
  "true" { return OpenSCADTypes.BOOL_LITERAL; }
  "false" { return OpenSCADTypes.BOOL_LITERAL; }
  "undef" { return OpenSCADTypes.UNDEF_LITERAL; }
  
  // Multi-character operators (must come before single-character)
  "==" { return OpenSCADTypes.EQ_EQ; }
  "!=" { return OpenSCADTypes.NOT_EQ; }
  "<=" { return OpenSCADTypes.LE; }
  ">=" { return OpenSCADTypes.GE; }
  "&&" { return OpenSCADTypes.AND_AND; }
  "||" { return OpenSCADTypes.OR_OR; }
  ".." { return OpenSCADTypes.DOT_DOT; }
  
  // Single-character operators and punctuation
  "=" { return OpenSCADTypes.EQ; }
  "<" { return OpenSCADTypes.LT; }
  ">" { return OpenSCADTypes.GT; }
  "!" { return OpenSCADTypes.NOT; }
  "?" { return OpenSCADTypes.QUESTION; }
  ":" { return OpenSCADTypes.COLON; }
  "+" { return OpenSCADTypes.PLUS; }
  "-" { return OpenSCADTypes.MINUS; }
  "*" { return OpenSCADTypes.MUL; }
  "/" { return OpenSCADTypes.DIV; }
  "%" { return OpenSCADTypes.MOD; }
  "^" { return OpenSCADTypes.POW; }
  "(" { return OpenSCADTypes.LPAREN; }
  ")" { return OpenSCADTypes.RPAREN; }
  "{" { return OpenSCADTypes.LBRACE; }
  "}" { return OpenSCADTypes.RBRACE; }
  "[" { return OpenSCADTypes.LBRACKET; }
  "]" { return OpenSCADTypes.RBRACKET; }
  ";" { return OpenSCADTypes.SEMICOLON; }
  "," { return OpenSCADTypes.COMMA; }
  "#" { return OpenSCADTypes.HASH; }
  
  // Literals (must come after keywords)
  {NUMBER} { return OpenSCADTypes.NUMBER; }
  {STRING} { return OpenSCADTypes.STRING; }
  {IDENT} { return OpenSCADTypes.IDENT; }
  
  // Error fallback
  . { return TokenType.BAD_CHARACTER; }
}
