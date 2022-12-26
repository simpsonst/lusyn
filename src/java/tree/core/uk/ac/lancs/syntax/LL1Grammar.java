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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Derives an LL(1) grammar from the annotations of an enumeration type.
 * Constants of the type that have productions should be annotated with
 * {@link Production}, which can be used multiple times. Each annotation
 * lists the names of enumeration constants that form the sequence of
 * expected tokens and non-terminals in the production.
 * 
 * @author simpsons
 * 
 * @param <T> the token/non-terminal type
 */
public final class LL1Grammar<T extends Enum<T>> {
    private T unmatched, epsilon;

    /**
     * Create a grammar from an enumeration type.
     * 
     * @param type the enumeration type
     * 
     * @throws IllegalArgumentException if the grammar fails to meet
     * requirements
     * 
     * @throws IllegalAccessException if an enumeration constant is not
     * accessible
     * 
     * @throws NoSuchFieldException if a production references a
     * non-existent constant
     */
    public LL1Grammar(Class<T> type)
        throws IllegalAccessException,
            NoSuchFieldException {
        final Map<T, List<List<T>>> prods = new EnumMap<>(type);
        for (Field f : type.getDeclaredFields()) {
            if (!f.isEnumConstant()) continue;
            @SuppressWarnings("unchecked")
            T c = (T) f.get(null);
            for (Production t : f.getAnnotationsByType(Production.class)) {
                List<T> exp = new ArrayList<>();
                for (String txt : t.value()) {
                    Field rf = type.getDeclaredField(txt);
                    if (!rf.isEnumConstant())
                        throw new IllegalArgumentException("Field " + txt
                            + " in " + type.getTypeName()
                            + " not enum constant");
                    @SuppressWarnings("unchecked")
                    T rc = (T) rf.get(null);
                    exp.add(rc);
                }
                prods.computeIfAbsent(c, k -> new ArrayList<>()).add(exp);
            }
            for (@SuppressWarnings("unused")
            Unmatched t : f.getAnnotationsByType(Unmatched.class)) {
                if (unmatched != null)
                    throw new IllegalArgumentException("Two @Unmatched types in "
                        + type + ": " + unmatched + " and " + c);
                unmatched = c;
            }
            for (@SuppressWarnings("unused")
            Epsilon t : f.getAnnotationsByType(Epsilon.class)) {
                if (epsilon != null)
                    throw new IllegalArgumentException("Two @Epsilon types in "
                        + type + ": " + epsilon + " and " + c);
                epsilon = c;
            }
        }
        if (epsilon == null) throw new IllegalArgumentException("Enum " + type
            + " lacks @Epsilon");

        Collection<T> emptyNonTerminals =
            Collections.newSetFromMap(new EnumMap<>(type));
        Map<T, Map<T, List<T>>> terminalIndex = new EnumMap<>(type);
        Map<T, List<T>> alternatives = new EnumMap<>(type);
        for (Map.Entry<T, List<List<T>>> entry : prods.entrySet()) {
            T lhs = entry.getKey();
            for (List<T> rhs : entry.getValue()) {
                if (rhs.isEmpty()) {
                    emptyNonTerminals.add(lhs);
                } else {
                    T first = rhs.get(0);
                    if (prods.containsKey(first)) {
                        List<T> already = alternatives.get(lhs);
                        if (already != null)
                            throw new IllegalArgumentException("dual "
                                + "non-terminal-lead productions for " + lhs
                                + ": " + already + " and " + rhs);
                        alternatives.put(lhs, rhs);
                    } else {
                        Map<T, List<T>> foo = terminalIndex
                            .computeIfAbsent(lhs, k -> new EnumMap<>(type));
                        List<T> already = foo.putIfAbsent(first, rhs);
                        if (already != null)
                            throw new IllegalArgumentException("duplicate "
                                + "terminal-lead productions for " + lhs + ": "
                                + already + " and " + rhs);
                    }
                }
            }
        }

        this.emptyNonTerminals = emptyNonTerminals;
        this.alternatives = alternatives;
        this.terminalIndex = terminalIndex;
    }

    /**
     * Identifies non-terminals that include an empty production.
     */
    private final Collection<T> emptyNonTerminals;

    private final Map<T, Map<T, List<T>>> terminalIndex;

    private final Map<T, List<T>> alternatives;

    private boolean isNonTerminal(T type) {
        return emptyNonTerminals.contains(type) ||
            terminalIndex.containsKey(type) || alternatives.containsKey(type);
    }

    private class NodeBuilder {
        final T type;

        final int expect;

        @Override
        public String toString() {
            return "[" + type + ", " + parts.size() + "/" + expect + " " + parts
                + "]";
        }

        NodeBuilder(T type, int expect) {
            this.type = type;
            this.expect = expect;
        }

        List<Node<T>> parts = new ArrayList<>();

        Node<T> complete(TextPosition defPos) {
            int len = parts.size();
            if (len > 0) {
                return NonTerminal.of(type, parts.get(0).start,
                                      parts.get(len - 1).end, parts);
            } else {
                return NonTerminal.of(type, defPos, defPos, parts);
            }
        }

