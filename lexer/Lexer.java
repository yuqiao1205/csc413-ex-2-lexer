package lexer;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

/**
 *  The Lexer class is responsible for scanning the source file
 *  which is a stream of characters and returning a stream of
 *  tokens; each token object will contain the string (or access
 *  to the string) that describes the token along with an
 *  indication of its location in the source program to be used
 *  for error reporting; we are tracking line numbers; white spaces
 *  are space, tab, newlines
 */
public class Lexer {
  private boolean atEOF = false;
  // next character to process
  private char ch;
  private SourceReader source;
  private int lineNumber;

  // positions in line of current token
  private int startPosition, endPosition;

  /**
   *  Lexer constructor
   * @param sourceFile is the name of the File to read the program source from
   */
  public Lexer( String sourceFile ) throws Exception {
    // init token table
    new TokenType();
    source = new SourceReader( sourceFile );
    ch = source.read();
  }

  /**
   *  newIdTokens are either ids or reserved words; new id's will be inserted
   *  in the symbol table with an indication that they are id's
   *  @param id is the String just scanned - it's either an id or reserved word
   *  @param startPosition is the column in the source file where the token begins
   *  @param endPosition is the column in the source file where the token ends
   *  @return the Token; either an id or one for the reserved words
   */
  public Token newIdToken( String id, int startPosition, int endPosition, int lineNumber) {
    return new Token(
      startPosition,
      endPosition,
      lineNumber, Symbol.symbol( id, Tokens.Identifier ));
  }

  /**
   *  integer tokens are inserted in the symbol table; we don't convert the
   *  integer strings to integers until we load the bytecodes for interpreting;
   *  this ensures that any machine numeric dependencies are deferred
   *  until we actually run the program; i.e. the numeric constraints of the
   *  hardware used to compile the source program are not used
   *  @param number is the int String just scanned
   *  @param startPosition is the column in the source file where the int begins
   *  @param endPosition is the column in the source file where the int ends
   *  @return the int Token
   */
//  public Token newIntegerToken( String number, int startPosition, int endPosition, int lineNumber) {
//    return new Token(
//            startPosition,
//            endPosition,
//            lineNumber,
//            Symbol.symbol( number, Tokens.INTeger )
//    );
//  }
  /**
   *  number tokens are inserted in the symbol table; we don't convert the
   *  numeric strings to numbers until we load the bytecodes for interpreting;
   *  this ensures that any machine numeric dependencies are deferred
   *  until we actually run the program; i.e. the numeric constraints of the
   *  hardware used to compile the source program are not used
   *  @param number is the int String just scanned
   *  @param startPosition is the column in the source file where the int begins
   *  @param endPosition is the column in the source file where the int ends
   *  @return the int Token
   */
  public Token newToken(String number, int startPosition, int endPosition, int lineNumber, Tokens kind) {
    return new Token(
      startPosition,
      endPosition,
      lineNumber,
      Symbol.symbol(number, kind)
    );
  }

//  public Token newDateToken( String date, int startPosition, int endPosition, int lineNumber) {
//    return new Token(
//            startPosition,
//            endPosition,
//            lineNumber,
//            Symbol.symbol( date, Tokens.DateLit )
//    );
//  }



  /**
   *  build the token for operators (+ -) or separators (parens, braces)
   *  filter out comments which begin with two slashes
   *  @param tokenString is the String representing the token
   *  @param startPosition is the column in the source file where the token begins
   *  @param endPosition is the column in the source file where the token ends
   *  @return the Token just found
   */
  public Token makeToken( String tokenString, int startPosition, int endPosition ) {
    // filter comments
    if( tokenString.equals("//") ) {
      try {
        int oldLine = source.getLineno();

        do {
          ch = source.read();
        } while( oldLine == source.getLineno() );
      } catch (Exception e) {
        atEOF = true;
      }

      return nextToken();
    }

    // ensure it's a valid token
    // Why not just set to null as we have already found illegal token
    Symbol symbol = Symbol.symbol( tokenString, Tokens.BogusToken );

    if( symbol == null ) {
      System.out.println( "******** illegal character: " + tokenString );
      atEOF = true;
      return nextToken();
    }

    return new Token( startPosition, endPosition, lineNumber,symbol );
  }

