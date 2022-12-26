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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A non-terminal node in a syntax tree
 * 
 * <p>
 * A non-terminal is characterized by a sequence of nodes that compose
 * it.
 * 
 * @author simpsons
 */
public final class NonTerminal<T extends Enum<T>> extends Node<T> {
    /**
     * An immutable list of the components of the non-terminal node
     */
    public final List<Node<T>> parts;

    private NonTerminal(T type, TextPosition start, TextPosition end,
                        List<? extends Node<T>> parts) {
        super(type, start, end);
        this.parts = List.copyOf(parts);
    }

    /**
     * Create a non-terminal.
     * 
     * @param <T> the token/non-terminal type
     * 
     * @param type the node type
     * 
     * @param start the start position of the non-terminal in the input
     * stream
     * 
     * @param end the end position of the non-terminal in the input
     * stream
     * 
     * @param parts the components of the node, which will be copied
     * 
     * @return the new non-terminal node
     */
    public static <T extends Enum<T>> NonTerminal<T>
        of(T type, TextPosition start, TextPosition end,
           List<? extends Node<T>> parts) {
        return new NonTerminal<>(type, start, end, parts);
    }

    /**
     * Create a non-terminal, prefixing a list of components with one
     * other.
     * 
     * @param <T> the token/non-terminal type
     * 
     * @param type the node type
     * 
     * @param start the start position of the non-terminal in the input
     * stream
     * 
     * @param end the end position of the non-terminal in the input
     * stream
     * 
     * @param part0 the first component
     * 
     * @param parts the other components of the node, which will be
     * copied
     * 
     * @return the new non-terminal node
     */
    public static <T extends Enum<T>> NonTerminal<T>
        of(T type, TextPosition start, TextPosition end, Node<T> part0,
           List<? extends Node<T>> parts) {
        return new NonTerminal<>(type, start, end,
                                 Stream.concat(Stream.of(part0), parts.stream())
                                     .collect(Collectors.toList()));
    }

    /**
     * Create a non-terminal, inferring text positions from the
     * elements.
     * 
     * @param <T> the token/non-terminal type
     * 
     * @param type the node type
     * 
     * @param parts the components of the node, which will be copied
     * 
     * @return the new non-terminal node
     * 
     * @throws IndexOutOfBoundsException if the component list is empty
     */
    public static <T extends Enum<T>> NonTerminal<T>
        of(T type, List<? extends Node<T>> parts) {
        return new NonTerminal<>(type, parts.get(0).start,
                                 parts.get(parts.size() - 1).end, parts);
    }

    /**
     * Create a non-terminal, prefixing a list of components with one
     * other, and inferring text positions from the elements.
     * 
     * @param <T> the token/non-terminal type
     * 
     * @param type the node type
     * 
     * @param part0 the first component
     * 
     * @param parts the other components of the node, which will be
     * copied
     * 
     * @return the new non-terminal node
     */
    public static <T extends Enum<T>> NonTerminal<T>
        of(T type, Node<T> part0, List<? extends Node<T>> parts) {
        return of(type, Stream.concat(Stream.of(part0), parts.stream())
            .collect(Collectors.toList()));
    }

    /**
     * Create a non-terminal from an array.
     * 
     * @param <T> the token/non-terminal type
     * 
     * @param type the node type
     * 
     * @param start the start position of the non-terminal in the input
     * stream
     * 
     * @param end the end position of the non-terminal in the input
     * stream
     * 
     * @param parts the components of the node, which will be copied
     * 
     * @return the new non-terminal node
     */
    @SafeVarargs
    public static <T extends Enum<T>> NonTerminal<T>
        of(T type, TextPosition start, TextPosition end, Node<T>... parts) {
        return of(type, start, end, Arrays.asList(parts));
    }

    /**
     * Create a non-terminal from an array, prefixing a list of
     * components with one other, and inferring text positions from the
     * elements.
     * 
     * @param <T> the token/non-terminal type
     * 
     * @param type the node type
     * 
     * @param part0 the first part
     * 
     * @param parts the other components of the node, which will be
     * copied
     * 
     * @return the new non-terminal node
     */
    @SafeVarargs
    public static <T extends Enum<T>> NonTerminal<T> of(T type, Node<T> part0,
                                                        Node<T>... parts) {
        return of(type, part0, Arrays.asList(parts));
    }

    /**
     * Get a string representation of this non-terminal node.
     * 
     * @return an informal representation including node type and
     * representations of the constituent nodes
     */
    @Override
    public String toString() {
        return type.name() + parts.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This method returns the concatenation of its parts'
     * text.
     */
    @Override
    public String text() {
        return parts.stream().map(Node::text).collect(Collectors.joining());
    }

    @Override
    public Node<T> child(int n) {
        if (n >= parts.size()) return null;
        if (n < -parts.size()) return null;
        if (n < 0) return parts.get(parts.size() + n);
        return parts.get(n);
    }

    @Override
    public List<Node<T>> children(int fromIndex, int toIndex) {
        if (fromIndex < 0) fromIndex = 0;
        if (toIndex < -parts.size()) toIndex = 0;
        if (toIndex < 0) toIndex += parts.size();
        return parts.subList(fromIndex, toIndex);
    }

    @Override
    public int size() {
        return parts.size();
    }
}
