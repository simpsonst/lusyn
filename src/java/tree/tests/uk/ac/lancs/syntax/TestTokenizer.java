// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright 2021, Lancaster University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 * 
 *  * Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.syntax;

import java.io.Reader;
import java.io.StringReader;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * 
 * @author simpsons
 */
public class TestTokenizer {
    private static final String NUMBER_PATTERN =
        "(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[Ee](?:[+-])?[0-9]+)?";

    static enum MyTokenType {
        @Epsilon
        EPSILON,

        @Literal("[ \\t\\x0B\\f]+")
        WHITESPACE,

        @Literal("\\r\\n|\\r|\\n")
        NEWLINE,

        @Literal("/\\*\\*")
        DOC_COMMENT_OPEN,

        @Literal("/\\*")
        COMMENT_OPEN,

        @Literal("\\*/")
        COMMENT_CLOSE,

        @Literal("//")
        COMMENT,

        @Literal("[+-]")
        PLUSMIN,

        @Literal("[*/]")
        MULTDIV,

        @Literal("\\(")
        OPEN,

        @Literal("\\)")
        CLOSE,

        @Literal("(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[Ee](?:[+-])?[0-9]+)?")
        NUMBER,

        @Literal("[a-zA-Z_][a-zA-Z_0-9]*")
        IDENTIFIER,

        @Literal("\"(?:" + "[\\p{Print}&&[^\"\\\\]]" + "|"
            + "\\\\[\"'nftba\\\\]" + "|" + "\\\\u[0-9a-fA-F]{4})*\"")
        STRING_LITERAL,

        @Unmatched
        UNMATCHED,

        @Production(
        { "MULT_EXPR", "EXPR_TAIL" })
        EXPR,

        @Production(
        { "PLUSMIN", "MULT_EXPR", "EXPR_TAIL" })
        /* @Production( { "PLUSMIN", "EXPR" }) */
        @Production({ })
        EXPR_TAIL,

        @Production(
        { "UNARY_EXPR", "MULT_EXPR_TAIL" })
        MULT_EXPR,

        @Production(
        { "MULTDIV", "UNARY_EXPR", "MULT_EXPR_TAIL" })
        @Production({ })
        MULT_EXPR_TAIL,

        @Production(
        { "PLUSMIN", "UNARY_EXPR" })
        @Production("PRIMARY_EXPR")
        UNARY_EXPR,

        @Production("NUMBER")
        @Production("IDENTIFIER")
        @Production(
        { "OPEN", "EXPR", "CLOSE" })
        PRIMARY_EXPR,

        DOC_COMMENT;
    }

    static class CommentEliminator implements Consumer<Token<MyTokenType>> {
        private final Consumer<? super Token<MyTokenType>> sub;

        private Token<MyTokenType> prior;

        enum Mode {
            NORMAL, SHORT_COMMENT, LONG_COMMENT, DOC_COMMENT;
        }

        private Mode mode = Mode.NORMAL;

        private final StringBuilder docComment = new StringBuilder();

        private TextPosition docStart;

        public CommentEliminator(Consumer<? super Token<MyTokenType>> sub) {
            this.sub = sub;
        }

