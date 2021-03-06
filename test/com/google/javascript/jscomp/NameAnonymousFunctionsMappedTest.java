/*
 * Copyright 2008 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;

/**
 * Test cases for {@link NameAnonymousFunctionsMapped}.
 *
 */
public final class NameAnonymousFunctionsMappedTest extends CompilerTestCase {

  private static final String EXTERNS = "var document;";

  private NameAnonymousFunctionsMapped pass;
  private VariableMap previous;

  public NameAnonymousFunctionsMappedTest() {
    super(EXTERNS);
  }

  @Override
  protected int getNumRepetitions() {
    return 1;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    previous = null;
  }

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return pass = new NameAnonymousFunctionsMapped(compiler, previous);
  }

  private void assertMapping(String... pairs) {
    VariableMap functionMap = pass.getFunctionMap();
    assertEquals(0, pairs.length % 2);
    for (int i = 0; i < pairs.length; i += 2) {
      String s = functionMap.lookupSourceName(pairs[i]);
      assertEquals(pairs[i + 1], s);
    }
    assertThat(functionMap.getNewNameToOriginalNameMap()).hasSize(pairs.length / 2);
  }

  public void testSimpleVarAssignment1() {
    test("var a = function() { return 1; }",
         "var a = function $() { return 1; }");
    assertMapping("$", "a");
  }

  public void testSimpleVarAssignment2() {
    previous = VariableMap.fromMap(ImmutableMap.of(
        "a", "previous"));

    test("var a = function() { return 1; }",
         "var a = function previous() { return 1; }");

    assertMapping("previous", "a");
  }

  public void testSimpleVarAssignment3() {
    previous = VariableMap.fromMap(ImmutableMap.of(
        "unused", "$"));

    test("var fn = function() { return 1; }",
         "var fn = function $a() { return 1; }");

    assertMapping("$a", "fn");
  }

  public void testSimpleLetAssignment() {
    test("let a = function() { return 1; }", "let a = function $() { return 1; }");
    assertMapping("$", "a");
  }

  public void testSimpleConstAssignment() {
    test("const a = function() { return 1; }", "const a = function $() { return 1; }");
    assertMapping("$", "a");
  }

  public void testAssignmentToProperty() {
    test("var a = {}; a.b = function() { return 1; }",
         "var a = {}; a.b = function $() { return 1; }");
    assertMapping("$", "a.b");
  }

  public void testAssignmentToPrototype() {
    test("function a() {} a.prototype.b = function() { return 1; };",
         "function a() {} " +
         "a.prototype.b = function $() { return 1; };");
    assertMapping("$", "a.prototype.b");
  }

  public void testAssignmentToPrototype2() {
    test("var a = {}; " +
         "a.b = function() {}; " +
         "a.b.prototype.c = function() { return 1; };",
         "var a = {}; " +
         "a.b = function $() {}; " +
         "a.b.prototype.c = function $a() { return 1; };");
    assertMapping("$", "a.b", "$a", "a.b.prototype.c");
  }

  public void testAssignmentToPrototype3() {
    test("function a() {} a.prototype['XXX'] = function() { return 1; };",
         "function a() {} " +
         "a.prototype['XXX'] = function $() { return 1; };");
    assertMapping("$", "a.prototype['XXX']");
    test("function a() {} a.prototype['\\n'] = function() { return 1; };",
         "function a() {} " +
         "a.prototype['\\n'] = function $() { return 1; };");
    assertMapping("$", "a.prototype['\\n']");
  }

  public void testAssignmentToPrototype4() {
    test("var Y = 1; function a() {} " +
         "a.prototype[Y] = function() { return 1; };",
         "var Y = 1; function a() {} " +
         "a.prototype[Y] = function $() { return 1; };");
    assertMapping("$", "a.prototype[Y]");
  }

  public void testAssignmentToPrototype5() {
    test("function a() {} a['prototype'].b = function() { return 1; };",
         "function a() {} " +
         "a['prototype'].b = function $() { return 1; };");
    assertMapping("$", "a['prototype'].b");
  }


  public void testPrototypeInitializer() {
    test("function a(){} a.prototype = {b: function() { return 1; }};",
         "function a(){} " +
         "a.prototype = {b: function $() { return 1; }};");
    assertMapping("$", "a.prototype.b");
  }

  public void testAssignmentToPropertyOfCallReturnValue() {
    test("document.getElementById('x').onClick = function() {};",
         "document.getElementById('x').onClick = " +
         "function $() {};");
    assertMapping("$", "document.getElementById('x').onClick");
  }

  public void testAssignmentToPropertyOfArrayElement() {
    test("var a = {}; a.b = [{}]; a.b[0].c = function() {};",
         "var a = {}; a.b = [{}]; a.b[0].c = function $() {};");
    assertMapping("$", "a.b[0].c");
    test("var a = {b: {'c': {}}}; a.b['c'].d = function() {};",
         "var a = {b: {'c': {}}}; a.b['c'].d = function $() {};");
    assertMapping("$", "a.b['c'].d");
    test("var a = {b: {'c': {}}}; a.b[x()].d = function() {};",
         "var a = {b: {'c': {}}}; a.b[x()].d = function $() {};");
    assertMapping("$", "a.b[x()].d");
  }

  public void testAssignmentToObjectLiteralOnDeclaration() {
    testSame("var a = { b: function() {} }");
    testSame("var a = { b: { c: function() {} } }");
  }

  public void testAssignmentToGetElem() {
    test("function f() { win['x' + this.id] = function(a){}; }",
         "function f() { win['x' + this.id] = function $(a){}; }");

    // TODO - could probably do a better job encoding these
    assertMapping("$", "win['x'+this.id]");
  }

  public void testGetElemWithDashes() {
    test("var foo = {}; foo['-'] = function() {};",
         "var foo = {}; foo['-'] = function $() {};");
    assertMapping("$", "foo['-']");
  }

  public void testDuplicateNames() {
    test("var a = function() { return 1; };a = function() { return 2; }",
         "var a = function $() { return 1; };a = function $() { return 2; }");
    assertMapping("$", "a");
  }

  public void testIgnoreArrowFunctions() {
    testSame("var a = () => 1");
    testSame("var a = {b: () => 1};");
    testSame("function A() {} A.prototype.foo = () => 5");
  }

  public void testComputedProperty() {
    test(
        "function A() {} A.prototype = {['foo']: function() {} };",
        "function A() {} A.prototype = {['foo']: function $() {} };");
    assertMapping("$", "A.prototype.foo");

    test(
        "function A() {} A.prototype = {['foo' + bar()]: function() {} };",
        "function A() {} A.prototype = {['foo' + bar()]: function $() {} };");
    assertMapping("$", "A.prototype.'foo'+bar()");
  }

  public void testGetter() {
    testSame("function A() {} A.prototype = { get foo() { return 5; } }");
  }

  public void testSetter() {
    testSame("function A() {} A.prototype = { set foo(bar) {} }");
  }

  public void testMethodDefinitionShorthand() {
    testSame("var obj = { b() {}, c() {} }");
    testSame("var obj; obj = { b() {}, c() {} }");
  }

  public void testClasses() {
    testSame("class A { static foo() {} }");
    testSame("class A { constructor() {} foo() {} }");
  }

  public void testExportedFunctions() {
    // Don't provide a name in the first case, since it would declare the function in the module
    // scope and potentially be unsafe.
    testSame("export default function() {}");
    // In this case, adding a name would be okay since this is a function expression.
    testSame("export default (function() {})");
    testSame("export default function foo() {}");
  }

  public void testDefaultParameters() {
    test("function f(g = function() {}) {}", "function f(g = function $() {}) {}");
    assertMapping("$", "g");
  }

  public void testSimpleGeneratorAssignment() {
    test("var a = function *() { yield 1; }",
        "var a = function *$() { yield 1; }");
    assertMapping("$", "a");
  }

  public void testDestructuring() {
    test("var {a = function() {}} = {};", "var {a = function $() {}} = {};");
    assertMapping("$", "a");
  }
}
