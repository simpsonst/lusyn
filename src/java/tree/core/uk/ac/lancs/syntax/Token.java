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

import java.util.Collections;
import java.util.List;

/**
 * A terminal node or token
 * 
 * <p>
 * A token's value is the text that matched its pattern.
 * 
 * @param <T> the node type representing different tokens
 * 
 * @author simpsons
 */
public final class Token<T extends Enum<T>> extends Node<T> {
    /**
     * The text of the token
     */
    public final String text;

    private Token(T type, TextPosition start, TextPosition end, String text) {
        super(type, start, end);
        this.text = text;
    }

    /**
     * Create a token.
     * 
     * @param type the node type
     * 
     * @param start the start position of the token in the input stream
     * 
     * @param end the end position of the token in the input stream
     * 
     * @param text the token text
     * 
     * @return the new token
     */
    public static <T extends Enum<T>> Token<T>
        of(T type, TextPosition start, TextPosition end, String text) {
        return new Token<>(type, start, end, text);
    }

    /**
     * Get a string representation of this token.
     * 
     * @return an informal representation including the type, name,
     * start and end positions, and the literal text
     */
    @Override
    public String toString() {
        return type.name() + "[" + start + "-" + end + ":" + text + "]";
    }

    @Override
    public String text() {
        return text;
    }

    /**
     * {@inheritDoc}
     * 
     * @return This method always returns {@code null}.
     */
    @Override
    public Node<T> child(int n) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @default This method always returns an empty list.
     */
    @Override
    public List<Node<T>> children(int fromIndex, int toIndex) {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This method always returns zero.
     */
    @Override
    public int size() {
        return 0;
    }
}
