package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import org.junit.Test;

public class ClassStructureTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void actions_implementing_DumbAware_should_extend_DumbAwareAction() {
    classes()
        .that().areAssignableTo(com.intellij.openapi.actionSystem.AnAction.class)
        .and().implement(com.intellij.openapi.project.DumbAware.class)
        .should().beAssignableTo(com.intellij.openapi.project.DumbAwareAction.class)
        .because("`extends DumbAwareAction` should be used instead of " +
            "extending `AnAction` and implementing `DumbAware` separately")
        .check(importedClasses);
  }

  @Test
  public void actions_overriding_onUpdate_should_call_super_onUpdate() {
    classes()
        .that()
        .areAssignableTo(com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction.class)
        .and()
        .areNotAssignableFrom(com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction.class)
        .and(new DescribedPredicate<JavaClass>("override onUpdate method") {
          @Override
          public boolean apply(JavaClass input) {
            return input.getMethods().stream().anyMatch(method -> method.getName().equals("onUpdate"));
          }
        })
        .should()
        .callMethodWhere(
            new DescribedPredicate<JavaMethodCall>("name is onUpdate and owner is the direct superclass") {
              @Override
              public boolean apply(JavaMethodCall input) {
                JavaCodeUnit origin = input.getOrigin(); // where is the method called from?
                AccessTarget.MethodCallTarget target = input.getTarget(); // where is the method declared?

                if (origin.getName().equals("onUpdate") && target.getName().equals("onUpdate")) {
                  return target.getOwner().equals(origin.getOwner().getSuperclass().orElse(null));
                }
                return false;
              }
            })
        .check(importedClasses);

  }

  @Test
  public void inner_classes_should_not_be_used() {
    noClasses()
        .that().haveNameNotMatching("^.*\\$[0-9]+$") // inner classes autogenerated by the compiler
        .and()
        .haveNameNotMatching("^" + com.virtuslab.gitmachete.frontend.actions.dialogs.GitPushDialog.class.getName() + "\\$.*$")
        .and()
        .haveNameNotMatching("^" + com.virtuslab.gitmachete.frontend.actions.dialogs.OverrideForkPointDialog.class.getName()
            + "\\$WhenMappings$")
        .should().beInnerClasses()
        .because("inner (non-static nested) classes are currently unsafe & discouraged " +
            "due to https://github.com/typetools/checker-framework/issues/3407; " +
            "consider using a static nested class " +
            "and passing a reference to the enclosing object (or to the fields thereof) explicitly")
        .check(importedClasses);
  }

}