  /**
   *  @return the next Token found in the source file
   */
  public Token nextToken() {
    // ch is always the next char to process
    if( atEOF ) {
      if( source != null ) {
        source.close();
        source = null;
      }

      return null;
    }

    try {
      // scan past whitespace
      while( Character.isWhitespace( ch )) {
        ch = source.read();
      }
    } catch( Exception e ) {
      atEOF = true;
      return nextToken();
    }

    startPosition = source.getPosition();
    endPosition = startPosition - 1;
    lineNumber = source.getLineno();


    if( Character.isJavaIdentifierStart( ch )) {
      // return tokens for ids and reserved words
      String id = "";

      try {
        do {
          endPosition++;
          id += ch;
          ch = source.read();
        } while( Character.isJavaIdentifierPart( ch ));
      } catch( Exception e ) {
        atEOF = true;
      }

      return newIdToken( id, startPosition, endPosition, lineNumber );
    }

    if( Character.isDigit( ch )) {
      // return number tokens
      String number = "";
      Tokens token = Tokens.INTeger;

      try {
        number += readNumber();
      } catch (Exception e) {
        atEOF = true;
      }

      try {
        if('.' == ch || '/' == ch || '-' == ch) {
          endPosition++;
          number += ch;
          ch = source.read();
          number += readNumber();

          if(isNumberLit(number)) {
            token = Tokens.NumberLit;
          } else if ('/' == ch || '-' == ch) {
            number += ch;
            ch = source.read();
            number += readNumber();
            if (isDateLit(number)){
              token = Tokens.DateLit;
            }
          }
        }
      } catch(Exception e) {
        atEOF = true;
      }
      return newToken( number, startPosition, endPosition, lineNumber, token );
    }

    // At this point the only tokens to check for are one or two
    // characters; we must also check for comments that begin with
    // 2 slashes
    String charOld = "" + ch;
    String op = charOld;
    Symbol sym;
    try {
      endPosition++;
      ch = source.read();
      op += ch;

      // check if valid 2 char operator; if it's not in the symbol
      // table then don't insert it since we really have a one char
      // token
      sym = Symbol.symbol( op, Tokens.BogusToken );
      if (sym == null) {
        // it must be a one char token
        return makeToken( charOld, startPosition, endPosition );
      }

      endPosition++;
      ch = source.read();

      return makeToken( op, startPosition, endPosition );
    } catch( Exception e ) { /* no-op */ }

    atEOF = true;
    if( startPosition == endPosition ) {
      op = charOld;
    }

    return makeToken( op, startPosition, endPosition );
  }

  private String readNumber() throws Exception {
    String number="";
    do {
      endPosition++;
      number += ch;
      ch = source.read();
    } while (Character.isDigit(ch));
    return number;
  }

  private boolean isNumberLit(String number) {
    return number.matches("\\d+\\.\\d+");
  }

  private boolean isDateLit(String date) {
    return date.matches("(\\d\\d?)[/-](\\d\\d?)[/-](\\d\\d?)")||
            date.matches("(\\d\\d?)[/-](\\d\\d?)[/-](\\d\\d\\d\\d)");
  }

  public static void main(String args[]) {

    if (args.length == 0){
      System.out.println("usage: java lexer.Lexer filename.x");
      return;
    }

    Token token;

    try {
      Lexer lex = new Lexer(args[0]);

      while (true) {
        token = lex.nextToken();

        if (token == null) {
          break;
        }

        System.out.printf("%-11s left: %-8d right: %-8d line: %-8d %s%n",token,token.getLeftPosition(),token.getRightPosition(),token.getLineNumber(),token.getKind());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    String filePath = (args[0]);

    try (LineNumberReader lineReader = new LineNumberReader(new FileReader(filePath))){
      String lineText = null;

      while ((lineText = lineReader.readLine()) != null) {
        System.out.printf( "%3d: %s%n",lineReader.getLineNumber(), lineText);
      }
    } catch (IOException ex) {
      System.err.println(ex);
    }


  }

}