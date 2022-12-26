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

import java.util.List;

/**
 * A node in a syntax tree
 * 
 * <p>
 * Nodes are either terminals (tokens) or non-terminals.
 * 
 * @author simpsons
 * 
 * @param <T> the token/non-terminal type
 */
public abstract class Node<T extends Enum<T>> {
    /**
     * The node type
     */
    public final T type;

    /**
     * The position of the start of the token in the input stream
     */
    public final TextPosition start;

    /**
     * The position of the end of the token in the input stream
     */
    public final TextPosition end;

    /**
     * Create a node.
     * 
     * @param type the node type
     * 
     * @param start the start position
     * 
     * @param end the end position
     */
    protected Node(T type, TextPosition start, TextPosition end) {
        this.type = type;
        this.start = start;
        this.end = end;
    }

    /**
     * Get the text of this node. A terminal's text is simply that which
     * it matched. A non-terminal's text is a concatenation of its
     * children's text.
     * 
     * @return the text of the node
     */
    public abstract String text();

    /**
     * Get a subnode, if this is a non-terminal.
     * 
     * @param n the subnode number, starting from 0; or negative to
     * count from the end (-1 being the last subnode)
     * 
     * @return the subnode; or {@code null} if it's a terminal, or the
     * number is out of range
     */
    public abstract Node<T> child(int n);

    /**
     * Get an immutable subsequence of subnodes.
     * 
     * @param fromIndex the index of the first child to include
     * 
     * @param toIndex the index just past the last child to include; or
     * negative to count from the end (-1 being the last subnode)
     * 
     * @return a list of the selected subnodes; or an empty list if a
     * terminal
     */
    public abstract List<Node<T>> children(int fromIndex, int toIndex);

    /**
     * Get an immutable tail of subnodes.
     * 
     * @param fromIndex the number of initial nodes to skip
     * 
     * @return a list of the selected subnodes; or an empty list if a
     * terminal
     */
    public final List<Node<T>> children(int fromIndex) {
        return children(fromIndex, size());
    }

    /**
     * Get an immutable list of subnodes.
     * 
     * @return a list of the subnodes; or an empty list if a terminal
     */
    public final List<Node<T>> children() {
        return children(0);
    }

    /**
     * Get the number of subnodes.
     * 
     * @return the number of subnodes, if this is a non-terminal; or
     * zero otherwise
     */
    public abstract int size();

    /**
     * Check whether this is an empty non-terminal.
     * 
     * @return {@code true} if the node is a non-terminal with zero
     * parts; {@code false} otherwise
     */
    public final boolean isEmpty() {
        return size() == 0;
    }
}