        /**
         * Add a child node to this node.
         * 
         * @param part the new subnode
         * 
         * @return {@code true} if this node is now complete
         */
        boolean submit(Node<T> part) {
            parts.add(part);
            return parts.size() >= expect;
        }
    }

    /**
     * Create a parser for the grammar.
     * 
     * @param root the root node of the grammar
     * 
     * @return a new parser capable of a single parsing action
     */
    public Parser<T> newParser(T root) {
        return new Parser<T>() {
            private final List<T> expected =
                new ArrayList<>(Collections.singleton(root));

            private final List<NodeBuilder> nodes = new ArrayList<>();

            private Token<T> fault = null;

            {
                // NodeBuilder nnb = new NodeBuilder(root, -1);
                // nodes.add(0, nnb);
            }

            private void trace() {
                logger.fine(() -> String.format("Expected: %s", expected));
                logger.fine(() -> String.format("Building: %s", nodes));
            }

            {
                trace();
            }

            @Override
            public void accept(Token<T> t) {
                logger.fine(() -> String.format("Accepting %s", t));

                /* Ignore the rest of the input if we've already found a
                 * syntax error. */
                if (fault != null) return;

                /* Repeatedly compare the top of the expected node stack
                 * to the current token, until the token is accepted. */
                do {
                    logger.fine(() -> String.format("Expecting %s", expected));

                    /* If we've parsed everything okay, no more input is
                     * expected. */
                    if (expected.isEmpty()) {
                        if (t.type == epsilon) return;
                        fault = t;
                        return;
                    }

                    T top = expected.get(0);
                    if (top == t.type) {
                        /* The token matches the expected type, which is
                         * a terminal. */
                        logger.fine("Token match");

                        /* Consume it from the stack. */
                        expected.remove(0);

                        /* Submit it to the node builder at the top of
                         * the node stack. */
                        complete(t, t.end);

                        trace();

                        /* The token was accepted. */
                        break;
                    }

                    if (!isNonTerminal(top)) {
                        /* A terminal is expected, but they don't match.
                         * This is a syntax error. */
                        logger.fine("Token mismatch");
                        fault = t;
                        break;
                    }

                    /* A non-terminal is expected, but can't match a
                     * terminal as is. */

                    /* Maybe there's a production beginning with the
                     * right terminal. */
                    List<T> replacement = terminalIndex
                        .getOrDefault(top, Collections.emptyMap()).get(t.type);

                    if (replacement == null) {
                        /* There might be a production beginning with
                         * another non-terminal. */
                        replacement = alternatives.get(top);
                        List<T> r = replacement;
                        logger.fine(() -> String
                            .format("Non-term replacement for %s: %s", top, r));
                    }

                    if (replacement != null) {
                        /* Replace the head of the expected nodes with
                         * the replacement. */
                        List<T> r = replacement;
                        logger.fine(() -> String.format("%s -> %s", top, r));
                        List<T> head = expected.subList(0, 1);
                        head.clear();
                        head.addAll(replacement);

                        /* Prepare for a new node to accept the
                         * replacements. */
                        NodeBuilder nnb =
                            new NodeBuilder(top, replacement.size());
                        nodes.add(0, nnb);

                        trace();

                        /* Try to consume the latest token again. */
                        continue;
                    }

                    /* Nothing matched. Does the non-terminal have an
                     * empty production? */
                    if (!emptyNonTerminals.contains(top)) {
                        /* The non-terminal does not have an empty
                         * production. */
                        logger.fine(() -> String.format("No expansion for %s",
                                                        top));
                        logger.fine(() -> String.format("Alternatives: %s",
                                                        alternatives));
                        fault = t;
                        break;
                    }

                    /* Submit an empty node of the expected type. */
                    logger.fine("Applying empty production");
                    complete(NonTerminal.of(top, t.start, t.end,
                                            Collections.emptyList()),
                             t.end);

                    /* We don't need to expect the top non-terminal any
                     * more. */
                    expected.remove(0);

                    /* Complete the top node. */
                    // Node<T> nn = nodes.remove(0).complete(t.end);
                    // complete(nn, t.end);

                    trace();
                } while (true);
            }

            void complete(Node<T> nn, TextPosition defPos) {
                while (nn != null && !nodes.isEmpty()) {
                    trace();
                    NodeBuilder nb = nodes.get(0);
                    if (nb.submit(nn)) {
                        nn = nb.complete(defPos);
                        nodes.remove(0);
                    } else
                        nn = null;
                }
                if (nn != null) result = nn;
            }

            private Node<T> result;

            @Override
            public Node<T> root() {
                return result;
            }

            @Override
            public Token<T> fault() {
                return fault;
            }

            @Override
            public List<T> expected() {
                return List.copyOf(expected);
            }
        };
    }

    private static final Logger logger = Logger.getLogger("uk.ac.lancs.syntax");
}