        @Override
        public void accept(Token<MyTokenType> token) {
            /* A NUMBER must not be followed by an IDENTIFIER or another
             * NUMBER. */
            if (prior != null) {
                switch (token.type) {
                case IDENTIFIER:
                    /* The errant sequence is terminated, so append the
                     * identifier, and report as unmatched. */
                    sub.accept(Token.of(MyTokenType.UNMATCHED, prior.start,
                                        token.end, prior.text + token.text));
                    prior = null;
                    return;

                case NUMBER:
                    /* The errant sequence is extended, so append the
                     * number, and store as unmatched. */
                    prior = Token.of(MyTokenType.UNMATCHED, prior.start,
                                     token.end, prior.text + token.text);
                    return;

                default:
                    /* The sequence is terminated, so report what we
                     * have, whether it's an error or not, and prepare
                     * for a fresh sequence. */
                    sub.accept(prior);
                    prior = null;
                    break;
                }
            }

            switch (mode) {
            case NORMAL:
                switch (token.type) {
                case NUMBER:
                    /* This could be the start of an errant sequence
                     * such as a NUMBER followed by an IDENTIFIER or
                     * another NUMBER. Don't report it yet. */
                    prior = token;
                    break;

                case UNMATCHED:
                    sub.accept(token);
                    break;

                case DOC_COMMENT_OPEN:
                    docComment.delete(0, docComment.length());
                    docStart = token.end;
                    mode = Mode.DOC_COMMENT;
                    break;

                case COMMENT_OPEN:
                    mode = Mode.LONG_COMMENT;
                    break;

                case COMMENT:
                    mode = Mode.SHORT_COMMENT;
                    break;

                case WHITESPACE:
                case NEWLINE:
                    break;

                default:
                    sub.accept(token);
                    break;
                }
                break;

            case DOC_COMMENT:
                switch (token.type) {
                case COMMENT_CLOSE:
                    mode = Mode.NORMAL;
                    sub.accept(Token.of(MyTokenType.DOC_COMMENT, docStart,
                                        token.start, docComment.toString()));
                    break;

                default:
                    docComment.append(token.text);
                    break;
                }
                break;

            case LONG_COMMENT:
                switch (token.type) {
                case COMMENT_CLOSE:
                    mode = Mode.NORMAL;
                    break;

                case UNMATCHED:
                    break;

                default:
                    break;
                }
                break;

            case SHORT_COMMENT:
                switch (token.type) {
                case NEWLINE:
                    mode = Mode.NORMAL;
                    break;

                case UNMATCHED:
                    break;

                default:
                    break;
                }
                break;
            }
        }
    }

    interface Expression {

    }

    interface AdditiveExpression extends Expression {
        Expression left();

        Expression right();

        boolean add();
    }

    interface VariableExpression extends Expression {
        String name();
    }

    interface NumberExpression extends Expression {
        Number value();
    }

    private static void print(String prefix, Node<MyTokenType> root) {
        if (root instanceof Token) {
            System.err.printf("%s%s%n", prefix, root);
        } else if (root instanceof NonTerminal) {
            NonTerminal<MyTokenType> nt = (NonTerminal<MyTokenType>) root;
            System.err.printf("%s%s [%n", prefix, nt.type);
            for (Node<MyTokenType> sn : nt.parts)
                print(prefix + "  ", sn);
            System.err.printf("%s]%n", prefix);
        }
    }

    public static void main(String[] args) throws Throwable {
        if (false) {
            Pattern p = Pattern.compile(NUMBER_PATTERN);
            Matcher m = p.matcher("  83e-4xx");
            if (m.find()) System.err.printf("match [%s]%n", m.group());
            System.exit(0);
        }
        Lexicon<MyTokenType> lex = new Lexicon<>(MyTokenType.class);
        LL1Grammar<MyTokenType> syn = new LL1Grammar<>(MyTokenType.class);
        Consumer<Token<MyTokenType>> me = new Consumer<Token<MyTokenType>>() {
            @Override
            public void accept(Token<MyTokenType> token) {
                System.err.printf("%s %s-%s: [%s]%n", token.type.name(),
                                  token.start, token.end, token.text);
            }
        };
        me = new CommentEliminator(me);
        try (Reader in = new StringReader("erk 435spall 435 + -")) {
            lex.tokenize(in, me);
        }

        Parser<MyTokenType> par = syn.newParser(MyTokenType.EXPR);
        try (Reader in = new StringReader("4 * 3 - 12 / 3 + 2 * 3")) {
            lex.tokenize(in, new CommentEliminator(par));
        }
        System.err.printf("Fault: %s%n", par.fault());
        System.err.printf("Expected: %s%n", par.expected());
        System.err.printf("Result: %s%n", par.root());
        print("", par.root());
    }
}
