// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2021, Lancaster University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.syntax.apt;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import uk.ac.lancs.scc.jardeps.Service;
import uk.ac.lancs.syntax.Epsilon;
import uk.ac.lancs.syntax.Literal;
import uk.ac.lancs.syntax.Production;
import uk.ac.lancs.syntax.Productions;
import uk.ac.lancs.syntax.Unmatched;

/**
 *
 * @author simpsons
 */
@Service(Processor.class)
public final class SourceChecker implements Processor {
    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private ProcessingEnvironment env;

    private Elements elements;

    private Types types;

    private Messager messager;

    private TypeElement getType(Class<?> type) {
        /* Get the type's module. It might not belong to one, so get the
         * unnamed module instead. */
        Module mod = type.getModule();
        ModuleElement melem = elements
            .getModuleElement(mod.getName() == null ? "" : mod.getName());
        return elements.getTypeElement(melem, type.getCanonicalName());
    }

    private TypeElement productionElem, productionsElem, literalElem;

    private ExecutableElement productionValueElem, productionsValueElem,
        literalValueElem;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        this.env = processingEnv;
        this.elements = this.env.getElementUtils();
        this.types = this.env.getTypeUtils();
        this.messager = this.env.getMessager();
        productionElem = getType(Production.class);
        productionsElem = getType(Productions.class);
        literalElem = getType(Literal.class);

        for (ExecutableElement xelem : ElementFilter
            .methodsIn(productionElem.getEnclosedElements())) {
            if (!xelem.getSimpleName().toString().equals("value")) continue;
            productionValueElem = xelem;
            break;
        }
        for (ExecutableElement xelem : ElementFilter
            .methodsIn(productionsElem.getEnclosedElements())) {
            if (!xelem.getSimpleName().toString().equals("value")) continue;
            productionsValueElem = xelem;
            break;
        }
        for (ExecutableElement xelem : ElementFilter
            .methodsIn(literalElem.getEnclosedElements())) {
            if (!xelem.getSimpleName().toString().equals("value")) continue;
            literalValueElem = xelem;
            break;
        }
    }

    private final Set<Class<? extends Annotation>> myAnnots =
        Set.of(Production.class, Productions.class, Literal.class,
               Epsilon.class, Unmatched.class);

    private final Map<TypeElement,
                      Map<VariableElement,
                          Collection<AnnotationMirror>>> productions =
                              new HashMap<>();

    private void recordProduction(VariableElement elem,
                                  AnnotationMirror production) {
        TypeElement type = (TypeElement) elem.getEnclosingElement();
        productions.computeIfAbsent(type, k -> new HashMap<>())
            .computeIfAbsent(elem, k -> new ArrayList<>()).add(production);
    }

    @Override
    public boolean process(Set<? extends TypeElement> set,
                           RoundEnvironment re) {
        productions.clear();
        for (Element elem : re.getElementsAnnotatedWithAny(myAnnots)) {
            if (elem.getKind() != ElementKind.ENUM_CONSTANT) {
                messager
                    .printMessage(Diagnostic.Kind.ERROR,
                                  "Production, Productions, Literal,"
                                      + " Epsilon and Unmatched only apply to"
                                      + " enumeration constants",
                                  elem);
                continue;
            }
            VariableElement velem = (VariableElement) elem;
            for (AnnotationMirror amir : velem.getAnnotationMirrors()) {
                DeclaredType atype = amir.getAnnotationType();
                if (types.isSameType(atype, literalElem.asType())) {
                    AnnotationValue av =
                        amir.getElementValues().get(literalValueElem);
                    String pattern = av.getValue().toString();
                    try {
                        Pattern.compile(pattern);
                    } catch (PatternSyntaxException ex) {
                        messager.printMessage(Diagnostic.Kind.ERROR,
                                              ex.getDescription(), velem, amir,
                                              av);
                    }
                } else if (types.isSameType(atype, productionElem.asType())) {
                    recordProduction(velem, amir);
                } else if (types.isSameType(atype, productionsElem.asType())) {
                    AnnotationValue val =
                        amir.getElementValues().get(productionsValueElem);
                    val.accept(makeArrayVisitor((list, p) -> {
                        for (AnnotationValue item : list)
                            item.accept(makeVisitor((am, pp) -> {
                                recordProduction(velem, am);
                                return null;
                            }), p);
                        return null;
                    }), null);
                }
            }
        }

        for (var item : productions.entrySet()) {
            TypeElement type = item.getKey();
            var index = ElementFilter.fieldsIn(type.getEnclosedElements())
                .stream().filter(e -> e.getKind() == ElementKind.ENUM_CONSTANT)
                .collect(Collectors.toMap(e -> e.getSimpleName().toString(),
                                          e -> e));
            for (var citem : item.getValue().entrySet()) {
                VariableElement constant = citem.getKey();
                for (AnnotationMirror amir : citem.getValue()) {
                    assert constant.getAnnotationMirrors().contains(amir);
                    AnnotationValue av =
                        amir.getElementValues().get(productionValueElem);
                    av.accept(makeArrayVisitor((list, p) -> {
                        for (AnnotationValue entry : list) {
                            String name = entry.getValue().toString();
                            if (!index.containsKey(name))
                                messager.printMessage(Diagnostic.Kind.ERROR,
                                                      "unknown node " + name,
                                                      constant, amir, av);
                        }
                        return null;
                    }), null);
                }
            }
        }
        return false;
    }

    private static <R, P> AnnotationValueVisitor<R, P>
        makeVisitor(BiFunction<? super AnnotationMirror, P, R> func) {
        return new SimpleAnnotationValueVisitor7<R, P>() {
            @Override
            public R visitAnnotation(AnnotationMirror a, P p) {
                return func.apply(a, p);
            }
        };
    }

    private static <R, P> AnnotationValueVisitor<R, P>
        makeArrayVisitor(BiFunction<? super List<? extends AnnotationValue>, P,
                                    R> func) {
        return new SimpleAnnotationValueVisitor7<R, P>() {
            @Override
            public R visitArray(List<? extends AnnotationValue> a, P p) {
                return func.apply(a, p);
            }
        };
    }

    @Override
    public Iterable<? extends Completion>
        getCompletions(Element elmnt, AnnotationMirror am, ExecutableElement ee,
                       String string) {
        return Collections.emptySet();
    }
}
