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

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives a lexicon from the annotations of an enumeration type. A
 * lexicon is a set of terminal nodes (token specifications) which can
 * be used to form a lexical analyzer.
 * {@link #tokenize(Reader, Consumer)} can be invoked to tokenize a
 * character sequence, and deliver it to a token consumer, such as a
 * {@link Parser} obtained from {@link LL1Grammar#newParser(Enum)}.
 * 
 * @author simpsons
 * 
 * @param <T> the token type
 */
public class Lexicon<T extends Enum<T>> {
    private final Map<String, T> patterns = new LinkedHashMap<>();

    private Pattern pattern;

    private List<T> tokenIndex;

    private T unmatched, epsilon;

    /**
     * Create a lexicon from an enumeration type. One constant must be
     * annotated with {@link Unmatched}, and will be used for all
     * unmatched sequenced. One must be annotated with {@link Epsilon},
     * and will be used as the final token.
     * 
     * @param type the enumeration type
     * 
     * @throws IllegalArgumentException if constraints for the
     * enumeration type are not met
     * 
     * @throws IllegalAccessException if an enumeration constant is
     * inaccessible
     */
    public Lexicon(Class<T> type) throws IllegalAccessException {
        for (Field f : type.getDeclaredFields()) {
            if (!f.isEnumConstant()) continue;
            @SuppressWarnings("unchecked")
            T c = (T) f.get(null);
            for (Literal t : f.getAnnotationsByType(Literal.class)) {
                add(t.value(), c);
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
        if (unmatched == null) throw new IllegalArgumentException("Enum " + type
            + " lacks @Unmatched");
        if (epsilon == null) throw new IllegalArgumentException("Enum " + type
            + " lacks @Epsilon");
    }

    private void add(String pat, T token) {
        patterns.put(pat, token);
        pattern = null;
        // System.err.printf("Added %s [%s]\n", token, pat);
    }

    private void ensurePattern() {
        if (pattern != null) return;

        tokenIndex = new ArrayList<>(patterns.size());
        String sep = "";
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, T> entry : patterns.entrySet()) {
            T type = entry.getValue();
            result.append(sep).append("(").append(entry.getKey()).append(")");
            sep = "|";
            tokenIndex.add(type);
        }
        pattern = Pattern.compile(result.toString(), Pattern.MULTILINE);
        // System.err.printf("midpat: %s%n", pattern);
    }

    /**
     * Tokenize a character stream.
     * 
     * @param in the input stream
     * 
     * @param handler a consumer of tokens
     * 
     * @throws IOException if an I/O error occurs
     */
    public void tokenize(Reader in, Consumer<? super Token<T>> handler)
        throws IOException {
        /* Ensure we have a complete pattern formed from the token ids'
         * patterns, and a mapping from each group to a token id. */
        ensurePattern();
        final Pattern pattern = this.pattern;
        final List<T> tokenIndex = List.copyOf(this.tokenIndex);
        final int tokenCount = tokenIndex.size();
        final TextPosition.Tracker tracker = new TextPosition.Tracker();
        final char[] chrs = new char[1024];
        boolean ended = false;

        StringBuilder buffer = new StringBuilder();
        boolean found;
        do {
            // System.err.printf("%s: [%s]%n", ended, buffer);
            Matcher m;
            while ((!(found = (m = pattern.matcher(buffer)).find()) ||
                (!ended && m.end() == buffer.length())) && !ended) {
                /* The buffer isn't long enough to determine a match.
                 * Read more from the input, and append it. */
                int got = in.read(chrs);
                if (got < 0) {
                    ended = true;
                } else {
                    buffer.append(chrs, 0, got);
                }
                // System.err.printf("*%s: [%s]%n", ended, buffer);
            }
            if (found) {
                String unmatched = buffer.substring(0, m.start());
                if (unmatched != null && !unmatched.isEmpty()) {
                    TextPosition start = tracker.get();
                    tracker.advance(unmatched);
                    TextPosition end = tracker.prior();
                    handler.accept(Token.of(this.unmatched, start, end,
                                            unmatched));
                }
                for (int i = 0; i < tokenCount; i++) {
                    T type = tokenIndex.get(i);
                    String text = m.group(i + 1);
                    if (text == null) continue;
                    TextPosition start = tracker.get();
                    tracker.advance(text);
                    TextPosition end = tracker.prior();
                    handler.accept(Token.of(type, start, end, text));
                    break;
                }
                buffer.delete(0, m.end());
            } else {
                String unmatched = buffer.toString();
                if (unmatched != null && !unmatched.isEmpty()) {
                    TextPosition start = tracker.get();
                    tracker.advance(unmatched);
                    TextPosition end = tracker.prior();
                    handler.accept(Token.of(this.unmatched, start, end,
                                            unmatched));
                    tracker.advance(unmatched);
                }
                break;
            }
        } while (true);
        handler.accept(Token.of(epsilon, tracker.get(), tracker.prior(), ""));
    }
}
