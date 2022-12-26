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

/**
 * Identifies the position in an input source of text.
 * 
 * @author simpsons
 */
public final class TextPosition {
    /**
     * The line number, starting at 1
     */
    public final int line;

    /**
     * The column number, starting at 1
     */
    public final int column;

    private TextPosition(int line, int column) {
        this.line = line;
        this.column = column;
    }

    /**
     * Create a text position.
     * 
     * @param line the line number, starting at 1
     * 
     * @param column the column number, starting at 1
     * 
     * @return the new text position
     */
    public static TextPosition of(int line, int column) {
        return new TextPosition(line, column);
    }

    /**
     * Get a hashcode of this object.
     * 
     * @return the object's hashcode
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + column;
        result = prime * result + line;
        return result;
    }

    /**
     * Determine whether this object is equal to another object.
     * 
     * @param obj the object to compare with
     * 
     * @return {@code true} if the other object is a
     * {@link TextPosition}, and has the same line and column numbers;
     * {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        TextPosition other = (TextPosition) obj;
        if (column != other.column) return false;
        if (line != other.line) return false;
        return true;
    }

    /**
     * Create a string representation of this text position.
     * 
     * @return an informal representation consisting of the line and
     * column numbers appended with a forward slash
     */
    @Override
    public String toString() {
        return line + "/" + column;
    }

    /**
     * Tracks line positions of a character sequence.
     * 
     * @author simpsons
     */
    public static class Tracker {
        private int line = 1, column = 1;

        private int lastLine = 1, lastColumn = 0;

        /**
         * Get the current position.
         * 
         * @return the current position as line and column
         */
        public TextPosition get() {
            return of(line, column);
        }

        /**
         * Get the position just before the last call to
         * {@link #advance(CharSequence)}.
         * 
         * @return the prior position
         * 
         * @throws IllegalStateException if
         * {@link #advance(CharSequence)} has not yet been called
         */
        public TextPosition prior() {
            if (lastColumn == 0)
                throw new IllegalStateException("no prior character");
            return of(lastLine, lastColumn);
        }

        /**
         * Advance the text position given a sequence of characters.
         * Newline <samp>\n</samp> and carriage return <samp>\r</samp>
         * reset the column position, and the latter also increments the
         * line number. Other characters are taken as incrementing the
         * column number by one.
         * 
         * @param txt the text to advance by
         */
        public void advance(CharSequence txt) {
            final int len = txt.length();
            for (int i = 0; i < len; i++) {
                char c = txt.charAt(i);
                if (c == '\r') {
                    lastColumn = column;
                    column = 1;
                } else if (c == '\n') {
                    lastColumn = column;
                    column = 1;
                    lastLine = line;
                    line++;
                } else {
                    lastColumn = column;
                    column++;
                }
            }
        }
    }
}
