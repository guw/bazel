// Copyright 2025 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.analysis.starlark;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.analysis.DependencyKind;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.Aspect;
import com.google.devtools.build.lib.packages.Attribute;
import com.google.devtools.build.lib.packages.ConfiguredAttributeMapper;
import com.google.devtools.build.lib.packages.Rule;
import com.google.devtools.build.lib.packages.RuleClass;
import com.google.devtools.build.lib.packages.StructImpl;
import com.google.devtools.build.lib.packages.StructProvider;
import com.google.devtools.build.lib.starlarkbuildapi.StarlarkAspectPropagationContextApi;
import com.google.devtools.build.lib.starlarkbuildapi.StarlarkAspectPropagationContextApi.QualifiedRuleKindApi;
import com.google.devtools.build.lib.starlarkbuildapi.StarlarkAspectPropagationContextApi.RuleAttributeApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.StructApi;
import com.google.devtools.build.lib.util.OrderedSetMultimap;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkValue;

/** Implementation of {@link StarlarkAspectPropagationContextApi}. */
public final record StarlarkAspectPropagationContext(
    StructImpl aspectPublicParams, StarlarkAspectPropagationRule rule)
    implements StarlarkAspectPropagationContextApi {

  public static StarlarkAspectPropagationContext createForPropagationPredicate(
      Aspect aspect, Label label, RuleClass ruleClass, ImmutableList<String> tags) {

    var aspectPublicParams =
        StructProvider.STRUCT.create(
            aspect.getParameters().getAttributes().keySet().stream()
                .map(aspect.getDefinition().getAttributes()::get)
                .collect(
                    toImmutableMap(
                        attr -> attr.getPublicName(),
                        attr -> Attribute.valueToStarlark(attr.getDefaultValue(null)))),
            " '%s' is not a public parameter of the aspect.");

    var qualifiedRuleKind =
        new QualifiedRuleKind(ruleClass.getRuleDefinitionEnvironmentLabel(), ruleClass.getName());

    var ruleAttributes =
        StructProvider.STRUCT.create(
            ImmutableMap.of(
                "tags", new RuleAttribute(StarlarkList.immutableCopyOf(tags), /* isTool= */ false)),
            "Only rule's \"tags\" are available in propagation_predicate function.");

    return new StarlarkAspectPropagationContext(
        aspectPublicParams,
        new StarlarkAspectPropagationRule(label, qualifiedRuleKind, ruleAttributes));
  }

  /**
   * Creates a {@link StarlarkAspectPropagationContext} for {@code attr_aspects} and {@code
   * toolchains_aspects}.
   */
  public static StarlarkAspectPropagationContext createForPropagationEdges(
      Aspect aspect,
      Rule rule,
      ConfiguredAttributeMapper attributeMap,
      OrderedSetMultimap<DependencyKind, Label> dependencyLabels) {
    // TODO(b/394400334): Implement this.
    return null;
  }

  @Override
  public StarlarkAspectPropagationRuleApi getRule() {
    return rule;
  }

  @Override
  public StructApi getAttr() {
    return aspectPublicParams;
  }

  private static record StarlarkAspectPropagationRule(
      Label label, QualifiedRuleKindApi qualifiedRuleKind, StructApi attr)
      implements StarlarkAspectPropagationRuleApi {
    @Override
    public Label getLabel() {
      return label;
    }

    @Override
    public QualifiedRuleKindApi getQualifiedKind() {
      return qualifiedRuleKind;
    }

    @Override
    public StructApi getAttr() {
      return attr;
    }
  }

  private static record QualifiedRuleKind(Label fileLabel, String ruleName)
      implements QualifiedRuleKindApi {
    @Override
    public Label getFileLabel() {
      return fileLabel;
    }

    @Override
    public String getRuleName() {
      return ruleName;
    }
  }

  private static record RuleAttribute(StarlarkValue value, boolean isTool)
      implements RuleAttributeApi {
    @Override
    public StarlarkValue getValue() {
      return value;
    }

    @Override
    public boolean isTool() {
      return isTool;
    }
  }
}
